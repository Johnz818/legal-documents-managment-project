import type {
  BackendProblemDetail,
  DocumentTemplateSummary,
  DocumentTemplateVersionSummary,
  PageResponse,
  PublishedTemplateVersion,
} from '@/types/documentGeneration'

const ENDPOINT = '/api/document-templates'

export class DocumentTemplateApiError extends Error {
  readonly status: number
  readonly problem: BackendProblemDetail | null

  constructor(operation: string, status: number, problem: BackendProblemDetail | null) {
    super(problem?.detail ?? `Failed to ${operation}: HTTP ${status}`)
    this.name = 'DocumentTemplateApiError'
    this.status = status
    this.problem = problem
  }

  get code(): string | undefined {
    return this.problem?.code
  }
}

const parseProblem = async (response: Response): Promise<BackendProblemDetail | null> => {
  try {
    const body = await response.json() as unknown
    return body && typeof body === 'object' && !Array.isArray(body)
      ? body as BackendProblemDetail
      : null
  } catch {
    return null
  }
}

const requireJson = async <T>(response: Response, operation: string): Promise<T> => {
  if (!response.ok) {
    throw new DocumentTemplateApiError(operation, response.status, await parseProblem(response))
  }
  return response.json() as Promise<T>
}

const pageQuery = (page: number, size: number) => new URLSearchParams({
  page: String(page), size: String(size),
})

export async function fetchTemplates(page = 0, size = 20): Promise<PageResponse<DocumentTemplateSummary>> {
  return requireJson(await fetch(`${ENDPOINT}?${pageQuery(page, size)}`), 'fetch document templates')
}

export async function fetchTemplateVersions(
  templateId: number,
  page = 0,
  size = 20,
): Promise<PageResponse<DocumentTemplateVersionSummary>> {
  return requireJson(
    await fetch(`${ENDPOINT}/${templateId}/versions?${pageQuery(page, size)}`),
    `fetch versions for template ${templateId}`,
  )
}

export async function fetchTemplateVersion(
  templateId: number,
  versionNumber: number,
): Promise<PublishedTemplateVersion> {
  return requireJson(
    await fetch(`${ENDPOINT}/${templateId}/versions/${versionNumber}`),
    `fetch template ${templateId} version ${versionNumber}`,
  )
}
