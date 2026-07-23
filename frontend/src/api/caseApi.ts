import type { CaseListResponse } from '@/types/case'

const CASES_ENDPOINT = 'http://localhost:8080/api/cases'

export async function fetchCases(): Promise<CaseListResponse> {
  const response = await fetch(CASES_ENDPOINT)

  if (!response.ok) {
    throw new Error(`Failed to fetch cases: HTTP ${response.status}`)
  }

  return response.json() as Promise<CaseListResponse>
}
