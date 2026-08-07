import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  fetchTemplateVersion,
  fetchTemplateVersions,
  fetchTemplates,
} from './documentTemplateApi'

const response = (status: number, body: unknown): Response => ({
  ok: status >= 200 && status < 300,
  status,
  json: vi.fn().mockResolvedValue(body),
}) as unknown as Response

describe('documentTemplateApi', () => {
  const fetchMock = vi.fn()
  beforeEach(() => vi.stubGlobal('fetch', fetchMock))

  it('lists templates and retrieves exact versions', async () => {
    fetchMock.mockResolvedValueOnce(response(200, { items: [], page: 1, size: 20, totalElements: 0, totalPages: 2 }))
      .mockResolvedValueOnce(response(200, { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }))
      .mockResolvedValueOnce(response(200, { templateId: 3, versionNumber: 2, fields: [] }))
    await fetchTemplates(1, 20)
    await fetchTemplateVersions(3)
    await fetchTemplateVersion(3, 2)
    expect(fetchMock.mock.calls.map(call => call[0])).toEqual([
      '/api/document-templates?page=1&size=20',
      '/api/document-templates/3/versions?page=0&size=20',
      '/api/document-templates/3/versions/2',
    ])
  })
})
