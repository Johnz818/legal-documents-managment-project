import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TemplateSelector from './TemplateSelector.vue'
import type { PublishedTemplateVersion } from '@/types/documentGeneration'

const template = {
  id: 11,
  name: '律师事务所函',
  description: '测试模板',
  templateType: 'CUSTOM' as const,
  createdAt: '2026-08-01T10:00:00',
  updatedAt: '2026-08-01T10:00:00',
}

const version = {
  versionNumber: 2,
  originalFileName: 'letter.docx',
  contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  fileSize: 100,
  contentSha256: 'a'.repeat(64),
  publishedAt: '2026-08-01T10:00:00',
}

const props = {
  templates: [template],
  versions: [version],
  selectedTemplateId: 11,
  selectedVersionNumber: null as number | null,
  exactVersion: null as PublishedTemplateVersion | null,
  isLoadingTemplates: false,
  isLoadingVersions: false,
  isLoadingExactVersion: false,
  templateError: '',
  versionError: '',
  hasMoreTemplates: true,
  hasMoreVersions: true,
  disabled: false,
}

describe('TemplateSelector', () => {
  it('requires explicit template/version selection and emits Load more', async () => {
    const wrapper = mount(TemplateSelector, { props })

    expect(wrapper.emitted('selectVersion')).toBeUndefined()
    const buttons = wrapper.findAll('button')
    await buttons.find(button => button.text().includes('律师事务所函'))!.trigger('click')
    await buttons.find(button => button.text().includes('版本 2'))!.trigger('click')
    await buttons.find(button => button.text().includes('加载更多模板'))!.trigger('click')
    await buttons.find(button => button.text().includes('加载更多版本'))!.trigger('click')

    expect(wrapper.emitted('selectTemplate')?.[0]).toEqual([11])
    expect(wrapper.emitted('selectVersion')?.[0]).toEqual([2])
    expect(wrapper.emitted('loadMoreTemplates')).toHaveLength(1)
    expect(wrapper.emitted('loadMoreVersions')).toHaveLength(1)
  })

  it('disables operation-defining selections during exact retry', () => {
    const wrapper = mount(TemplateSelector, {
      props: { ...props, disabled: true },
    })

    expect(wrapper.findAll('button').every(button => button.attributes('disabled') !== undefined))
      .toBe(true)
  })
})
