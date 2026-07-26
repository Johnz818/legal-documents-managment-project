import type {
  CaseDetailResponse,
  CaseArchiveRequest,
  CaseCreateRequest,
  CaseListResponse,
  CaseSearchCriteria,
  CaseUpdateRequest,
} from '@/types/case'

const CASES_ENDPOINT = 'http://localhost:8080/api/cases'

export class CaseNotFoundError extends Error {
  constructor(caseId: number) {
    super(`Case ${caseId} was not found`)
    this.name = 'CaseNotFoundError'
  }
}

export class CaseConflictError extends Error {
  constructor() {
    super('Case number already exists')
    this.name = 'CaseConflictError'
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

export async function postCase(
  request: CaseCreateRequest,
): Promise<CaseDetailResponse> {
  const response = await fetch(CASES_ENDPOINT, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (response.status === 409) {
    throw new CaseConflictError()
  }

  if (!response.ok) {
    throw new Error(`Failed to create case: HTTP ${response.status}`)
  }

  return response.json() as Promise<CaseDetailResponse>
}

export async function putCase(
  caseId: number,
  request: CaseUpdateRequest,
): Promise<CaseDetailResponse> {
  const response = await fetch(`${CASES_ENDPOINT}/${caseId}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (response.status === 404) {
    throw new CaseNotFoundError(caseId)
  }

  if (response.status === 409) {
    throw new CaseConflictError()
  }

  if (!response.ok) {
    throw new Error(`Failed to update case ${caseId}: HTTP ${response.status}`)
  }

  return response.json() as Promise<CaseDetailResponse>
}

export async function postCaseArchiveState(
  caseId: number,
  action: 'archive' | 'restore',
  request: CaseArchiveRequest,
): Promise<CaseDetailResponse> {
  const response = await fetch(`${CASES_ENDPOINT}/${caseId}/${action}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (response.status === 404) {
    throw new CaseNotFoundError(caseId)
  }

  if (response.status === 409) {
    throw new CaseConflictError()
  }

  if (!response.ok) {
    throw new Error(`Failed to ${action} case ${caseId}: HTTP ${response.status}`)
  }

  return response.json() as Promise<CaseDetailResponse>
}
