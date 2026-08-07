import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  DocumentGenerationApiError,
  fetchGenerationPreparation,
  postDocumentGeneration,
} from '@/api/documentGenerationApi'
import type {
  GeneratedDocument,
  GenerationContext,
  GenerationPreparation,
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

    const error = await fetchGenerationPreparation(context).catch(value => value)

    expect(error).toBeInstanceOf(DocumentGenerationApiError)
    expect(error).toMatchObject({ status: 503, problem: null })
    expect(error.message).toBe('Failed to prepare document generation for case 7: HTTP 503')
  })
})
