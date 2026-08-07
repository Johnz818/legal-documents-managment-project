import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateManagementPage from './TemplateManagementPage.vue'
import { fetchTemplates } from '@/services/documentTemplateService'

vi.mock('@/services/documentTemplateService', async importOriginal => ({
  ...await importOriginal<typeof import('@/services/documentTemplateService')>(),
  fetchTemplates: vi.fn(),
}))

const custom = {
  id: 3, name: '案件确认函', description: '受控 DOCX', templateType: 'CUSTOM' as const,
  createdAt: '2026-08-01T00:00:00', updatedAt: '2026-08-01T00:00:00',
}

describe('TemplateManagementPage', () => {
  beforeEach(() => {
    vi.mocked(fetchTemplates).mockResolvedValue({
      items: [custom], page: 0, size: 20, totalElements: 1, totalPages: 1,
    })
  })

  it('loads live templates and does not render legacy edit or delete actions', async () => {
    const wrapper = mount(TemplateManagementPage, {
      global: {
        stubs: {
          TemplatePublicationDialog: true,
          TemplateVersionDialog: true,
        },
      },
    })
    await flushPromises()
    expect(fetchTemplates).toHaveBeenCalledWith(0, 20)
    expect(wrapper.text()).toContain('案件确认函')
    expect(wrapper.text()).toContain('查看版本')
    expect(wrapper.text()).toContain('发布新版本')
    expect(wrapper.text()).not.toContain('删除')
    expect(wrapper.text()).not.toContain('编辑模板')
  })
})
