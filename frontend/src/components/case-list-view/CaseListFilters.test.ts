import { defineComponent } from 'vue'
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CaseListFilters from '@/components/case-list-view/CaseListFilters.vue'

const InputStub = defineComponent({
  props: {
    modelValue: String,
  },
  emits: ['update:modelValue'],
  template: `
    <input
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `,
})

const SelectStub = defineComponent({
  props: {
    modelValue: String,
  },
  emits: ['update:modelValue'],
  template: `
    <select
      :value="modelValue"
      @change="$emit('update:modelValue', $event.target.value)"
    >
      <slot />
    </select>
  `,
})

const SelectItemStub = defineComponent({
  props: {
    value: String,
  },
  template: '<option :value="value"><slot /></option>',
})

const mountFilters = (isLoading = false) => mount(CaseListFilters, {
  props: { isLoading },
  global: {
    stubs: {
      Input: InputStub,
      Select: SelectStub,
      SelectContent: { template: '<slot />' },
      SelectItem: SelectItemStub,
      SelectTrigger: { template: '<slot />' },
      SelectValue: true,
      SafeIcon: true,
      Button: {
        props: ['disabled', 'type'],
        template: '<button :disabled="disabled" :type="type"><slot /></button>',
      },
    },
  },
})

describe('CaseListFilters', () => {
  it('normalizes and emits all supported search criteria', async () => {
    const wrapper = mountFilters()
    const inputs = wrapper.findAll('input')
    const selects = wrapper.findAll('select')

    await inputs[0].setValue('  (2026)沪  ')
    await inputs[1].setValue('  劳动争议  ')
    await selects[0].setValue('IN_TRIAL')
    await inputs[2].setValue('  张律师  ')
    await selects[1].setValue('ARCHIVED')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('search')).toEqual([[
      {
        caseNumberPrefix: '(2026)沪',
        caseNamePrefix: '劳动争议',
        status: 'IN_TRIAL',
        leadLawyerName: '张律师',
        archiveState: 'ARCHIVED',
      },
    ]])
  })

  it('omits blank and default filter values', async () => {
    const wrapper = mountFilters()

    await wrapper.findAll('input')[0].setValue('   ')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('search')).toEqual([[{}]])
  })

  it('resets every filter and emits reset', async () => {
    const wrapper = mountFilters()
    const inputs = wrapper.findAll('input')
    const selects = wrapper.findAll('select')

    await inputs[0].setValue('(2026)沪')
    await inputs[1].setValue('劳动争议')
    await selects[0].setValue('IN_TRIAL')
    await inputs[2].setValue('张律师')
    await selects[1].setValue('ARCHIVED')

    const resetButton = wrapper.findAll('button')
      .find(button => button.text().includes('重置'))
    expect(resetButton).toBeDefined()
    await resetButton!.trigger('click')

    expect(inputs.map(input => input.element.value)).toEqual(['', '', ''])
    expect(selects.map(select => select.element.value)).toEqual(['ALL', 'ACTIVE'])
    expect(wrapper.emitted('reset')).toHaveLength(1)
  })

  it('disables search actions while loading', () => {
    const wrapper = mountFilters(true)

    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
  })
})
