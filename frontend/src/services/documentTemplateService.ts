import {
  DocumentTemplateApiError,
  downloadTemplateVersion,
  fetchTemplateVersion,
  fetchTemplateVersions,
  fetchTemplates,
  inspectTemplate,
  publishTemplate,
  publishTemplateVersion,
} from '@/api/documentTemplateApi'
import type { DocumentFieldSource, DocumentFieldValueType } from '@/types/documentGeneration'
import type {
  DetectedTemplateMarker,
  NewTemplatePublicationRequest,
  TemplateErrorPresentation,
  TemplateFieldPublicationRequest,
  TemplatePublicationDraft,
  TemplatePublicationValidation,
  TemplateVersionPublicationRequest,
} from '@/types/documentTemplate'

export const CASE_SOURCE_OPTIONS: ReadonlyArray<{ key: string; type: DocumentFieldValueType; label: string }> = [
  { key: 'caseNumber', type: 'TEXT', label: '案号' },
  { key: 'caseName', type: 'TEXT', label: '案件名称' },
  { key: 'courtName', type: 'TEXT', label: '法院' },
  { key: 'caseCause', type: 'TEXT', label: '案由' },
  { key: 'plaintiff', type: 'TEXT', label: '原告/申请人' },
  { key: 'defendant', type: 'TEXT', label: '被告/被申请人' },
  { key: 'leadLawyerName', type: 'TEXT', label: '主办律师' },
  { key: 'description', type: 'TEXT', label: '案件描述' },
  { key: 'filingDate', type: 'DATE', label: '立案日期' },
  { key: 'hearingDate', type: 'DATE', label: '开庭日期' },
  { key: 'judgmentDate', type: 'DATE', label: '判决日期' },
]

export const SYSTEM_SOURCE_OPTIONS = [{ key: 'currentDate', type: 'DATE' as const, label: '当前日期' }]
const FIELD_KEY = /^[a-z][a-z0-9_]{0,99}$/

export const markerId = (marker: Pick<DetectedTemplateMarker, 'kind' | 'value'>) => `${marker.kind}:${marker.value}`

export function initialPublicationDrafts(markers: DetectedTemplateMarker[]): {
  drafts: TemplatePublicationDraft[]
  assignments: Record<string, string>
} {
  const drafts = markers.map((marker, index) => ({
    clientId: `field-${index}`,
    fieldKey: marker.kind === 'CANONICAL' ? marker.value : '',
    displayName: marker.value,
    description: '',
    valueType: 'TEXT' as const,
    required: true,
    defaultSource: 'USER_INPUT' as const,
    sourceKey: '',
  }))
  return {
    drafts,
    assignments: Object.fromEntries(markers.map((marker, index) => [markerId(marker), `field-${index}`])),
  }
}

const compatibleSource = (source: DocumentFieldSource, sourceKey: string, type: DocumentFieldValueType) => {
  if (source === 'USER_INPUT') return sourceKey === ''
  const options = source === 'CASE_FIELD' ? CASE_SOURCE_OPTIONS : SYSTEM_SOURCE_OPTIONS
  return options.some(option => option.key === sourceKey && option.type === type)
}

export function validatePublication(
  markers: DetectedTemplateMarker[],
  drafts: TemplatePublicationDraft[],
  assignments: Readonly<Record<string, string>>,
): TemplatePublicationValidation {
  const activeIds = new Set(Object.values(assignments))
  const active = drafts.filter(draft => activeIds.has(draft.clientId))
  const fieldErrors: Record<string, Record<string, string>> = {}
  const seenKeys = new Set<string>()

  for (const draft of active) {
    const errors: Record<string, string> = {}
    if (!FIELD_KEY.test(draft.fieldKey)) errors.fieldKey = '字段键必须以小写字母开头，仅包含小写字母、数字和下划线，最长 100 个字符。'
    else if (seenKeys.has(draft.fieldKey)) errors.fieldKey = '字段键在此版本中必须唯一。'
    seenKeys.add(draft.fieldKey)
    if (!draft.displayName.trim() || draft.displayName.length > 200) errors.displayName = '显示名称不能为空且不能超过 200 个字符。'
    if (draft.description.length > 1000) errors.description = '字段说明不能超过 1000 个字符。'
    if (!compatibleSource(draft.defaultSource, draft.sourceKey, draft.valueType)) errors.sourceKey = '值来源与字段类型不兼容。'
    if (Object.keys(errors).length) fieldErrors[draft.clientId] = errors
  }

  let formError: string | undefined
  if (markers.some(marker => !assignments[markerId(marker)])) formError = '每个检测到的标记都必须分配到一个字段。'
  for (const marker of markers.filter(item => item.kind === 'CANONICAL')) {
    const draft = drafts.find(item => item.clientId === assignments[markerId(marker)])
    if (draft?.fieldKey !== marker.value) formError = '规范标记必须保留其自身字段键。'
  }
  return { valid: !formError && Object.keys(fieldErrors).length === 0, fieldErrors, formError }
}

export function buildFields(
  markers: DetectedTemplateMarker[],
  drafts: TemplatePublicationDraft[],
  assignments: Readonly<Record<string, string>>,
): TemplateFieldPublicationRequest[] {
  const activeIds = [...new Set(Object.values(assignments))]
  return activeIds.map(clientId => {
    const draft = drafts.find(item => item.clientId === clientId)!
    return {
      fieldKey: draft.fieldKey,
      displayName: draft.displayName.trim(),
      description: draft.description.trim() || null,
      valueType: draft.valueType,
      required: draft.required,
      defaultSource: draft.defaultSource,
      sourceKey: draft.defaultSource === 'USER_INPUT' ? null : draft.sourceKey,
      markers: markers.filter(marker => assignments[markerId(marker)] === clientId)
        .map(marker => ({ kind: marker.kind, value: marker.value })),
    }
  })
}

export const presentTemplateError = (error: unknown, fallback = '模板操作失败'): TemplateErrorPresentation => {
  if (!(error instanceof DocumentTemplateApiError)) return { summary: fallback }
  return {
    summary: typeof error.problem?.title === 'string' ? error.problem.title : fallback,
    detail: typeof error.problem?.detail === 'string' ? error.problem.detail : undefined,
    code: error.code,
  }
}

export {
  downloadTemplateVersion,
  fetchTemplateVersion,
  fetchTemplateVersions,
  fetchTemplates,
  inspectTemplate,
  publishTemplate,
  publishTemplateVersion,
}

export type { NewTemplatePublicationRequest, TemplateVersionPublicationRequest }
