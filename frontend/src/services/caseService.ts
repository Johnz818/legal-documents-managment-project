import {
  CaseConflictError,
  CaseNotFoundError,
  fetchCaseById,
  fetchCases,
  postCase,
} from '@/api/caseApi'
import type {
  CaseCreateRequest,
  CaseDetailResponse,
  CaseSearchCriteria,
  CaseSummaryResponse,
} from '@/types/case'

export class DuplicateCaseNumberError extends Error {
  constructor() {
    super('Case number already exists')
    this.name = 'DuplicateCaseNumberError'
  }
}

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

export async function createCase(
  request: CaseCreateRequest,
): Promise<CaseDetailResponse> {
  try {
    return await postCase(request)
  } catch (error) {
    if (error instanceof CaseConflictError) {
      throw new DuplicateCaseNumberError()
    }

    throw error
  }
}
