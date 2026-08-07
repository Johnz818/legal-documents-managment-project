import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DocumentGenerateSelection from './DocumentGenerateSelection.vue'
import { DocumentGenerationApiError } from '@/api/documentGenerationApi'
import { getCases } from '@/services/caseService'
import {
  downloadGeneratedDocument,
  getDocumentTemplateVersion,
  getDocumentTemplateVersions,
  getDocumentTemplates,
  prepareDocumentGeneration,
  submitGenerationAttempt,
} from '@/services/documentGenerationService'
import type { CaseSummaryResponse } from '@/types/case'
import type {
  GeneratedDocument,
  GenerationPreparation,
  PublishedTemplateVersion,
} from '@/types/documentGeneration'

vi.mock('@/services/caseService', () => ({ getCases: vi.fn() }))
vi.mock('@/services/documentGenerationService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/documentGenerationService')>()
  return {
    ...original,
    getDocumentTemplates: vi.fn(),
    getDocumentTemplateVersions: vi.fn(),
    getDocumentTemplateVersion: vi.fn(),
    prepareDocumentGeneration: vi.fn(),
    submitGenerationAttempt: vi.fn(),
    downloadGeneratedDocument: vi.fn(),
  }
})

const legalCase = {
  id: 7,
  caseNumber: '(2026)沪01号',
  caseName: '测试案件',
  status: '审理中',
  plaintiff: '张三',
  leadLawyerName: '李律师',
} as CaseSummaryResponse

const template = {
  id: 11,
  name: '律师事务所函',
  description: '测试模板',
  templateType: 'CUSTOM' as const,
  createdAt: '2026-08-01T10:00:00',
  updatedAt: '2026-08-01T10:00:00',
}

const versionSummary = {
  versionNumber: 2,
  originalFileName: 'letter.docx',
  contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  fileSize: 100,
  contentSha256: 'a'.repeat(64),
  publishedAt: '2026-08-01T10:00:00',
}

const exactVersion = {
  templateId: 11,
  templateName: '律师事务所函',
  templateDescription: '版本描述',
  versionNumber: 2,
  originalFileName: 'letter.docx',
  contentType: versionSummary.contentType,
  fileSize: 100,
  contentSha256: 'a'.repeat(64),
  publishedAt: versionSummary.publishedAt,
  fields: [],
} as PublishedTemplateVersion

const preparation: GenerationPreparation = {
  caseId: 7,
  templateId: 11,
  versionNumber: 2,
  timezone: 'Asia/Shanghai',
  fields: [{
    fieldKey: 'case_number',
    displayName: '案号',
    description: null,
    valueType: 'TEXT',
    required: true,
    defaultSource: 'CASE_FIELD',
    sourceKey: 'caseNumber',
    displayOrder: 0,
    suggestedValue: '(2026)沪01号',
    status: 'RESOLVED',
  }],
}

const stalePreparation: GenerationPreparation = {
  ...preparation,
  fields: [
    preparation.fields[0],
    {
      ...preparation.fields[0],
      fieldKey: 'current_date',
      displayName: '当前日期',
      defaultSource: 'SYSTEM_VALUE',
      sourceKey: 'currentDate',
      displayOrder: 1,
      suggestedValue: '2026年8月6日',
    },
    {
      ...preparation.fields[0],
      fieldKey: 'manual_note',
      displayName: '备注',
      required: false,
      defaultSource: 'USER_INPUT',
      sourceKey: null,
      displayOrder: 2,
      suggestedValue: null,
      status: 'REQUIRES_USER_INPUT',
    },
  ],
}

const refreshedPreparation: GenerationPreparation = {
  ...stalePreparation,
  fields: stalePreparation.fields.map(item => {
    if (item.fieldKey === 'case_number') return { ...item, suggestedValue: '(2026)沪02号' }
    if (item.fieldKey === 'current_date') return { ...item, suggestedValue: '2026年8月7日' }
    return item
  }),
}

const generated: GeneratedDocument = {
  generationId: 23,
  caseId: 7,
  templateId: 11,
  versionNumber: 2,
  caseDocumentId: 31,
  outputAvailable: true,
  fileName: '律师事务所函.docx',
  createdAt: '2026-08-06T10:00:00',
}

const stubs = {
  CaseSelector: {
    props: ['cases', 'selectedCaseId', 'isLoading', 'errorMessage', 'disabled'],
    emits: ['select', 'retry'],
    template: `<div>
      <button class="select-case" @click="$emit('select', 7)">case</button>
      <button class="select-other-case" @click="$emit('select', 8)">other case</button>
    </div>`,
  },
  TemplateSelector: {
    props: [
      'templates', 'versions', 'selectedTemplateId', 'selectedVersionNumber',
      'exactVersion', 'isLoadingTemplates', 'isLoadingVersions',
      'isLoadingExactVersion', 'templateError', 'versionError',
      'hasMoreTemplates', 'hasMoreVersions', 'disabled',
    ],
    emits: [
      'selectTemplate', 'selectVersion', 'loadMoreTemplates', 'loadMoreVersions',
      'retryTemplates', 'retryVersions',
    ],
    template: `<div>
      <span class="template-count">{{ templates.length }}</span>
      <span class="template-error">{{ templateError }}</span>
      <span class="exact-version">{{ exactVersion?.versionNumber }}</span>
      <button class="select-template" @click="$emit('selectTemplate', 11)">template</button>
      <button class="select-version" @click="$emit('selectVersion', 2)">version</button>
      <button class="load-more-templates" @click="$emit('loadMoreTemplates')">more</button>
    </div>`,
  },
  GenerationValueReview: {
    props: ['fields', 'values', 'fieldErrors', 'formError', 'disabled', 'conflicts'],
    emits: ['updateValue', 'resolveConflict'],
    template: `<div>
      <span class="review-value">{{ values.case_number }}</span>
      <span class="manual-value">{{ values.manual_note }}</span>
      <span class="review-locked">{{ disabled }}</span>
      <span class="field-error">{{ fieldErrors.case_number }}</span>
      <span class="conflict-count">{{ conflicts.length }}</span>
      <button class="edit-field" @click="$emit('updateValue', 'case_number', '(2026)沪02号')">edit</button>
      <button class="edit-manual" @click="$emit('updateValue', 'manual_note', '保留人工备注')">manual</button>
      <button class="use-current-case" @click="$emit('resolveConflict', 'case_number', 'USE_CURRENT')">current case</button>
      <button class="keep-current-date" @click="$emit('resolveConflict', 'current_date', 'KEEP_PREVIOUS')">old date</button>
    </div>`,
  },
  GenerationResult: {
    props: ['result', 'isDownloading', 'downloadError'],
    emits: ['download', 'generateAnother'],
    template: `<div>
      <button class="download" @click="$emit('download')">{{ result.fileName }}</button>
      <button class="generate-another" @click="$emit('generateAnother')">again</button>
    </div>`,
  },
}

const mountPage = () => mount(DocumentGenerateSelection, {
  global: { stubs },
})

const selectAndPrepare = async (wrapper: ReturnType<typeof mountPage>) => {
  await wrapper.get('.select-case').trigger('click')
  await wrapper.get('.select-template').trigger('click')
  await flushPromises()
  await wrapper.get('.select-version').trigger('click')
  await flushPromises()
  const prepareButton = wrapper.findAll('button').find(button => button.text().includes('准备字段值'))
  await prepareButton!.trigger('click')
  await flushPromises()
}

describe('DocumentGenerateSelection', () => {
  beforeEach(() => {
    vi.mocked(getCases).mockResolvedValue([legalCase])
    vi.mocked(getDocumentTemplates).mockResolvedValue({
      items: [template], page: 0, size: 20, totalElements: 1, totalPages: 1,
    })
    vi.mocked(getDocumentTemplateVersions).mockResolvedValue({
      items: [versionSummary], page: 0, size: 20, totalElements: 1, totalPages: 1,
    })
    vi.mocked(getDocumentTemplateVersion).mockResolvedValue(exactVersion)
    vi.mocked(prepareDocumentGeneration).mockResolvedValue(preparation)
    vi.mocked(submitGenerationAttempt).mockResolvedValue(generated)
    vi.mocked(downloadGeneratedDocument).mockResolvedValue(new Blob(['docx']))
    vi.spyOn(Intl, 'DateTimeFormat').mockImplementation(() => ({
      resolvedOptions: () => ({ timeZone: 'Asia/Shanghai' }),
    }) as Intl.DateTimeFormat)
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => '00000000-0000-4000-8000-000000000001') })
    window.history.replaceState({}, '', '/document-generate-selection.html')
  })

  it('prepares exact selection and submits reviewed values once', async () => {
    const wrapper = mountPage()
    await flushPromises()
    await selectAndPrepare(wrapper)

    expect(prepareDocumentGeneration).toHaveBeenCalledWith({
      caseId: 7, templateId: 11, versionNumber: 2, timezone: 'Asia/Shanghai',
    })
    expect(wrapper.get('.review-value').text()).toBe('(2026)沪01号')

    const generateButton = wrapper.findAll('button').find(button => button.text().includes('确认字段并生成'))
    await generateButton!.trigger('click')
    await generateButton!.trigger('click')
    await flushPromises()

    expect(submitGenerationAttempt).toHaveBeenCalledTimes(1)
    const submitted = vi.mocked(submitGenerationAttempt).mock.calls[0][0]
    expect(submitted).toMatchObject({
      idempotencyKey: '00000000-0000-4000-8000-000000000001',
      request: { values: [{ value: '(2026)沪01号', valueSource: 'CASE_FIELD' }] },
    })
    expect(wrapper.text()).toContain('律师事务所函.docx')
  })

  it('preselects an exact version from query parameters without page-walking templates', async () => {
    window.history.replaceState({}, '', '/document-generate-selection.html?templateId=11&versionNumber=2')
    vi.mocked(getDocumentTemplates).mockResolvedValue({
      items: [], page: 0, size: 20, totalElements: 25, totalPages: 2,
    })
    const wrapper = mountPage()
    await flushPromises()

    expect(getDocumentTemplateVersion).toHaveBeenCalledWith(11, 2)
    expect(getDocumentTemplateVersions).toHaveBeenCalledWith(11, 0, 20)
    expect(wrapper.get('.exact-version').text()).toBe('2')
  })

  it('locks an uncertain attempt and retries the exact snapshot', async () => {
    vi.mocked(submitGenerationAttempt)
      .mockRejectedValueOnce(new TypeError('network interrupted'))
      .mockResolvedValueOnce(generated)
    const wrapper = mountPage()
    await flushPromises()
    await selectAndPrepare(wrapper)

    await wrapper.get('.edit-field').trigger('click')
    const generateButton = wrapper.findAll('button').find(button => button.text().includes('确认字段并生成'))
    await generateButton!.trigger('click')
    await flushPromises()

    expect(wrapper.get('.review-locked').text()).toBe('true')
    const firstAttempt = vi.mocked(submitGenerationAttempt).mock.calls[0][0]
    const retryButton = wrapper.findAll('button').find(button => button.text().includes('重试同一请求'))
    await retryButton!.trigger('click')
    await flushPromises()

    expect(vi.mocked(submitGenerationAttempt).mock.calls[1][0]).toBe(firstAttempt)
    expect(firstAttempt.request.values[0].value).toBe('(2026)沪02号')
  })

  it('discards an uncertain attempt before editing and creates a new operation', async () => {
    vi.mocked(submitGenerationAttempt)
      .mockRejectedValueOnce(new TypeError('network interrupted'))
      .mockResolvedValueOnce(generated)
    const randomUUID = vi.fn()
      .mockReturnValueOnce('00000000-0000-4000-8000-000000000001')
      .mockReturnValueOnce('00000000-0000-4000-8000-000000000002')
    vi.stubGlobal('crypto', { randomUUID })
    const wrapper = mountPage()
    await flushPromises()
    await selectAndPrepare(wrapper)

    let generateButton = wrapper.findAll('button').find(button => button.text().includes('确认字段并生成'))
    await generateButton!.trigger('click')
    await flushPromises()
    const firstAttempt = vi.mocked(submitGenerationAttempt).mock.calls[0][0]

    const editButton = wrapper.findAll('button').find(button => button.text().includes('返回编辑字段'))
    await editButton!.trigger('click')
    expect(wrapper.get('.review-locked').text()).toBe('false')
    await wrapper.get('.edit-field').trigger('click')
    generateButton = wrapper.findAll('button').find(button => button.text().includes('确认字段并生成'))
    await generateButton!.trigger('click')
    await flushPromises()

    const secondAttempt = vi.mocked(submitGenerationAttempt).mock.calls[1][0]
    expect(firstAttempt.idempotencyKey).toBe('00000000-0000-4000-8000-000000000001')
    expect(secondAttempt.idempotencyKey).toBe('00000000-0000-4000-8000-000000000002')
    expect(secondAttempt).not.toBe(firstAttempt)
  })

  it('preserves loaded templates when a later page fails', async () => {
    vi.mocked(getDocumentTemplates)
      .mockResolvedValueOnce({
        items: [template], page: 0, size: 20, totalElements: 21, totalPages: 2,
      })
      .mockRejectedValueOnce(new Error('page failed'))
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.get('.template-count').text()).toBe('1')
    await wrapper.get('.load-more-templates').trigger('click')
    await flushPromises()

    expect(wrapper.get('.template-count').text()).toBe('1')
    expect(wrapper.get('.template-error').text()).toContain('模板加载失败')
  })

  it('ignores a stale preparation response after the Case changes', async () => {
    let resolvePreparation!: (value: GenerationPreparation) => void
    vi.mocked(prepareDocumentGeneration).mockReturnValue(new Promise(resolve => {
      resolvePreparation = resolve
    }))
    const wrapper = mountPage()
    await flushPromises()
    await wrapper.get('.select-case').trigger('click')
    await wrapper.get('.select-template').trigger('click')
    await flushPromises()
    await wrapper.get('.select-version').trigger('click')
    await flushPromises()
    const prepareButton = wrapper.findAll('button').find(button => button.text().includes('准备字段值'))
    await prepareButton!.trigger('click')
    await wrapper.get('.select-other-case').trigger('click')

    resolvePreparation(preparation)
    await flushPromises()

    expect(wrapper.find('.review-value').exists()).toBe(false)
  })

  it('downloads the persisted generated Case document', async () => {
    const createObjectURL = vi.fn(() => 'blob:test')
    const revokeObjectURL = vi.fn()
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL })
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const wrapper = mountPage()
    await flushPromises()
    await selectAndPrepare(wrapper)
    const generateButton = wrapper.findAll('button').find(button => button.text().includes('确认字段并生成'))
    await generateButton!.trigger('click')
    await flushPromises()
    await wrapper.get('.download').trigger('click')
    await flushPromises()

    expect(downloadGeneratedDocument).toHaveBeenCalledWith(generated)
    expect(click).toHaveBeenCalled()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:test')
  })

  it('refreshes stale suggestions, preserves human input, and requires each choice', async () => {
    vi.mocked(prepareDocumentGeneration)
      .mockResolvedValueOnce(stalePreparation)
      .mockResolvedValueOnce(refreshedPreparation)
    vi.mocked(submitGenerationAttempt)
      .mockRejectedValueOnce(new DocumentGenerationApiError('generate', 409, {
        title: 'Generation value is stale',
        detail: 'The case number changed after review.',
        code: 'GENERATION_VALUE_STALE',
        details: { fieldKey: 'case_number' },
      }))
      .mockResolvedValueOnce(generated)
    const wrapper = mountPage()
    await flushPromises()
    await selectAndPrepare(wrapper)
    await wrapper.get('.edit-manual').trigger('click')

    let generateButton = wrapper.findAll('button').find(button => button.text().includes('确认字段并生成'))
    await generateButton!.trigger('click')
    await flushPromises()

    expect(prepareDocumentGeneration).toHaveBeenCalledTimes(2)
    expect(wrapper.get('.manual-value').text()).toBe('保留人工备注')
    expect(wrapper.get('.conflict-count').text()).toBe('2')
    expect(generateButton!.attributes('disabled')).toBeDefined()

    await wrapper.get('.use-current-case').trigger('click')
    expect(generateButton!.attributes('disabled')).toBeDefined()
    await wrapper.get('.keep-current-date').trigger('click')
    expect(generateButton!.attributes('disabled')).toBeUndefined()

    generateButton = wrapper.findAll('button').find(button => button.text().includes('确认字段并生成'))
    await generateButton!.trigger('click')
    await flushPromises()

    const retryRequest = vi.mocked(submitGenerationAttempt).mock.calls[1][0].request.values
    expect(retryRequest).toEqual([
      expect.objectContaining({
        fieldKey: 'case_number', value: '(2026)沪02号', valueSource: 'CASE_FIELD',
      }),
      expect.objectContaining({
        fieldKey: 'current_date', value: '2026年8月6日', valueSource: 'USER_INPUT',
      }),
      expect.objectContaining({
        fieldKey: 'manual_note', value: '保留人工备注', valueSource: 'USER_INPUT',
      }),
    ])

    await wrapper.get('.generate-another').trigger('click')
    expect(wrapper.get('.conflict-count').text()).toBe('0')
  })

  it('surfaces controlled backend details and highlights the affected display field', async () => {
    vi.mocked(submitGenerationAttempt).mockRejectedValueOnce(
      new DocumentGenerationApiError('generate', 400, {
        title: 'Invalid generation value',
        detail: 'The case number is not valid for this template.',
        code: 'GENERATION_REQUEST_INVALID',
        details: { fieldKey: 'case_number' },
      }),
    )
    const wrapper = mountPage()
    await flushPromises()
    await selectAndPrepare(wrapper)

    const generateButton = wrapper.findAll('button').find(button => button.text().includes('确认字段并生成'))
    await generateButton!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('The case number is not valid for this template.')
    expect(wrapper.text()).toContain('受影响字段：案号（case_number）')
    expect(wrapper.get('.field-error').text()).toBe(
      'The case number is not valid for this template.',
    )
  })
})
