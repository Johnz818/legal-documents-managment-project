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
import {
  CaseUpdateNotFoundError,
  DuplicateCaseNumberError,
  StaleCaseVersionError,
  changeCaseArchiveState,
  createCase,
  getCaseById,
  getCases,
  updateCase,
} from '@/services/caseService'
import type {
  CaseCreateRequest,
  CaseDetailResponse,
  CaseSummaryResponse,
  CaseUpdateRequest,
} from '@/types/case'

vi.mock('@/api/caseApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/api/caseApi')>()
  return {
    ...original,
    fetchCases: vi.fn(),
    fetchCaseById: vi.fn(),
    postCase: vi.fn(),
    putCase: vi.fn(),
    postCaseArchiveState: vi.fn(),
  }
})

const summary = { id: 7 } as CaseSummaryResponse
const detail = { id: 7, version: 4 } as CaseDetailResponse
const createRequest = {} as CaseCreateRequest
const updateRequest = { version: 3 } as CaseUpdateRequest

describe('caseService', () => {
  beforeEach(() => {
    vi.mocked(fetchCases).mockReset()
    vi.mocked(fetchCaseById).mockReset()
    vi.mocked(postCase).mockReset()
    vi.mocked(putCase).mockReset()
    vi.mocked(postCaseArchiveState).mockReset()
  })

  it('returns the list response data', async () => {
    vi.mocked(fetchCases).mockResolvedValue({ data: [summary] })

    await expect(getCases({ status: 'IN_TRIAL' })).resolves.toEqual([summary])
    expect(fetchCases).toHaveBeenCalledWith({ status: 'IN_TRIAL' })
  })

  it('returns null when case detail is missing', async () => {
    vi.mocked(fetchCaseById).mockRejectedValue(new CaseNotFoundError(7))

    await expect(getCaseById(7)).resolves.toBeNull()
  })

  it('returns case detail when it exists', async () => {
    vi.mocked(fetchCaseById).mockResolvedValue(detail)

    await expect(getCaseById(7)).resolves.toBe(detail)
  })

  it('preserves unexpected detail errors', async () => {
    const error = new Error('network unavailable')
    vi.mocked(fetchCaseById).mockRejectedValue(error)

    await expect(getCaseById(7)).rejects.toBe(error)
  })

  it('translates create conflicts into DuplicateCaseNumberError', async () => {
    vi.mocked(postCase).mockRejectedValue(new CaseConflictError())

    await expect(createCase(createRequest)).rejects.toBeInstanceOf(
      DuplicateCaseNumberError,
    )
  })

  it('returns a successfully created case', async () => {
    vi.mocked(postCase).mockResolvedValue(detail)

    await expect(createCase(createRequest)).resolves.toBe(detail)
  })

  it('preserves unexpected create errors', async () => {
    const error = new Error('network unavailable')
    vi.mocked(postCase).mockRejectedValue(error)

    await expect(createCase(createRequest)).rejects.toBe(error)
  })

  it('returns a successfully updated case', async () => {
    vi.mocked(putCase).mockResolvedValue(detail)

    await expect(updateCase(7, updateRequest)).resolves.toBe(detail)
  })

  it('translates a missing update target', async () => {
    vi.mocked(putCase).mockRejectedValue(new CaseNotFoundError(7))

    await expect(updateCase(7, updateRequest)).rejects.toBeInstanceOf(
      CaseUpdateNotFoundError,
    )
  })

  it('identifies a stale update from the latest version', async () => {
    vi.mocked(putCase).mockRejectedValue(new CaseConflictError())
    vi.mocked(fetchCaseById).mockResolvedValue(detail)

    await expect(updateCase(7, updateRequest)).rejects.toBeInstanceOf(
      StaleCaseVersionError,
    )
  })

  it('identifies a duplicate number when the version remains current', async () => {
    vi.mocked(putCase).mockRejectedValue(new CaseConflictError())
    vi.mocked(fetchCaseById).mockResolvedValue({
      ...detail,
      version: updateRequest.version,
    })

    await expect(updateCase(7, updateRequest)).rejects.toBeInstanceOf(
      DuplicateCaseNumberError,
    )
  })

  it('preserves unexpected update errors', async () => {
    const error = new Error('network unavailable')
    vi.mocked(putCase).mockRejectedValue(error)

    await expect(updateCase(7, updateRequest)).rejects.toBe(error)
  })

  it.each([
    [true, 'archive'],
    [false, 'restore'],
  ] as const)('selects the correct archive action', async (archived, action) => {
    vi.mocked(postCaseArchiveState).mockResolvedValue(detail)

    await expect(changeCaseArchiveState(7, 3, archived)).resolves.toBe(detail)
    expect(postCaseArchiveState).toHaveBeenCalledWith(
      7,
      action,
      { version: 3 },
    )
  })

  it('translates archive conflicts into StaleCaseVersionError', async () => {
    vi.mocked(postCaseArchiveState).mockRejectedValue(new CaseConflictError())

    await expect(changeCaseArchiveState(7, 3, true)).rejects.toBeInstanceOf(
      StaleCaseVersionError,
    )
  })

  it('translates a missing archive target', async () => {
    vi.mocked(postCaseArchiveState).mockRejectedValue(new CaseNotFoundError(7))

    await expect(
      changeCaseArchiveState(7, 3, true),
    ).rejects.toBeInstanceOf(CaseUpdateNotFoundError)
  })

  it('preserves unexpected archive errors', async () => {
    const error = new Error('network unavailable')
    vi.mocked(postCaseArchiveState).mockRejectedValue(error)

    await expect(changeCaseArchiveState(7, 3, true)).rejects.toBe(error)
  })
})
