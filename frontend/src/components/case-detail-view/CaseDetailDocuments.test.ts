import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import CaseDetailDocuments from '@/components/case-detail-view/CaseDetailDocuments.vue'
import {
  DocumentRemovalError,
  DocumentUploadError,
  downloadCaseDocument,
  getCaseDocuments,
  removeCaseDocument,
  uploadCaseDocument,
} from '@/services/documentService'
import type { CaseDocumentSummaryResponse } from '@/types/document'

vi.mock('@/services/documentService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/documentService')>()
  return {
    ...original,
    getCaseDocuments: vi.fn(),
    uploadCaseDocument: vi.fn(),
    downloadCaseDocument: vi.fn(),
    removeCaseDocument: vi.fn(),
  }
})

const documentSummary: CaseDocumentSummaryResponse = {
  id: 4,
  caseId: 7,
  originalFileName: '证据材料.pdf',
  documentSource: 'UPLOADED',
  fileFormat: 'PDF',
  contentType: 'application/pdf',
  fileSize: 1536,
  createdAt: '2026-07-29T10:00:00',
  updatedAt: '2026-07-29T10:00:00',
  generatedAt: null,
}

const mountDocuments = () => mount(CaseDetailDocuments, {
  props: {
    caseId: 7,
  },
  global: {
    stubs: {
      SafeIcon: true,
    },
  },
})

describe('CaseDetailDocuments', () => {
  const createObjectUrl = vi.fn(() => 'blob:test-document')
  const revokeObjectUrl = vi.fn()

  beforeEach(() => {
    vi.mocked(getCaseDocuments).mockReset()
    vi.mocked(uploadCaseDocument).mockReset()
    vi.mocked(downloadCaseDocument).mockReset()
    vi.mocked(removeCaseDocument).mockReset()
    createObjectUrl.mockClear()
    revokeObjectUrl.mockClear()
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: createObjectUrl,
    })
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      value: revokeObjectUrl,
    })
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('loads and displays live document metadata', async () => {
    vi.mocked(getCaseDocuments).mockResolvedValue([documentSummary])

    const wrapper = mountDocuments()
    expect(wrapper.text()).toContain('正在加载案件文件')
    await flushPromises()

    expect(getCaseDocuments).toHaveBeenCalledWith(7)
    expect(wrapper.text()).toContain('证据材料.pdf')
    expect(wrapper.text()).toContain('PDF')
    expect(wrapper.text()).toContain('1.5 KB')
    expect(wrapper.text()).toContain('上传于')
    expect(wrapper.text()).not.toContain('暂无案件文件')
  })

  it('shows verified local generation times and stable IDs for identical filenames', async () => {
    const generatedAt = '2026-08-06T16:20:17Z'
    const localTime = vi.spyOn(Date.prototype, 'toLocaleString').mockReturnValue('浏览器本地时间')
    vi.mocked(getCaseDocuments).mockResolvedValue([
      {
        ...documentSummary,
        id: 13,
        originalFileName: '生成文书.docx',
        documentSource: 'GENERATED',
        fileFormat: 'DOCX',
        generatedAt,
      },
      {
        ...documentSummary,
        id: 14,
        originalFileName: '生成文书.docx',
        documentSource: 'GENERATED',
        fileFormat: 'DOCX',
        generatedAt,
      },
    ])
    const wrapper = mountDocuments()
    await flushPromises()

    expect(wrapper.text()).toContain('生成于')
    expect(wrapper.text()).toContain('浏览器本地时间')
    expect(wrapper.text()).toContain('文件编号 #13')
    expect(wrapper.text()).toContain('文件编号 #14')
    expect(wrapper.findAll('p.truncate.font-medium').map(item => item.text()))
      .toEqual(['生成文书.docx', '生成文书.docx'])
    expect((localTime.mock.instances[0] as Date).toISOString()).toBe('2026-08-06T16:20:17.000Z')
    expect(localTime).toHaveBeenCalledWith('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
    localTime.mockRestore()
  })

  it('falls back to the stable ID when historical generation time is unavailable', async () => {
    vi.mocked(getCaseDocuments).mockResolvedValue([{
      ...documentSummary,
      id: 15,
      originalFileName: '历史生成文书.docx',
      documentSource: 'GENERATED',
      fileFormat: 'DOCX',
      generatedAt: null,
    }])
    const wrapper = mountDocuments()
    await flushPromises()

    expect(wrapper.text()).toContain('生成文书 · 文件编号 #15')
    expect(wrapper.text()).not.toContain('生成于')
  })

  it('shows an empty state for a case without documents', async () => {
    vi.mocked(getCaseDocuments).mockResolvedValue([])

    const wrapper = mountDocuments()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无案件文件')
    expect(wrapper.text()).toContain('单个文件最大 5 MB')
    expect(wrapper.get('[data-slot="empty-content"]').classes())
      .toEqual(expect.arrayContaining(['mx-auto', 'justify-center']))
  })

  it('shows a list error and retries', async () => {
    vi.mocked(getCaseDocuments)
      .mockRejectedValueOnce(new Error('backend unavailable'))
      .mockResolvedValueOnce([])

    const wrapper = mountDocuments()
    await flushPromises()
    expect(wrapper.text()).toContain('案件文件加载失败')

    const retry = wrapper.findAll('button')
      .find(button => button.text().includes('重新加载'))
    await retry?.trigger('click')
    await flushPromises()

    expect(getCaseDocuments).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('暂无案件文件')
  })

  it('uploads a selected file and refreshes the list', async () => {
    vi.mocked(getCaseDocuments)
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([documentSummary])
    vi.mocked(uploadCaseDocument).mockResolvedValue(documentSummary)
    const wrapper = mountDocuments()
    await flushPromises()
    const input = wrapper.get('input[type="file"]')
    const file = new File(['%PDF-test'], '证据材料.pdf', {
      type: 'application/pdf',
    })
    Object.defineProperty(input.element, 'files', {
      configurable: true,
      value: [file],
    })

    await input.trigger('change')
    await flushPromises()

    expect(uploadCaseDocument).toHaveBeenCalledWith(7, file)
    expect(getCaseDocuments).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('证据材料.pdf')
    expect((input.element as HTMLInputElement).value).toBe('')
  })

  it('shows a specific upload validation failure', async () => {
    vi.mocked(getCaseDocuments).mockResolvedValue([])
    vi.mocked(uploadCaseDocument).mockRejectedValue(
      new DocumentUploadError('too-large'),
    )
    const wrapper = mountDocuments()
    await flushPromises()
    const input = wrapper.get('input[type="file"]')
    const file = new File(['content'], 'large.pdf')
    Object.defineProperty(input.element, 'files', {
      configurable: true,
      value: [file],
    })

    await input.trigger('change')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('不能超过 5 MB')
    expect(getCaseDocuments).toHaveBeenCalledTimes(1)
  })

  it('downloads content with the backend filename and revokes the object URL', async () => {
    vi.mocked(getCaseDocuments).mockResolvedValue([documentSummary])
    const content = new Blob(['%PDF-test'], { type: 'application/pdf' })
    vi.mocked(downloadCaseDocument).mockResolvedValue(content)
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined)
    const wrapper = mountDocuments()
    await flushPromises()
    const download = wrapper.findAll('button')
      .find(button => button.text().includes('下载'))

    await download?.trigger('click')
    await flushPromises()

    expect(downloadCaseDocument).toHaveBeenCalledWith(7, 4)
    expect(createObjectUrl).toHaveBeenCalledWith(content)
    expect(click).toHaveBeenCalled()
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:test-document')
    click.mockRestore()
  })

  it('cancels removal without calling the backend', async () => {
    vi.mocked(getCaseDocuments).mockResolvedValue([documentSummary])
    const wrapper = mountDocuments()
    await flushPromises()

    const remove = wrapper.findAll('button')
      .find(button => button.text().includes('移除'))
    await remove?.trigger('click')
    await flushPromises()

    const cancel = Array.from(document.body.querySelectorAll('button'))
      .find(button => button.textContent?.includes('取消'))
    cancel?.click()
    await flushPromises()

    expect(removeCaseDocument).not.toHaveBeenCalled()
  })

  it('confirms removal and refreshes the document list', async () => {
    vi.mocked(getCaseDocuments)
      .mockResolvedValueOnce([documentSummary])
      .mockResolvedValueOnce([])
    vi.mocked(removeCaseDocument).mockResolvedValue()
    const wrapper = mountDocuments()
    await flushPromises()

    const remove = wrapper.findAll('button')
      .find(button => button.text().includes('移除'))
    await remove?.trigger('click')
    await flushPromises()
    expect(document.body.textContent).toContain('证据材料.pdf')

    const confirm = Array.from(document.body.querySelectorAll('button'))
      .find(button => button.textContent?.includes('确认移除'))
    confirm?.click()
    await flushPromises()

    expect(removeCaseDocument).toHaveBeenCalledWith(7, 4)
    expect(getCaseDocuments).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('暂无案件文件')
  })

  it('keeps the document visible and reports a removal failure', async () => {
    vi.mocked(getCaseDocuments).mockResolvedValue([documentSummary])
    vi.mocked(removeCaseDocument).mockRejectedValue(
      new DocumentRemovalError('not-found'),
    )
    const wrapper = mountDocuments()
    await flushPromises()

    const remove = wrapper.findAll('button')
      .find(button => button.text().includes('移除'))
    await remove?.trigger('click')
    await flushPromises()
    const confirm = Array.from(document.body.querySelectorAll('button'))
      .find(button => button.textContent?.includes('确认移除'))
    confirm?.click()
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('文件已不存在')
    expect(wrapper.text()).toContain('证据材料.pdf')
    expect(getCaseDocuments).toHaveBeenCalledTimes(1)
  })
})
