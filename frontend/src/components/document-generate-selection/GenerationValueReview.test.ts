import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import GenerationValueReview from './GenerationValueReview.vue'
import type { PreparedGenerationField } from '@/types/documentGeneration'

const field = (overrides: Partial<PreparedGenerationField> = {}): PreparedGenerationField => ({
  fieldKey: 'case_number',
  displayName: '案号',
  description: '案件的正式案号',
  valueType: 'TEXT',
  required: true,
  defaultSource: 'CASE_FIELD',
  sourceKey: 'caseNumber',
  displayOrder: 0,
  suggestedValue: '(2026)沪01号',
  status: 'RESOLVED',
  ...overrides,
})

describe('GenerationValueReview', () => {
  it('shows field context and emits a reviewed text value', async () => {
    const wrapper = mount(GenerationValueReview, {
      props: {
        fields: [field()],
        values: { case_number: '(2026)沪01号' },
        fieldErrors: {},
        disabled: false,
        conflicts: [],
      },
    })

    expect(wrapper.text()).toContain('案件信息建议值')
    expect(wrapper.text()).toContain('case_number')
    expect((wrapper.get('input').element as HTMLInputElement).value).toBe('(2026)沪01号')
    await wrapper.get('input').setValue('(2026)沪02号')
    expect(wrapper.emitted('updateValue')?.[0]).toEqual(['case_number', '(2026)沪02号'])
  })

  it('retains every sequential value emitted by the shared Input contract', async () => {
    const wrapper = mount(GenerationValueReview, {
      props: {
        fields: [field({ defaultSource: 'USER_INPUT', sourceKey: null, suggestedValue: null })],
        values: { case_number: '' },
        fieldErrors: {},
        disabled: false,
        conflicts: [],
      },
    })

    await wrapper.get('input').setValue('123456')
    expect(wrapper.emitted('updateValue')?.at(-1)).toEqual(['case_number', '123456'])
    await wrapper.get('input').setValue('12')
    expect(wrapper.emitted('updateValue')?.at(-1)).toEqual(['case_number', '12'])
  })

  it('uses explicit boolean choices and locks controls during exact retry', () => {
    const wrapper = mount(GenerationValueReview, {
      props: {
        fields: [field({
          fieldKey: 'confirmed',
          displayName: '是否确认',
          valueType: 'BOOLEAN',
          defaultSource: 'USER_INPUT',
          sourceKey: null,
          suggestedValue: null,
          status: 'REQUIRES_USER_INPUT',
        })],
        values: { confirmed: '' },
        fieldErrors: { confirmed: '此字段为必填项。' },
        disabled: true,
        conflicts: [],
      },
    })

    expect(wrapper.get('select').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('需要手动输入')
    expect(wrapper.text()).toContain('此字段为必填项')
  })

  it('supports an empty template contract', () => {
    const wrapper = mount(GenerationValueReview, {
      props: {
        fields: [],
        values: {},
        fieldErrors: {},
        disabled: false,
        conflicts: [],
      },
    })

    expect(wrapper.text()).toContain('没有占位字段')
  })

  it('shows stale values and requires an explicit resolution', async () => {
    const wrapper = mount(GenerationValueReview, {
      props: {
        fields: [field()],
        values: { case_number: '(2026)沪01号' },
        fieldErrors: {},
        disabled: false,
        conflicts: [{
          fieldKey: 'case_number',
          displayName: '案号',
          previousValue: '(2026)沪01号',
          currentValue: '(2026)沪02号',
          currentSource: 'CASE_FIELD',
          resolution: null,
        }],
      },
    })

    expect(wrapper.text()).toContain('(2026)沪01号')
    expect(wrapper.text()).toContain('(2026)沪02号')
    expect(wrapper.get('input').attributes('disabled')).toBeDefined()
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('resolveConflict')?.[0]).toEqual([
      'case_number',
      'KEEP_PREVIOUS',
    ])
  })
})
