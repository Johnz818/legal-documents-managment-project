import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  DocumentGenerationApiError,
  fetchGenerationPreparation,
  postDocumentGeneration,
} from '@/api/documentGenerationApi'
import { fetchTemplateVersion, fetchTemplateVersions, fetchTemplates } from '@/services/documentTemplateService'
import { downloadCaseDocument } from '@/services/documentService'
import {
  GenerationValidationError,
  buildGenerationRequest,
  classifyGenerationFailure,
  createGenerationAttempt,
  downloadGeneratedDocument,
  getDocumentTemplateVersion,
  getDocumentTemplateVersions,
  getDocumentTemplates,
  presentGenerationError,
  prepareDocumentGeneration,
  submitGenerationAttempt,
  validateGenerationValues,
} from '@/services/documentGenerationService'
import type {
  GeneratedDocument,
  GenerationContext,
  GenerationPreparation,
  PageResponse,
  PreparedGenerationField,
  PublishedTemplateVersion,
} from '@/types/documentGeneration'

vi.mock('@/api/documentGenerationApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/api/documentGenerationApi')>()
  return {
    ...original,
    fetchGenerationPreparation: vi.fn(),
    postDocumentGeneration: vi.fn(),
  }
})

vi.mock('@/services/documentService', () => ({
  downloadCaseDocument: vi.fn(),
}))

vi.mock('@/services/documentTemplateService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/documentTemplateService')>()
  return {
    ...original,
    fetchTemplates: vi.fn(),
    fetchTemplateVersions: vi.fn(),
    fetchTemplateVersion: vi.fn(),
  }
})

const context: GenerationContext = {
  caseId: 7,
  templateId: 11,
  versionNumber: 2,
  timezone: 'Asia/Shanghai',
}

const field = (
  overrides: Partial<PreparedGenerationField> = {},
): PreparedGenerationField => ({
  fieldKey: 'case_number',
  displayName: '案号',
  description: null,
  valueType: 'TEXT',
  required: true,
  defaultSource: 'CASE_FIELD',
  sourceKey: 'caseNumber',
  displayOrder: 0,
  suggestedValue: '(2026)沪01号',
  status: 'RESOLVED',
  ...overrides,
})

const generated: GeneratedDocument = {
  generationId: 23,
  caseId: 7,
  templateId: 11,
  versionNumber: 2,
  caseDocumentId: 31,
  outputAvailable: true,
  fileName: '律师事务所函.docx',
  createdAt: '2026-08-06T10:00:00',
}

describe('documentGenerationService', () => {
  beforeEach(() => {
    vi.mocked(fetchTemplates).mockReset()
    vi.mocked(fetchTemplateVersions).mockReset()
    vi.mocked(fetchTemplateVersion).mockReset()
    vi.mocked(fetchGenerationPreparation).mockReset()
    vi.mocked(postDocumentGeneration).mockReset()
    vi.mocked(downloadCaseDocument).mockReset()
  })

  it('delegates template, version, and preparation reads', async () => {
    const page: PageResponse<never> = {
      items: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
    }
    const exactVersion = {
      templateId: 11,
      versionNumber: 2,
      fields: [],
    } as PublishedTemplateVersion
    const preparation: GenerationPreparation = { ...context, fields: [] }
    vi.mocked(fetchTemplates).mockResolvedValue(page)
    vi.mocked(fetchTemplateVersions).mockResolvedValue(page)
    vi.mocked(fetchTemplateVersion).mockResolvedValue(exactVersion)
    vi.mocked(fetchGenerationPreparation).mockResolvedValue(preparation)

    await expect(getDocumentTemplates()).resolves.toBe(page)
    await expect(getDocumentTemplateVersions(11)).resolves.toBe(page)
    await expect(getDocumentTemplateVersion(11, 2)).resolves.toBe(exactVersion)
    await expect(prepareDocumentGeneration(context)).resolves.toBe(preparation)
  })

  it('retains deterministic provenance only for an unchanged resolved suggestion', () => {
    expect(buildGenerationRequest([field()], {
      case_number: '(2026)沪01号',
    }).values[0].valueSource).toBe('CASE_FIELD')

    expect(buildGenerationRequest([field()], {
      case_number: '(2026)沪02号',
    }).values[0].valueSource).toBe('USER_INPUT')

    expect(buildGenerationRequest([field({
      status: 'REQUIRES_USER_INPUT',
      suggestedValue: null,
    })], {
      case_number: '(2026)沪01号',
    }).values[0].valueSource).toBe('USER_INPUT')
  })

  it('honors an explicit provenance choice made during stale-value review', () => {
    expect(buildGenerationRequest([field()], {
      case_number: '(2026)沪01号',
    }, {
      case_number: 'USER_INPUT',
    }).values[0].valueSource).toBe('USER_INPUT')
  })

  it.each([
    [field({ valueType: 'DATE' }), '2026-08-06'],
    [field({ valueType: 'DATE' }), '2026年8月6日'],
    [field({ valueType: 'DECIMAL' }), '-1200.50'],
    [field({ valueType: 'BOOLEAN' }), 'false'],
    [field({ required: false }), ''],
  ] as const)('accepts supported scalar value %s', (templateField, value) => {
    expect(validateGenerationValues([templateField], {
      case_number: value,
    }).valid).toBe(true)
  })

  it.each([
    [field(), 'line one\nline two'],
    [field(), '   '],
    [field({ valueType: 'DATE' }), '2026-02-30'],
    [field({ valueType: 'DECIMAL' }), '01.2'],
    [field({ valueType: 'BOOLEAN' }), 'yes'],
  ] as const)('rejects unsupported scalar value %s', (templateField, value) => {
    expect(validateGenerationValues([templateField], {
      case_number: value,
    }).valid).toBe(false)
  })

  it('rejects missing, unexpected, oversized, and aggregate values', () => {
    expect(validateGenerationValues([field()], {}).valid).toBe(false)
    expect(validateGenerationValues([field()], {
      case_number: 'valid',
      unexpected: 'value',
    })).toMatchObject({ valid: false, formError: expect.any(String) })
    expect(validateGenerationValues([field()], {
      case_number: 'a'.repeat(10_001),
    }).valid).toBe(false)

    const fields = Array.from({ length: 11 }, (_, index) => field({
      fieldKey: `field_${index}`,
      required: false,
    }))
    const values = Object.fromEntries(fields.map(item => [item.fieldKey, 'a'.repeat(10_000)]))
    expect(validateGenerationValues(fields, values)).toMatchObject({
      valid: false,
      formError: expect.any(String),
    })
  })

  it('does not create a UUID when validation fails', () => {
    const createUuid = vi.fn(() => 'unused')

    expect(() => createGenerationAttempt(context, [field()], {}, createUuid))
      .toThrow(GenerationValidationError)
    expect(createUuid).not.toHaveBeenCalled()
  })

  it('creates a deeply immutable snapshot only after validation', () => {
    const values = { case_number: '(2026)沪01号' }
    const attempt = createGenerationAttempt(context, [field()], values, () => 'uuid-1')

    values.case_number = 'changed later'

    expect(attempt).toMatchObject({
      idempotencyKey: 'uuid-1',
      ...context,
      request: {
        values: [{
          fieldKey: 'case_number',
          value: '(2026)沪01号',
          valueSource: 'CASE_FIELD',
        }],
      },
    })
    expect(Object.isFrozen(attempt)).toBe(true)
    expect(Object.isFrozen(attempt.request)).toBe(true)
    expect(Object.isFrozen(attempt.request.values)).toBe(true)
    expect(Object.isFrozen(attempt.request.values[0])).toBe(true)
  })

  it('submits exact retries from the same frozen attempt', async () => {
    const attempt = createGenerationAttempt(context, [field()], {
      case_number: '(2026)沪01号',
    }, () => 'uuid-1')
    vi.mocked(postDocumentGeneration).mockResolvedValue(generated)

    await submitGenerationAttempt(attempt)
    await submitGenerationAttempt(attempt)

    expect(postDocumentGeneration).toHaveBeenCalledTimes(2)
    expect(postDocumentGeneration).toHaveBeenNthCalledWith(
      1, attempt, 'uuid-1', attempt.request,
    )
    expect(postDocumentGeneration).toHaveBeenNthCalledWith(
      2, attempt, 'uuid-1', attempt.request,
    )
  })

  it.each([
    ['GENERATION_REQUEST_INVALID', 'CORRECT_VALUES'],
    ['GENERATION_VALUE_STALE', 'REFRESH_PREPARATION'],
    ['GENERATION_CASE_NOT_FOUND', 'RESELECT_CASE'],
    ['GENERATION_TEMPLATE_VERSION_NOT_FOUND', 'RESELECT_TEMPLATE_VERSION'],
    ['GENERATION_IDEMPOTENCY_CONFLICT', 'STOP_IDEMPOTENCY_CONFLICT'],
    ['GENERATION_TEMPLATE_INTEGRITY_FAILED', 'STOP_TEMPLATE_INTEGRITY'],
    ['GENERATION_RENDERING_FAILED', 'RETRY_EXACT_REQUEST'],
    ['GENERATION_STORAGE_FAILED', 'RETRY_EXACT_REQUEST'],
    ['GENERATION_PERSISTENCE_FAILED', 'RETRY_EXACT_REQUEST'],
  ] as const)('classifies %s as %s', (code, action) => {
    const error = new DocumentGenerationApiError('generate', 400, { code })
    expect(classifyGenerationFailure(error)).toBe(action)
  })

  it('treats network and unknown backend failures as uncertain', () => {
    expect(classifyGenerationFailure(new TypeError('network failed')))
      .toBe('RETRY_EXACT_REQUEST')
    expect(classifyGenerationFailure(
      new DocumentGenerationApiError('generate', 500, { code: 'UNKNOWN' }),
    )).toBe('RETRY_EXACT_REQUEST')
  })

  it('presents a controlled backend detail against the matching display field', () => {
    const presentation = presentGenerationError(
      new DocumentGenerationApiError('generate', 400, {
        title: 'Invalid generation value',
        detail: 'The reviewed case number is no longer current.',
        code: 'GENERATION_VALUE_STALE',
        details: { fieldKey: 'case_number' },
      }),
      [field()],
    )

    expect(presentation).toEqual({
      summary: 'Invalid generation value',
      detail: 'The reviewed case number is no longer current.',
      affectedFields: [{
        fieldKey: 'case_number',
        displayName: '案号',
        message: 'The reviewed case number is no longer current.',
      }],
    })
  })

  it('falls back safely when a failure has no controlled backend problem detail', () => {
    expect(presentGenerationError(new TypeError('network internals'), [], '请求失败'))
      .toEqual({ summary: '请求失败', affectedFields: [] })
  })

  it('downloads only an available generated Case document', async () => {
    const blob = new Blob(['docx'])
    vi.mocked(downloadCaseDocument).mockResolvedValue(blob)

    await expect(downloadGeneratedDocument(generated)).resolves.toBe(blob)
    expect(downloadCaseDocument).toHaveBeenCalledWith(7, 31)

    await expect(downloadGeneratedDocument({
      ...generated,
      caseDocumentId: null,
      outputAvailable: false,
    })).rejects.toThrow('unavailable')
  })
})
