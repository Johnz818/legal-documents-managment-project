import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  deleteCaseDocument,
  DocumentApiError,
  fetchCaseDocumentContent,
  fetchCaseDocuments,
  postCaseDocument,
} from '@/api/documentApi'
import type { CaseDocumentSummaryResponse } from '@/types/document'

const documentSummary: CaseDocumentSummaryResponse = {
  id: 4,
  caseId: 7,
  originalFileName: '证据材料.pdf',
  documentSource: 'UPLOADED',
  fileFormat: 'PDF',
  contentType: 'application/pdf',
  fileSize: 12,
  createdAt: '2026-07-29T10:00:00',
  updatedAt: '2026-07-29T10:00:00',
}

const response = (
  status: number,
  body: unknown = documentSummary,
): Response => ({
  ok: status >= 200 && status < 300,
  status,
  json: vi.fn().mockResolvedValue(body),
  blob: vi.fn().mockResolvedValue(body),
}) as unknown as Response

describe('documentApi', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  it('fetches documents for one case', async () => {
    fetchMock.mockResolvedValue(response(200, { data: [documentSummary] }))

    await expect(fetchCaseDocuments(7)).resolves.toEqual({
      data: [documentSummary],
    })
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/cases/7/documents',
    )
  })

  it('rejects an unsuccessful list request with its status', async () => {
    fetchMock.mockResolvedValue(response(404))

    await expect(fetchCaseDocuments(7)).rejects.toMatchObject({
      status: 404,
    })
  })

  it('uploads a multipart file without setting the content type header', async () => {
    fetchMock.mockResolvedValue(response(201))
    const file = new File(['%PDF-test'], 'evidence.pdf', {
      type: 'application/pdf',
    })

    await expect(postCaseDocument(7, file)).resolves.toEqual(documentSummary)

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/cases/7/documents')
    expect(options.method).toBe('POST')
    expect(options.headers).toBeUndefined()
    expect(options.body).toBeInstanceOf(FormData)
    expect((options.body as FormData).get('file')).toBe(file)
  })

  it.each([400, 404, 413, 415, 500])(
    'preserves upload HTTP %i for service-level handling',
    async (status) => {
      fetchMock.mockResolvedValue(response(status))

      await expect(postCaseDocument(
        7,
        new File(['content'], 'evidence.pdf'),
      )).rejects.toEqual(expect.objectContaining({
        status,
      }))
    },
  )

  it('downloads document content as a blob', async () => {
    const content = new Blob(['%PDF-test'], { type: 'application/pdf' })
    fetchMock.mockResolvedValue(response(200, content))

    await expect(fetchCaseDocumentContent(7, 4)).resolves.toBe(content)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/cases/7/documents/4/content',
    )
  })

  it('rejects an unsuccessful download with a DocumentApiError', async () => {
    fetchMock.mockResolvedValue(response(500))

    await expect(fetchCaseDocumentContent(7, 4))
      .rejects.toBeInstanceOf(DocumentApiError)
  })

  it('removes one document from its owning case', async () => {
    fetchMock.mockResolvedValue(response(204))

    await expect(deleteCaseDocument(7, 4)).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/cases/7/documents/4',
      { method: 'DELETE' },
    )
  })

  it('preserves an unsuccessful removal status', async () => {
    fetchMock.mockResolvedValue(response(404))

    await expect(deleteCaseDocument(7, 4)).rejects.toMatchObject({
      status: 404,
    })
  })
})
