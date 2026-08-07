import type {
  BackendProblemDetail,
  DocumentGenerationRequest,
  GeneratedDocument,
  GenerationContext,
  GenerationPreparation,
} from '@/types/documentGeneration'

const CASES_ENDPOINT = '/api/cases'

export class DocumentGenerationApiError extends Error {
  readonly status: number
  readonly problem: BackendProblemDetail | null

  constructor(operation: string, status: number, problem: BackendProblemDetail | null) {
    super(problem?.detail ?? `Failed to ${operation}: HTTP ${status}`)
    this.name = 'DocumentGenerationApiError'
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
    if (body === null || typeof body !== 'object' || Array.isArray(body)) {
      return null
    }
    return body as BackendProblemDetail
  } catch {
    return null
  }
}

const requireJson = async <T>(response: Response, operation: string): Promise<T> => {
  if (!response.ok) {
    throw new DocumentGenerationApiError(
      operation,
      response.status,
      await parseProblem(response),
    )
  }
  return response.json() as Promise<T>
}

const generationQuery = (context: GenerationContext) => new URLSearchParams({
  templateId: String(context.templateId),
  versionNumber: String(context.versionNumber),
  timezone: context.timezone,
})

export async function fetchGenerationPreparation(
  context: GenerationContext,
): Promise<GenerationPreparation> {
  const response = await fetch(
    `${CASES_ENDPOINT}/${context.caseId}/document-generations/preparation?${generationQuery(context)}`,
  )
  return requireJson(response, `prepare document generation for case ${context.caseId}`)
}

export async function postDocumentGeneration(
  context: GenerationContext,
  idempotencyKey: string,
  request: Readonly<{ values: readonly Readonly<DocumentGenerationRequest['values'][number]>[] }>,
): Promise<GeneratedDocument> {
  const response = await fetch(
    `${CASES_ENDPOINT}/${context.caseId}/document-generations?${generationQuery(context)}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify(request),
    },
  )
  return requireJson(response, `generate document for case ${context.caseId}`)
}
