import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import GenerationResult from './GenerationResult.vue'
import type { GeneratedDocument } from '@/types/documentGeneration'

const result: GeneratedDocument = {
  generationId: 23,
  caseId: 7,
  templateId: 11,
  versionNumber: 2,
  caseDocumentId: 31,
  outputAvailable: true,
  fileName: '律师事务所函.docx',
  createdAt: '2026-08-06T16:20:17Z',
}

describe('GenerationResult', () => {
  it('offers download for an available persisted output', async () => {
    const localTime = vi.spyOn(Date.prototype, 'toLocaleString').mockReturnValue('浏览器本地时间')
    const wrapper = mount(GenerationResult, {
      props: { result, isDownloading: false, downloadError: '' },
    })

    expect(wrapper.text()).toContain('生成于 浏览器本地时间')
    expect(wrapper.text()).toContain('生成记录 #23')
    expect(wrapper.text()).toContain('案件文书 #31')
    expect((localTime.mock.instances[0] as Date).toISOString()).toBe('2026-08-06T16:20:17.000Z')
    expect(localTime).toHaveBeenCalledWith('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
    const download = wrapper.findAll('button').find(button => button.text().includes('下载 DOCX'))!
    expect(download.attributes('disabled')).toBeUndefined()
    await download.trigger('click')
    expect(wrapper.emitted('download')).toHaveLength(1)
    localTime.mockRestore()
  })

  it('explains and disables download after output removal', () => {
    const wrapper = mount(GenerationResult, {
      props: {
        result: { ...result, caseDocumentId: null, outputAvailable: false },
        isDownloading: false,
        downloadError: '',
      },
    })

    expect(wrapper.text()).toContain('案件文书已被移除')
    const download = wrapper.findAll('button').find(button => button.text().includes('下载 DOCX'))!
    expect(download.attributes('disabled')).toBeDefined()
  })
})
