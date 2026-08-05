import {
  DocumentGenerationApiError,
  fetchDocumentTemplates,
  fetchDocumentTemplateVersion,
  fetchDocumentTemplateVersions,
  fetchGenerationPreparation,
  postDocumentGeneration,
} from '@/api/documentGenerationApi'
import { downloadCaseDocument } from '@/services/documentService'
import type {
  DocumentFieldSource,
  DocumentGenerationRequest,
  DocumentTemplateSummary,
  DocumentTemplateVersionSummary,
  GeneratedDocument,
  GenerationAttempt,
  GenerationContext,
  GenerationFailureAction,
  GenerationPreparation,
  GenerationValidationResult,
  PageResponse,
  PreparedGenerationField,
  PublishedTemplateVersion,
} from '@/types/documentGeneration'

const MAX_VALUE_CODE_POINTS = 10_000
const MAX_AGGREGATE_CODE_POINTS = 100_000
const DECIMAL = /^-?(?:0|[1-9][0-9]*)(?:\.[0-9]+)?$/
const ISO_DATE = /^([0-9]{4})-([0-9]{2})-([0-9]{2})$/
const CHINESE_DATE = /^([0-9]{4})年([0-9]{1,2})月([0-9]{1,2})日$/

export class GenerationValidationError extends Error {
  readonly validation: GenerationValidationResult

  constructor(validation: GenerationValidationResult) {
    super('Generation values are invalid')
    this.name = 'GenerationValidationError'
    this.validation = validation
  }
}

const codePointLength = (value: string) => [...value].length

const isRealDate = (value: string) => {
  const match = ISO_DATE.exec(value) ?? CHINESE_DATE.exec(value)
  if (!match) return false

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  if (month < 1 || month > 12 || day < 1) return false
  return day <= new Date(Date.UTC(year, month, 0)).getUTCDate()
}

const fieldError = (field: PreparedGenerationField, value: string): string | null => {
  if (codePointLength(value) > MAX_VALUE_CODE_POINTS || /[\r\n]/.test(value)) {
    return '值必须是长度不超过 10000 个字符的单行文本。'
  }
  if (field.required && value.trim().length === 0) {
    return '此字段为必填项。'
  }
  if (!field.required && value.length === 0) return null

  const valid = field.valueType === 'TEXT'
    || field.valueType === 'DECIMAL' && DECIMAL.test(value)
    || field.valueType === 'BOOLEAN' && (value === 'true' || value === 'false')
    || field.valueType === 'DATE' && isRealDate(value)

  return valid ? null : '值与字段类型不匹配。'
}

export function validateGenerationValues(
  fields: readonly PreparedGenerationField[],
  values: Readonly<Record<string, string>>,
): GenerationValidationResult {
  const contractKeys = new Set(fields.map(field => field.fieldKey))
  const submittedKeys = Object.keys(values)
  const fieldErrors: Record<string, string> = {}
  let aggregate = 0

  for (const field of fields) {
    const value = values[field.fieldKey]
    if (typeof value !== 'string') {
      fieldErrors[field.fieldKey] = '此字段缺少值。'
      continue
    }
    aggregate += codePointLength(value)
    const error = fieldError(field, value)
    if (error) fieldErrors[field.fieldKey] = error
  }

  let formError: string | undefined
  if (submittedKeys.some(key => !contractKeys.has(key))) {
    formError = '提交值包含模板未定义的字段。'
  } else if (aggregate > MAX_AGGREGATE_CODE_POINTS) {
    formError = '全部字段值的总长度不能超过 100000 个字符。'
  }

  return {
    valid: Object.keys(fieldErrors).length === 0 && formError === undefined,
    fieldErrors,
    formError,
  }
}

const valueSource = (field: PreparedGenerationField, value: string): DocumentFieldSource => {
  if (field.status === 'RESOLVED'
      && field.suggestedValue === value
      && field.defaultSource !== 'USER_INPUT') {
    return field.defaultSource
  }
  return 'USER_INPUT'
}

export function buildGenerationRequest(
  fields: readonly PreparedGenerationField[],
  values: Readonly<Record<string, string>>,
): DocumentGenerationRequest {
  return {
    values: fields.map(field => ({
      fieldKey: field.fieldKey,
      value: values[field.fieldKey],
      valueSource: valueSource(field, values[field.fieldKey]),
    })),
  }
}

export function createGenerationAttempt(
  context: GenerationContext,
  fields: readonly PreparedGenerationField[],
  values: Readonly<Record<string, string>>,
  createUuid: () => string = () => crypto.randomUUID(),
): GenerationAttempt {
  const validation = validateGenerationValues(fields, values)
  if (!validation.valid) throw new GenerationValidationError(validation)

  const request = buildGenerationRequest(fields, values)
  const immutableValues = Object.freeze(
    request.values.map(value => Object.freeze({ ...value })),
  )
  return Object.freeze({
    idempotencyKey: createUuid(),
    ...context,
    request: Object.freeze({ values: immutableValues }),
  })
}

export function submitGenerationAttempt(
  attempt: GenerationAttempt,
): Promise<GeneratedDocument> {
  return postDocumentGeneration(
    attempt,
    attempt.idempotencyKey,
    attempt.request,
  )
}

export function classifyGenerationFailure(error: unknown): GenerationFailureAction {
  if (!(error instanceof DocumentGenerationApiError)) return 'RETRY_EXACT_REQUEST'

  const actionByCode: Record<string, GenerationFailureAction> = {
    GENERATION_REQUEST_INVALID: 'CORRECT_VALUES',
    GENERATION_VALUE_STALE: 'REFRESH_PREPARATION',
    GENERATION_CASE_NOT_FOUND: 'RESELECT_CASE',
    GENERATION_TEMPLATE_VERSION_NOT_FOUND: 'RESELECT_TEMPLATE_VERSION',
    GENERATION_IDEMPOTENCY_CONFLICT: 'STOP_IDEMPOTENCY_CONFLICT',
    GENERATION_TEMPLATE_INTEGRITY_FAILED: 'STOP_TEMPLATE_INTEGRITY',
    GENERATION_RENDERING_FAILED: 'RETRY_EXACT_REQUEST',
    GENERATION_STORAGE_FAILED: 'RETRY_EXACT_REQUEST',
    GENERATION_PERSISTENCE_FAILED: 'RETRY_EXACT_REQUEST',
  }
  return error.code ? actionByCode[error.code] ?? 'RETRY_EXACT_REQUEST' : 'RETRY_EXACT_REQUEST'
}

export const getDocumentTemplates = (
  page = 0,
  size = 20,
): Promise<PageResponse<DocumentTemplateSummary>> => fetchDocumentTemplates(page, size)

export const getDocumentTemplateVersions = (
  templateId: number,
  page = 0,
  size = 20,
): Promise<PageResponse<DocumentTemplateVersionSummary>> => (
  fetchDocumentTemplateVersions(templateId, page, size)
)

export const getDocumentTemplateVersion = (
  templateId: number,
  versionNumber: number,
): Promise<PublishedTemplateVersion> => (
  fetchDocumentTemplateVersion(templateId, versionNumber)
)

export const prepareDocumentGeneration = (
  context: GenerationContext,
): Promise<GenerationPreparation> => fetchGenerationPreparation(context)

export function downloadGeneratedDocument(generated: GeneratedDocument): Promise<Blob> {
  if (!generated.outputAvailable || generated.caseDocumentId === null) {
    return Promise.reject(new Error('Generated document output is unavailable'))
  }
  return downloadCaseDocument(generated.caseId, generated.caseDocumentId)
}

