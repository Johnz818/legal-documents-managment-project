import type { CaseDetailResponse, CaseListResponse } from '@/types/case'

const CASES_ENDPOINT = 'http://localhost:8080/api/cases'

export class CaseNotFoundError extends Error {
  constructor(caseId: number) {
    super(`Case ${caseId} was not found`)
    this.name = 'CaseNotFoundError'
  }
}

export async function fetchCases(): Promise<CaseListResponse> {
  const response = await fetch(CASES_ENDPOINT)

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
