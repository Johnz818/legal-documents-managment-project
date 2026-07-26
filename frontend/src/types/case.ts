export type CaseStatusCode =
  | 'PENDING_FILING'
  | 'PRE_TRIAL_PREPARATION'
  | 'IN_TRIAL'
  | 'JUDGMENT_PENDING_APPEAL'
  | 'APPEAL_IN_PROGRESS'
  | 'FINAL_JUDGMENT'
  | 'IN_ENFORCEMENT'
  | 'CLOSED'

export type CaseArchiveState = 'ACTIVE' | 'ARCHIVED'

export interface CaseSearchCriteria {
  caseNumberPrefix?: string
  caseNamePrefix?: string
  status?: CaseStatusCode
  leadLawyerName?: string
  archiveState?: CaseArchiveState
}

export interface CaseCreateRequest {
  caseNumber: string
  caseName: string
  status: CaseStatusCode
  courtName?: string | null
  caseCause?: string | null
  plaintiff: string
  defendant: string
  leadLawyerName: string
  filingDate?: string | null
  hearingDate?: string | null
  judgmentDate?: string | null
  description?: string | null
}

export interface CaseUpdateRequest extends CaseCreateRequest {
  version: number
}

export interface CaseArchiveRequest {
  version: number
}

export interface CaseSummaryResponse {
  id: number
  caseNumber: string
  caseName: string
  status: string
  courtName: string | null
  caseCause: string | null
  plaintiff: string
  leadLawyerName: string
  filingDate: string | null
  hearingDate: string | null
  createdAt: string
  updatedAt: string
  archived: boolean
}

export interface CaseListResponse {
  data: CaseSummaryResponse[]
}

export interface CaseDetailResponse {
  id: number
  caseNumber: string
  caseName: string
  status: string
  courtName: string | null
  caseCause: string | null
  plaintiff: string
  defendant: string
  leadLawyerName: string
  filingDate: string | null
  hearingDate: string | null
  judgmentDate: string | null
  description: string | null
  createdAt: string
  updatedAt: string
  archived: boolean
  version: number
}
