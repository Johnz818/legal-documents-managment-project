import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  CaseConflictError,
  CaseNotFoundError,
  fetchCaseById,
  fetchCases,
  postCase,
  postCaseArchiveState,
  putCase,
} from '@/api/caseApi'
import type {
  CaseCreateRequest,
  CaseDetailResponse,
  CaseUpdateRequest,
} from '@/types/case'

const caseDetail: CaseDetailResponse = {
  id: 7,
  caseNumber: '(2026)沪0115民初1001号',
  caseName: '张三诉某公司劳动争议案',
  status: '审理中',
  courtName: '上海市浦东新区人民法院',
  caseCause: '劳动争议',
  plaintiff: '张三',
  defendant: '某公司',
  leadLawyerName: '李律师',
  filingDate: '2026-01-10',
  hearingDate: null,
  judgmentDate: null,
  description: null,
  createdAt: '2026-01-10T10:00:00',
  updatedAt: '2026-01-10T10:00:00',
  archived: false,
  version: 0,
}

const createRequest: CaseCreateRequest = {
  caseNumber: caseDetail.caseNumber,
  caseName: caseDetail.caseName,
  status: 'IN_TRIAL',
  plaintiff: caseDetail.plaintiff,
  defendant: caseDetail.defendant,
  leadLawyerName: caseDetail.leadLawyerName,
}

const updateRequest: CaseUpdateRequest = {
  ...createRequest,
  version: 2,
}

const response = (
  status: number,
  body: unknown = caseDetail,
): Response => ({
  ok: status >= 200 && status < 300,
  status,
  json: vi.fn().mockResolvedValue(body),
}) as unknown as Response

describe('caseApi', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  it('fetches the default case list without a query string', async () => {
    fetchMock.mockResolvedValue(response(200, { data: [] }))

    await expect(fetchCases()).resolves.toEqual({ data: [] })
    expect(fetchMock).toHaveBeenCalledWith('/api/cases')
  })

  it('adds only populated search criteria to the list request', async () => {
    fetchMock.mockResolvedValue(response(200, { data: [] }))

    await fetchCases({
      caseNumberPrefix: '(2026)沪',
      leadLawyerName: '',
      archiveState: 'ARCHIVED',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/cases?caseNumberPrefix=%282026%29%E6%B2%AA&archiveState=ARCHIVED',
    )
  })

  it('rejects unsuccessful list requests', async () => {
    fetchMock.mockResolvedValue(response(503))

    await expect(fetchCases()).rejects.toThrow(
      'Failed to fetch cases: HTTP 503',
    )
  })

  it('translates a missing case detail into CaseNotFoundError', async () => {
    fetchMock.mockResolvedValue(response(404))

    await expect(fetchCaseById(7)).rejects.toBeInstanceOf(CaseNotFoundError)
  })

  it('rejects other unsuccessful detail requests', async () => {
    fetchMock.mockResolvedValue(response(503))

    await expect(fetchCaseById(7)).rejects.toThrow(
      'Failed to fetch case 7: HTTP 503',
    )
  })

  it('posts a JSON case creation request', async () => {
    fetchMock.mockResolvedValue(response(201))

    await expect(postCase(createRequest)).resolves.toEqual(caseDetail)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/cases',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(createRequest),
      },
    )
  })

  it('translates create conflicts into CaseConflictError', async () => {
    fetchMock.mockResolvedValue(response(409))

    await expect(postCase(createRequest)).rejects.toBeInstanceOf(
      CaseConflictError,
    )
  })

  it('rejects other unsuccessful create requests', async () => {
    fetchMock.mockResolvedValue(response(400))

    await expect(postCase(createRequest)).rejects.toThrow(
      'Failed to create case: HTTP 400',
    )
  })

  it('puts a JSON case update request', async () => {
    fetchMock.mockResolvedValue(response(200))

    await putCase(7, updateRequest)

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/cases/7',
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updateRequest),
      },
    )
  })

  it('maps missing and conflicting updates and rejects other failures', async () => {
    fetchMock.mockResolvedValueOnce(response(404))
    await expect(putCase(7, updateRequest)).rejects.toBeInstanceOf(
      CaseNotFoundError,
    )

    fetchMock.mockResolvedValueOnce(response(409))
    await expect(putCase(7, updateRequest)).rejects.toBeInstanceOf(
      CaseConflictError,
    )

    fetchMock.mockResolvedValueOnce(response(500))
    await expect(putCase(7, updateRequest)).rejects.toThrow(
      'Failed to update case 7: HTTP 500',
    )
  })

  it.each(['archive', 'restore'] as const)(
    'posts the %s action with the current version',
    async (action) => {
      fetchMock.mockResolvedValue(response(200))

      await postCaseArchiveState(7, action, { version: 3 })

      expect(fetchMock).toHaveBeenCalledWith(
        `/api/cases/7/${action}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ version: 3 }),
        },
      )
    },
  )

  it.each([
    [404, CaseNotFoundError],
    [409, CaseConflictError],
  ] as const)('maps archive state HTTP %i responses', async (status, errorType) => {
    fetchMock.mockResolvedValue(response(status))

    await expect(
      postCaseArchiveState(7, 'archive', { version: 3 }),
    ).rejects.toBeInstanceOf(errorType)
  })

  it('rejects other unsuccessful archive state requests', async () => {
    fetchMock.mockResolvedValue(response(500))

    await expect(
      postCaseArchiveState(7, 'restore', { version: 3 }),
    ).rejects.toThrow('Failed to restore case 7: HTTP 500')
  })
})
