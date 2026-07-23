import { fetchCases } from '@/api/caseApi'
import type { CaseSummaryResponse } from '@/types/case'

export async function getCases(): Promise<CaseSummaryResponse[]> {
  const response = await fetchCases()
  return response.data
}
