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
}
