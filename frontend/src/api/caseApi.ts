import type {
  CaseDetailResponse,
  CaseListResponse,
  CaseSearchCriteria,
} from '@/types/case'

const CASES_ENDPOINT = 'http://localhost:8080/api/cases'

export class CaseNotFoundError extends Error {
  constructor(caseId: number) {
    super(`Case ${caseId} was not found`)
    this.name = 'CaseNotFoundError'
  }
}

export async function fetchCases(
  criteria: CaseSearchCriteria = {},
): Promise<CaseListResponse> {
  const query = new URLSearchParams()

  Object.entries(criteria).forEach(([name, value]) => {
    if (value) {
      query.set(name, value)
    }
  })

  const endpoint = query.size > 0
    ? `${CASES_ENDPOINT}?${query.toString()}`
    : CASES_ENDPOINT
  const response = await fetch(endpoint)

  if (!response.ok) {
    throw new Error(`Failed to fetch cases: HTTP ${response.status}`)
  }

  return response.json() as Promise<CaseListResponse>
}

export async function fetchCaseById(caseId: number): Promise<CaseDetailResponse> {
  const response = await fetch(`${CASES_ENDPOINT}/${caseId}`)

  if (response.status === 404) {
    throw new CaseNotFoundError(caseId)
  }

  if (!response.ok) {
    throw new Error(`Failed to fetch case ${caseId}: HTTP ${response.status}`)
  }

  return response.json() as Promise<CaseDetailResponse>
}
