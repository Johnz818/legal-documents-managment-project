import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CaseListTable from '@/components/case-list-view/CaseListTable.vue'
import type { CaseSummaryResponse } from '@/types/case'

const summary: CaseSummaryResponse = {
  id: 7,
  caseNumber: '(2026)沪0115民初1001号',
  caseName: '张三诉某公司劳动争议案',
  status: '审理中',
  courtName: '上海市浦东新区人民法院',
  caseCause: '劳动争议',
  plaintiff: '张三',
  leadLawyerName: '李律师',
  filingDate: '2026-01-10',
  hearingDate: null,
  createdAt: '2026-01-10T10:00:00',
  updatedAt: '2026-01-10T10:00:00',
  archived: false,
}

interface TableProps {
  cases: CaseSummaryResponse[]
  isLoading: boolean
  errorMessage: string
  isFiltered: boolean
}

const mountTable = (
  props: Partial<TableProps> = {},
) => mount(CaseListTable, {
  props: {
    cases: [],
    isLoading: false,
    errorMessage: '',
    isFiltered: false,
    ...props,
  },
  global: {
    stubs: {
      SafeIcon: true,
      EmptyState: { template: '<div>暂无案件</div>' },
      Button: { template: '<button><slot /></button>' },
      Badge: { template: '<span><slot /></span>' },
      Table: { template: '<table><slot /></table>' },
      TableHeader: { template: '<thead><slot /></thead>' },
      TableBody: { template: '<tbody><slot /></tbody>' },
      TableRow: { template: '<tr><slot /></tr>' },
      TableHead: { template: '<th><slot /></th>' },
      TableCell: { template: '<td><slot /></td>' },
      DropdownMenu: { template: '<div><slot /></div>' },
      DropdownMenuTrigger: { template: '<div><slot /></div>' },
      DropdownMenuContent: { template: '<div><slot /></div>' },
      DropdownMenuItem: { template: '<a><slot /></a>' },
    },
  },
})

describe('CaseListTable', () => {
  it('renders the loading state', () => {
    expect(mountTable({ isLoading: true }).text()).toContain('正在加载案件')
  })

  it('renders and emits retry from the error state', async () => {
    const wrapper = mountTable({ errorMessage: '加载失败' })

    expect(wrapper.text()).toContain('加载失败')
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })

  it('distinguishes the default empty state from filtered no results', () => {
    expect(mountTable().text()).toContain('暂无案件')
    expect(mountTable({ isFiltered: true }).text()).toContain('未找到匹配的案件')
  })

  it('renders case summary data and a detail link', () => {
    const wrapper = mountTable({ cases: [summary] })

    expect(wrapper.text()).toContain(summary.caseNumber)
    expect(wrapper.text()).toContain(summary.courtName)
    expect(wrapper.text()).toContain(summary.leadLawyerName)
    expect(
      wrapper.get(`a[href="./case-detail-view.html?id=${summary.id}"]`)
        .attributes('href'),
    ).toBe(`./case-detail-view.html?id=${summary.id}`)
  })
})
