import {
  deleteCaseDocument,
  DocumentApiError,
  fetchCaseDocumentContent,
  fetchCaseDocuments,
  postCaseDocument,
} from '@/api/documentApi'
import type { CaseDocumentSummaryResponse } from '@/types/document'

export type DocumentUploadFailure =
  | 'invalid'
  | 'not-found'
  | 'too-large'
  | 'unsupported'
  | 'unexpected'

export class DocumentUploadError extends Error {
  readonly reason: DocumentUploadFailure

  constructor(reason: DocumentUploadFailure) {
    super(`Document upload failed: ${reason}`)
    this.name = 'DocumentUploadError'
    this.reason = reason
  }
}

export type DocumentRemovalFailure = 'not-found' | 'unexpected'

export class DocumentRemovalError extends Error {
  readonly reason: DocumentRemovalFailure

  constructor(reason: DocumentRemovalFailure) {
    super(`Document removal failed: ${reason}`)
    this.name = 'DocumentRemovalError'
    this.reason = reason
  }
}

export async function getCaseDocuments(
  caseId: number,
): Promise<CaseDocumentSummaryResponse[]> {
  const response = await fetchCaseDocuments(caseId)
  return response.data
}

export async function uploadCaseDocument(
  caseId: number,
  file: File,
): Promise<CaseDocumentSummaryResponse> {
  try {
    return await postCaseDocument(caseId, file)
  } catch (error) {
    if (!(error instanceof DocumentApiError)) {
      throw error
    }

    const reasonByStatus: Partial<Record<number, DocumentUploadFailure>> = {
      400: 'invalid',
      404: 'not-found',
      413: 'too-large',
      415: 'unsupported',
    }
    throw new DocumentUploadError(reasonByStatus[error.status] ?? 'unexpected')
  }
}

export function downloadCaseDocument(
  caseId: number,
  documentId: number,
): Promise<Blob> {
  return fetchCaseDocumentContent(caseId, documentId)
}

export async function removeCaseDocument(
  caseId: number,
  documentId: number,
): Promise<void> {
  try {
    await deleteCaseDocument(caseId, documentId)
  } catch (error) {
    if (!(error instanceof DocumentApiError)) {
      throw error
    }

    throw new DocumentRemovalError(
      error.status === 404 ? 'not-found' : 'unexpected',
    )
  }
}
