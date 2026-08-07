import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplatePublicationDialog from './TemplatePublicationDialog.vue'
import { inspectTemplate, publishTemplate } from '@/services/documentTemplateService'

vi.mock('@/services/documentTemplateService', async importOriginal => ({
  ...await importOriginal<typeof import('@/services/documentTemplateService')>(),
  inspectTemplate: vi.fn(),
  publishTemplate: vi.fn(),
  publishTemplateVersion: vi.fn(),
}))

describe('TemplatePublicationDialog', () => {
  beforeEach(() => {
    vi.mocked(inspectTemplate).mockResolvedValue({ markers: [] })
    vi.mocked(publishTemplate).mockResolvedValue({
      templateId: 3,
      templateName: '无字段模板',
      templateDescription: null,
      versionNumber: 1,
      originalFileName: 'empty.docx',
      contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      fileSize: 20,
      contentSha256: 'a'.repeat(64),
      publishedAt: '2026-08-07T00:00:00',
      fields: [],
    })
  })

  it('retains one file from inspection through empty-contract publication', async () => {
    const wrapper = mount(TemplatePublicationDialog, {
      props: { open: true, template: null },
      global: {
        stubs: {
          Dialog: { template: '<div><slot /></div>' },
          DialogContent: { template: '<div><slot /></div>' },
          DialogHeader: { template: '<div><slot /></div>' },
          DialogTitle: { template: '<h2><slot /></h2>' },
          DialogDescription: { template: '<p><slot /></p>' },
        },
      },
    })
    await wrapper.findAll('input')[0].setValue('无字段模板')
    const fileInput = wrapper.get('input[type="file"]')
    const file = new File(['docx'], 'empty.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    Object.defineProperty(fileInput.element, 'files', { configurable: true, value: [file] })
    await fileInput.trigger('change')
    const inspectButton = wrapper.findAll('button').find(button => button.text().includes('检查 DOCX'))!
    await inspectButton.trigger('click')
    await flushPromises()

    expect(inspectTemplate).toHaveBeenCalledWith(file)
    expect(wrapper.text()).toContain('空字段合同')
    await wrapper.findAll('button').find(button => button.text().includes('审核发布内容'))!.trigger('click')
    await wrapper.findAll('button').find(button => button.text().includes('确认并发布'))!.trigger('click')
    await flushPromises()

    expect(publishTemplate).toHaveBeenCalledWith(file, {
      name: '无字段模板', description: null, fields: [],
    })
    expect(wrapper.emitted('published')).toHaveLength(1)
    wrapper.unmount()
  })

  it('provides accessible syntax, field-key, and field-group guidance before errors', async () => {
    vi.mocked(inspectTemplate).mockResolvedValue({
      markers: [{ kind: 'CHINESE', value: '案件编号', occurrenceCount: 1 }],
    })
    const wrapper = mount(TemplatePublicationDialog, {
      props: { open: true, template: null },
      attachTo: document.body,
      global: {
        stubs: {
          Dialog: { template: '<div><slot /></div>' },
          DialogContent: { template: '<div><slot /></div>' },
          DialogHeader: { template: '<div><slot /></div>' },
          DialogTitle: { template: '<h2><slot /></h2>' },
          DialogDescription: { template: '<p><slot /></p>' },
          SafeIcon: true,
        },
      },
    })

    const syntaxHelp = wrapper.get('button[aria-label="查看 DOCX 占位符语法说明"]')
    expect(syntaxHelp.attributes('type')).toBe('button')
    await syntaxHelp.trigger('focus')
    await flushPromises()
    expect(document.body.textContent).toContain('{{case_number}}')
    expect(document.body.textContent).toContain('花括号内不能包含空格')

    await wrapper.findAll('input')[0].setValue('中文标记模板')
    const fileInput = wrapper.get('input[type="file"]')
    const file = new File(['docx'], 'markers.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    Object.defineProperty(fileInput.element, 'files', { configurable: true, value: [file] })
    await fileInput.trigger('change')
    await wrapper.findAll('button').find(button => button.text().includes('检查 DOCX'))!.trigger('click')
    await flushPromises()

    const groupHelp = wrapper.get('button[aria-label="查看字段组说明"]')
    expect(groupHelp.attributes('type')).toBe('button')
    await groupHelp.trigger('focus')
    await flushPromises()
    expect(document.body.textContent).toContain('多个占位符共享一个字段值')
    const fieldKeyHelp = wrapper.get('button[aria-label="查看字段键说明"]')
    expect(fieldKeyHelp.attributes('type')).toBe('button')
    await fieldKeyHelp.trigger('focus')
    await flushPromises()
    expect(document.body.textContent).toContain('中文标记需要手动填写')
    expect(wrapper.get('select[aria-label="为 案件编号 选择字段组"]').text())
      .toContain('映射到字段组：案件编号')
    wrapper.unmount()
  })
})
