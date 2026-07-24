import { CaseNotFoundError, fetchCaseById, fetchCases } from '@/api/caseApi'
import type {
  CaseDetailResponse,
  CaseSearchCriteria,
  CaseSummaryResponse,
} from '@/types/case'

export async function getCases(
  criteria: CaseSearchCriteria = {},
): Promise<CaseSummaryResponse[]> {
  const response = await fetchCases(criteria)
  return response.data
}

export async function getCaseById(caseId: number): Promise<CaseDetailResponse | null> {
  try {
    return await fetchCaseById(caseId)
  } catch (error) {
    if (error instanceof CaseNotFoundError) {
      return null
    }

    throw error
  }
}
