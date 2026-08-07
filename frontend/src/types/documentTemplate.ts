import type { BackendProblemDetail, DocumentFieldSource, DocumentFieldValueType, PageResponse } from './documentGeneration'

export type TemplateMarkerKind = 'CHINESE' | 'CANONICAL'

export interface DetectedTemplateMarker {
  kind: TemplateMarkerKind
  value: string
  occurrenceCount: number
}

export interface TemplateInspectionResponse {
  markers: DetectedTemplateMarker[]
}

export interface TemplateMarkerRequest {
  kind: TemplateMarkerKind
  value: string
}

export interface TemplateFieldPublicationRequest {
  fieldKey: string
  displayName: string
  description: string | null
  valueType: DocumentFieldValueType
  required: boolean
  defaultSource: DocumentFieldSource
  sourceKey: string | null
  markers: TemplateMarkerRequest[]
}

export interface NewTemplatePublicationRequest {
  name: string
  description: string | null
  fields: TemplateFieldPublicationRequest[]
}

export interface TemplateVersionPublicationRequest {
  fields: TemplateFieldPublicationRequest[]
}

export interface TemplatePublicationDraft {
  clientId: string
  fieldKey: string
  displayName: string
  description: string
  valueType: DocumentFieldValueType
  required: boolean
  defaultSource: DocumentFieldSource
  sourceKey: string
}

export interface TemplatePublicationValidation {
  valid: boolean
  fieldErrors: Record<string, Record<string, string>>
  formError?: string
}

export interface TemplateErrorPresentation {
  summary: string
  detail?: string
  code?: string
}

export type { BackendProblemDetail, PageResponse }
