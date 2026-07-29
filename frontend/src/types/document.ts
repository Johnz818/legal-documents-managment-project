export type DocumentSource = 'UPLOADED' | 'GENERATED'

export type DocumentFormat = 'PDF' | 'DOC' | 'DOCX'

export interface CaseDocumentSummaryResponse {
  id: number
  caseId: number
  originalFileName: string
  documentSource: DocumentSource
  fileFormat: DocumentFormat
  contentType: string
  fileSize: number
  createdAt: string
  updatedAt: string
}

export interface CaseDocumentListResponse {
  data: CaseDocumentSummaryResponse[]
}
