import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  downloadTemplateVersion,
  fetchTemplateVersion,
  fetchTemplateVersions,
  fetchTemplates,
  inspectTemplate,
  publishTemplate,
  publishTemplateVersion,
} from './documentTemplateApi'

const response = (status: number, body: unknown): Response => ({
  ok: status >= 200 && status < 300,
  status,
  json: vi.fn().mockResolvedValue(body),
  blob: vi.fn().mockResolvedValue(body),
}) as unknown as Response

describe('documentTemplateApi', () => {
  const fetchMock = vi.fn()
  beforeEach(() => vi.stubGlobal('fetch', fetchMock))

  it('inspects a DOCX with browser-owned multipart headers', async () => {
    fetchMock.mockResolvedValue(response(200, { markers: [] }))
    const file = new File(['docx'], 'template.docx')
    await inspectTemplate(file)
    const [, init] = fetchMock.mock.calls[0]
    expect(fetchMock.mock.calls[0][0]).toBe('/api/document-templates/inspections')
    expect(init.headers).toBeUndefined()
    expect(init.body).toBeInstanceOf(FormData)
    expect(init.body.get('file')).toBe(file)
  })

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

  it('publishes new and later versions with JSON Blob parts', async () => {
    fetchMock.mockResolvedValue(response(201, { templateId: 3, versionNumber: 1, fields: [] }))
    const file = new File(['docx'], 'template.docx')
    await publishTemplate(file, { name: '测试', description: null, fields: [] })
    const firstInit = fetchMock.mock.calls[0][1]
    expect(firstInit.headers).toBeUndefined()
    expect(await ((firstInit.body as FormData).get('publication') as Blob).text()).toBe(
      JSON.stringify({ name: '测试', description: null, fields: [] }),
    )

    await publishTemplateVersion(3, file, { fields: [] })
    expect(fetchMock.mock.calls[1][0]).toBe('/api/document-templates/3/versions')
    expect(await ((fetchMock.mock.calls[1][1].body as FormData).get('publication') as Blob).text()).toBe(
      JSON.stringify({ fields: [] }),
    )
  })

  it('downloads exact published content', async () => {
    const blob = new Blob(['docx'])
    fetchMock.mockResolvedValue(response(200, blob))
    await expect(downloadTemplateVersion(3, 2)).resolves.toBe(blob)
    expect(fetchMock).toHaveBeenCalledWith('/api/document-templates/3/versions/2/content')
  })
})
