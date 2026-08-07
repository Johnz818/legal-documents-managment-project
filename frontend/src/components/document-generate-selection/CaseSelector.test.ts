import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CaseSelector from './CaseSelector.vue'
import type { CaseSummaryResponse } from '@/types/case'

const cases = [{
  id: 7,
  caseNumber: '(2026)沪01号',
  caseName: '张三诉某公司劳动争议案',
  status: '审理中',
  courtName: '上海市某法院',
  caseCause: '劳动争议',
  plaintiff: '张三',
  leadLawyerName: '李律师',
}] as CaseSummaryResponse[]

describe('CaseSelector', () => {
  it('filters live summaries and emits the numeric Case id', async () => {
    const wrapper = mount(CaseSelector, {
      props: {
        cases,
        selectedCaseId: null,
        isLoading: false,
        errorMessage: '',
        disabled: false,
      },
    })

    await wrapper.get('input').setValue('张三')
    expect(wrapper.text()).toContain('(2026)沪01号')
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('select')?.[0]).toEqual([7])

    await wrapper.get('input').setValue('不存在')
    expect(wrapper.text()).toContain('未找到匹配的案件')
  })

  it('offers retry after a live Case loading failure', async () => {
    const wrapper = mount(CaseSelector, {
      props: {
        cases: [],
        selectedCaseId: null,
        isLoading: false,
        errorMessage: '案件数据加载失败',
        disabled: false,
      },
    })

    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })
})
