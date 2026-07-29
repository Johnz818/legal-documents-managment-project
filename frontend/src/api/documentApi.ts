import type {
  CaseDocumentListResponse,
  CaseDocumentSummaryResponse,
} from '@/types/document'

const CASES_ENDPOINT = 'http://localhost:8080/api/cases'

export class DocumentApiError extends Error {
  readonly status: number

  constructor(operation: string, status: number) {
    super(`Failed to ${operation}: HTTP ${status}`)
    this.name = 'DocumentApiError'
    this.status = status
  }
}

export async function fetchCaseDocuments(
  caseId: number,
): Promise<CaseDocumentListResponse> {
  const response = await fetch(`${CASES_ENDPOINT}/${caseId}/documents`)

  if (!response.ok) {
    throw new DocumentApiError(`fetch documents for case ${caseId}`, response.status)
  }

  return response.json() as Promise<CaseDocumentListResponse>
}

export async function postCaseDocument(
  caseId: number,
  file: File,
): Promise<CaseDocumentSummaryResponse> {
  const body = new FormData()
  body.append('file', file)

  const response = await fetch(`${CASES_ENDPOINT}/${caseId}/documents`, {
    method: 'POST',
    body,
  })

  if (!response.ok) {
    throw new DocumentApiError(`upload document for case ${caseId}`, response.status)
  }

  return response.json() as Promise<CaseDocumentSummaryResponse>
}

export async function fetchCaseDocumentContent(
  caseId: number,
  documentId: number,
): Promise<Blob> {
  const response = await fetch(
    `${CASES_ENDPOINT}/${caseId}/documents/${documentId}/content`,
  )

  if (!response.ok) {
    throw new DocumentApiError(
      `download document ${documentId} for case ${caseId}`,
      response.status,
    )
  }

  return response.blob()
}

export async function deleteCaseDocument(
  caseId: number,
  documentId: number,
): Promise<void> {
  const response = await fetch(
    `${CASES_ENDPOINT}/${caseId}/documents/${documentId}`,
    { method: 'DELETE' },
  )

  if (!response.ok) {
    throw new DocumentApiError(
      `remove document ${documentId} from case ${caseId}`,
      response.status,
    )
  }
}
