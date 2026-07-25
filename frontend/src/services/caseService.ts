import {
  CaseConflictError,
  CaseNotFoundError,
  fetchCaseById,
  fetchCases,
  postCase,
  putCase,
} from '@/api/caseApi'
import type {
  CaseCreateRequest,
  CaseDetailResponse,
  CaseSearchCriteria,
  CaseSummaryResponse,
  CaseUpdateRequest,
} from '@/types/case'

export class DuplicateCaseNumberError extends Error {
  constructor() {
    super('Case number already exists')
    this.name = 'DuplicateCaseNumberError'
  }
}

export class StaleCaseVersionError extends Error {
  constructor() {
    super('Case was modified by another request')
    this.name = 'StaleCaseVersionError'
  }
}

export class CaseUpdateNotFoundError extends Error {
  constructor() {
    super('Case no longer exists')
    this.name = 'CaseUpdateNotFoundError'
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

export async function updateCase(
  caseId: number,
  request: CaseUpdateRequest,
): Promise<CaseDetailResponse> {
  try {
    return await putCase(caseId, request)
  } catch (error) {
    if (error instanceof CaseNotFoundError) {
      throw new CaseUpdateNotFoundError()
    }

    if (error instanceof CaseConflictError) {
      const latestCase = await fetchCaseById(caseId)
      if (latestCase.version !== request.version) {
        throw new StaleCaseVersionError()
      }

      throw new DuplicateCaseNumberError()
    }

    throw error
  }
}
