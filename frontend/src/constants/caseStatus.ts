import type { CaseStatusCode } from '@/types/case'

export const CASE_STATUS_VALUES = [
  'PENDING_FILING',
  'PRE_TRIAL_PREPARATION',
  'IN_TRIAL',
  'JUDGMENT_PENDING_APPEAL',
  'APPEAL_IN_PROGRESS',
  'FINAL_JUDGMENT',
  'IN_ENFORCEMENT',
  'CLOSED',
] as const satisfies readonly CaseStatusCode[]

export const CASE_STATUS_OPTIONS: ReadonlyArray<{
  value: CaseStatusCode
  label: string
}> = [
  { value: 'PENDING_FILING', label: '待立案' },
  { value: 'PRE_TRIAL_PREPARATION', label: '审理准备' },
  { value: 'IN_TRIAL', label: '审理中' },
  { value: 'JUDGMENT_PENDING_APPEAL', label: '已判决(上诉期内)' },
  { value: 'APPEAL_IN_PROGRESS', label: '上诉审理中' },
  { value: 'FINAL_JUDGMENT', label: '已判决(生效)' },
  { value: 'IN_ENFORCEMENT', label: '执行中' },
  { value: 'CLOSED', label: '已结案' },
]

export function getCaseStatusCode(displayLabel: string): CaseStatusCode | undefined {
  return CASE_STATUS_OPTIONS.find(option => option.label === displayLabel)?.value
}
