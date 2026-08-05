import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  DocumentGenerationApiError,
  fetchDocumentTemplates,
  fetchDocumentTemplateVersion,
  fetchDocumentTemplateVersions,
  fetchGenerationPreparation,
  postDocumentGeneration,
} from '@/api/documentGenerationApi'
import type {
  DocumentTemplateSummary,
  DocumentTemplateVersionSummary,
  GeneratedDocument,
  GenerationContext,
  GenerationPreparation,
  PageResponse,
  PublishedTemplateVersion,
} from '@/types/documentGeneration'

const context: GenerationContext = {
  caseId: 7,
  templateId: 11,
  versionNumber: 2,
  timezone: 'Asia/Shanghai',
}

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

const response = (status: number, body: unknown): Response => ({
  ok: status >= 200 && status < 300,
  status,
  json: vi.fn().mockResolvedValue(body),
}) as unknown as Response

describe('documentGenerationApi', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  it('fetches a bounded page of templates', async () => {
    const page: PageResponse<DocumentTemplateSummary> = {
      items: [], page: 1, size: 20, totalElements: 21, totalPages: 2,
    }
    fetchMock.mockResolvedValue(response(200, page))

    await expect(fetchDocumentTemplates(1, 20)).resolves.toEqual(page)
    expect(fetchMock).toHaveBeenCalledWith('/api/document-templates?page=1&size=20')
  })

  it('fetches versions scoped to one template', async () => {
    const page: PageResponse<DocumentTemplateVersionSummary> = {
      items: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
    }
    fetchMock.mockResolvedValue(response(200, page))

    await expect(fetchDocumentTemplateVersions(11)).resolves.toEqual(page)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/document-templates/11/versions?page=0&size=20',
    )
  })

  it('fetches one exact template version', async () => {
    const version = {
      templateId: 11,
      versionNumber: 2,
      fields: [],
    } as PublishedTemplateVersion
    fetchMock.mockResolvedValue(response(200, version))

    await expect(fetchDocumentTemplateVersion(11, 2)).resolves.toEqual(version)
    expect(fetchMock).toHaveBeenCalledWith('/api/document-templates/11/versions/2')
  })

  it('prepares generation with the exact context and encoded timezone', async () => {
    const preparation: GenerationPreparation = { ...context, fields: [] }
    fetchMock.mockResolvedValue(response(200, preparation))

    await expect(fetchGenerationPreparation(context)).resolves.toEqual(preparation)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/cases/7/document-generations/preparation?templateId=11&versionNumber=2&timezone=Asia%2FShanghai',
    )
  })

  it('posts reviewed values with its idempotency key', async () => {
    fetchMock.mockResolvedValue(response(201, generated))
    const request = {
      values: [{ fieldKey: 'case_number', value: '(2026)沪01号', valueSource: 'USER_INPUT' as const }],
    }

    await expect(postDocumentGeneration(context, 'test-key', request)).resolves.toEqual(generated)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/cases/7/document-generations?templateId=11&versionNumber=2&timezone=Asia%2FShanghai',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': 'test-key',
        },
        body: JSON.stringify(request),
      },
    )
  })

  it('preserves structured backend problem details', async () => {
    fetchMock.mockResolvedValue(response(409, {
      title: 'Document generation failed',
      status: 409,
      detail: 'A reviewed deterministic value is no longer current',
      code: 'GENERATION_VALUE_STALE',
      details: { fieldKey: 'case_number' },
    }))

    const error = await fetchGenerationPreparation(context).catch(value => value)

    expect(error).toBeInstanceOf(DocumentGenerationApiError)
    expect(error).toMatchObject({
      status: 409,
      code: 'GENERATION_VALUE_STALE',
      problem: {
        details: { fieldKey: 'case_number' },
      },
    })
  })

  it('falls back safely when an error response is not JSON', async () => {
    const invalidResponse = response(503, null)
    vi.mocked(invalidResponse.json).mockRejectedValue(new SyntaxError('invalid JSON'))
    fetchMock.mockResolvedValue(invalidResponse)

    const error = await fetchDocumentTemplates().catch(value => value)

    expect(error).toBeInstanceOf(DocumentGenerationApiError)
    expect(error).toMatchObject({ status: 503, problem: null })
    expect(error.message).toBe('Failed to fetch document templates: HTTP 503')
  })
})
