export interface CaseSummaryResponse {
  id: number
  caseNumber: string
  caseName: string
  status: string
  courtName: string | null
  leadLawyerName: string | null
  createdAt: string
  updatedAt: string
  archived: boolean
}

export interface CaseListResponse {
  data: CaseSummaryResponse[]
}
