import { describe, expect, it } from 'vitest'
import { buildFields, initialPublicationDrafts, markerId, validatePublication } from './documentTemplateService'
import type { DetectedTemplateMarker } from '@/types/documentTemplate'

const markers: DetectedTemplateMarker[] = [
  { kind: 'CANONICAL', value: 'case_number', occurrenceCount: 1 },
  { kind: 'CHINESE', value: '案号', occurrenceCount: 2 },
]

describe('documentTemplateService', () => {
  it('keeps canonical keys and supports explicit many-to-one grouping', () => {
    const state = initialPublicationDrafts(markers)
    const canonicalId = state.assignments[markerId(markers[0])]
    state.assignments[markerId(markers[1])] = canonicalId
    const draft = state.drafts.find(item => item.clientId === canonicalId)!
    draft.displayName = '案号'

    expect(validatePublication(markers, state.drafts, state.assignments).valid).toBe(true)
    expect(buildFields(markers, state.drafts, state.assignments)).toEqual([
      expect.objectContaining({
        fieldKey: 'case_number',
        markers: [
          { kind: 'CANONICAL', value: 'case_number' },
          { kind: 'CHINESE', value: '案号' },
        ],
      }),
    ])
  })

  it('rejects canonical remapping and incompatible sources', () => {
    const state = initialPublicationDrafts(markers.slice(0, 1))
    state.drafts[0].fieldKey = 'other_key'
    state.drafts[0].defaultSource = 'SYSTEM_VALUE'
    state.drafts[0].sourceKey = 'currentDate'
    expect(validatePublication(markers.slice(0, 1), state.drafts, state.assignments)).toMatchObject({
      valid: false,
      formError: '规范标记必须保留其自身字段键。',
    })
  })

  it('allows an empty marker and field contract', () => {
    expect(validatePublication([], [], {})).toEqual({ valid: true, fieldErrors: {}, formError: undefined })
    expect(buildFields([], [], {})).toEqual([])
  })
})
