import {
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
