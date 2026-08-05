export type DocumentTemplateType = 'PRESET' | 'CUSTOM'

export type DocumentFieldValueType = 'TEXT' | 'DATE' | 'DECIMAL' | 'BOOLEAN'

export type DocumentFieldSource = 'CASE_FIELD' | 'SYSTEM_VALUE' | 'USER_INPUT'

export type GenerationValueStatus = 'RESOLVED' | 'REQUIRES_USER_INPUT'

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface DocumentTemplateSummary {
  id: number
  name: string
  description: string | null
  templateType: DocumentTemplateType
  createdAt: string
  updatedAt: string
}

export interface DocumentTemplateVersionSummary {
  versionNumber: number
  originalFileName: string
  contentType: string
  fileSize: number
  contentSha256: string
  publishedAt: string
}

export interface PublishedTemplateField {
  fieldKey: string
  displayName: string
  description: string | null
  valueType: DocumentFieldValueType
  required: boolean
  defaultSource: DocumentFieldSource
  sourceKey: string | null
  displayOrder: number
}

export interface PublishedTemplateVersion {
  templateId: number
  templateName: string
  templateDescription: string | null
  versionNumber: number
  originalFileName: string
  contentType: string
  fileSize: number
  contentSha256: string
  publishedAt: string
  fields: PublishedTemplateField[]
}

export interface PreparedGenerationField extends PublishedTemplateField {
  suggestedValue: string | null
  status: GenerationValueStatus
}

export interface GenerationPreparation {
  caseId: number
  templateId: number
  versionNumber: number
  timezone: string
  fields: PreparedGenerationField[]
}

export interface GenerationValueRequest {
  fieldKey: string
  value: string
  valueSource: DocumentFieldSource
}

export interface DocumentGenerationRequest {
  values: GenerationValueRequest[]
}

export interface GeneratedDocument {
  generationId: number
  caseId: number
  templateId: number
  versionNumber: number
  caseDocumentId: number | null
  outputAvailable: boolean
  fileName: string
  createdAt: string
}

export interface BackendProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  code?: string
  details?: Record<string, unknown>
}

export interface GenerationContext {
  caseId: number
  templateId: number
  versionNumber: number
  timezone: string
}

export interface GenerationAttempt {
  readonly idempotencyKey: string
  readonly caseId: number
  readonly templateId: number
  readonly versionNumber: number
  readonly timezone: string
  readonly request: Readonly<{
    values: readonly Readonly<GenerationValueRequest>[]
  }>
}

export interface GenerationValidationResult {
  valid: boolean
  fieldErrors: Record<string, string>
  formError?: string
}

export type GenerationFailureAction =
  | 'RETRY_EXACT_REQUEST'
  | 'CORRECT_VALUES'
  | 'REFRESH_PREPARATION'
  | 'RESELECT_CASE'
  | 'RESELECT_TEMPLATE_VERSION'
  | 'STOP_IDEMPOTENCY_CONFLICT'
  | 'STOP_TEMPLATE_INTEGRITY'

