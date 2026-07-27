import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import CaseListContent from '@/components/case-list-view/CaseListContent.vue'
import { getCases } from '@/services/caseService'

vi.mock('@/services/caseService', () => ({
  getCases: vi.fn(),
}))

const FiltersStub = defineComponent({
  name: 'CaseListFilters',
  props: {
    isLoading: Boolean,
  },
  emits: ['search', 'reset'],
  template: '<div />',
})

const TableStub = defineComponent({
  name: 'CaseListTable',
  props: {
    cases: Array,
    isLoading: Boolean,
    errorMessage: String,
    isFiltered: Boolean,
  },
  emits: ['retry'],
  template: '<div />',
})

const mountContent = () => mount(CaseListContent, {
  global: {
    stubs: {
      CaseListFilters: FiltersStub,
      CaseListTable: TableStub,
    },
  },
})

describe('CaseListContent', () => {
  beforeEach(() => {
    vi.mocked(getCases).mockReset()
  })

  it('loads the default recent cases on mount', async () => {
    vi.mocked(getCases).mockResolvedValue([])

    const wrapper = mountContent()
    await flushPromises()

    expect(getCases).toHaveBeenCalledWith({})
    expect(wrapper.getComponent(TableStub).props()).toMatchObject({
      cases: [],
      isLoading: false,
      isFiltered: false,
    })
  })

  it('keeps an empty filtered result instead of loading recent cases', async () => {
    vi.mocked(getCases).mockResolvedValue([])
    const wrapper = mountContent()
    await flushPromises()

    wrapper.getComponent(FiltersStub).vm.$emit('search', {
      leadLawyerName: '张律师',
    })
    await flushPromises()

    expect(getCases).toHaveBeenNthCalledWith(2, {
      leadLawyerName: '张律师',
    })
    expect(getCases).toHaveBeenCalledTimes(2)
    expect(wrapper.getComponent(TableStub).props()).toMatchObject({
      cases: [],
      isFiltered: true,
    })
  })

  it('resets filters back to the default query', async () => {
    vi.mocked(getCases).mockResolvedValue([])
    const wrapper = mountContent()
    await flushPromises()

    wrapper.getComponent(FiltersStub).vm.$emit('search', {
      status: 'IN_TRIAL',
    })
    await flushPromises()
    wrapper.getComponent(FiltersStub).vm.$emit('reset')
    await flushPromises()

    expect(getCases).toHaveBeenLastCalledWith({})
    expect(wrapper.getComponent(TableStub).props('isFiltered')).toBe(false)
  })

  it('shows an error and retries the same query', async () => {
    vi.mocked(getCases)
      .mockRejectedValueOnce(new Error('backend unavailable'))
      .mockResolvedValueOnce([])

    const wrapper = mountContent()
    await flushPromises()

    const table = wrapper.getComponent(TableStub)
    expect(table.props('errorMessage')).toContain('案件数据加载失败')

    table.vm.$emit('retry')
    await flushPromises()

    expect(getCases).toHaveBeenNthCalledWith(2, {})
    expect(wrapper.getComponent(TableStub).props('errorMessage')).toBe('')
  })
})
