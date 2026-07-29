import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  DocumentApiError,
  fetchCaseDocumentContent,
  fetchCaseDocuments,
  postCaseDocument,
} from '@/api/documentApi'
import {
  DocumentUploadError,
  downloadCaseDocument,
  getCaseDocuments,
  uploadCaseDocument,
} from '@/services/documentService'
import type { CaseDocumentSummaryResponse } from '@/types/document'

vi.mock('@/api/documentApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/api/documentApi')>()
  return {
    ...original,
    fetchCaseDocuments: vi.fn(),
    postCaseDocument: vi.fn(),
    fetchCaseDocumentContent: vi.fn(),
  }
})

const documentSummary = { id: 4 } as CaseDocumentSummaryResponse

describe('documentService', () => {
  beforeEach(() => {
    vi.mocked(fetchCaseDocuments).mockReset()
    vi.mocked(postCaseDocument).mockReset()
    vi.mocked(fetchCaseDocumentContent).mockReset()
  })

  it('extracts case documents from the list response', async () => {
    vi.mocked(fetchCaseDocuments).mockResolvedValue({
      data: [documentSummary],
    })

    await expect(getCaseDocuments(7)).resolves.toEqual([documentSummary])
  })

  it('returns uploaded document metadata', async () => {
    const file = new File(['content'], 'evidence.pdf')
    vi.mocked(postCaseDocument).mockResolvedValue(documentSummary)

    await expect(uploadCaseDocument(7, file)).resolves.toBe(documentSummary)
  })

  it.each([
    [400, 'invalid'],
    [404, 'not-found'],
    [413, 'too-large'],
    [415, 'unsupported'],
    [500, 'unexpected'],
  ] as const)('maps upload HTTP %i to %s', async (status, reason) => {
    vi.mocked(postCaseDocument).mockRejectedValue(
      new DocumentApiError('upload document', status),
    )

    await expect(uploadCaseDocument(
      7,
      new File(['content'], 'evidence.pdf'),
    )).rejects.toEqual(expect.objectContaining({
      reason,
    }))
  })

  it('does not hide non-HTTP upload failures', async () => {
    const failure = new Error('network failure')
    vi.mocked(postCaseDocument).mockRejectedValue(failure)

    await expect(uploadCaseDocument(
      7,
      new File(['content'], 'evidence.pdf'),
    )).rejects.toBe(failure)
  })

  it('delegates document downloads to the API client', async () => {
    const content = new Blob(['content'])
    vi.mocked(fetchCaseDocumentContent).mockResolvedValue(content)

    await expect(downloadCaseDocument(7, 4)).resolves.toBe(content)
    expect(fetchCaseDocumentContent).toHaveBeenCalledWith(7, 4)
  })

  it('exposes a typed upload error', () => {
    expect(new DocumentUploadError('too-large')).toMatchObject({
      name: 'DocumentUploadError',
      reason: 'too-large',
    })
  })
})
