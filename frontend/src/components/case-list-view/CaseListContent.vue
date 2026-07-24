<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CaseListFilters from '@/components/case-list-view/CaseListFilters.vue'
import CaseListTable from '@/components/case-list-view/CaseListTable.vue'
import { getCases } from '@/services/caseService'
import type { CaseSearchCriteria, CaseSummaryResponse } from '@/types/case'

const cases = ref<CaseSummaryResponse[]>([])
const appliedCriteria = ref<CaseSearchCriteria>({})
const isLoading = ref(true)
const errorMessage = ref('')

const isFiltered = computed(() => Object.keys(appliedCriteria.value).length > 0)

const loadCases = async (criteria: CaseSearchCriteria = appliedCriteria.value) => {
  appliedCriteria.value = criteria
  isLoading.value = true
  errorMessage.value = ''
  cases.value = []

  try {
    cases.value = await getCases(criteria)
  } catch {
    errorMessage.value = '案件数据加载失败，请确认后端服务可用后重试。'
  } finally {
    isLoading.value = false
  }
}

const handleSearch = (criteria: CaseSearchCriteria) => {
  void loadCases(criteria)
}

const handleReset = () => {
  void loadCases({})
}

const handleRetry = () => {
  void loadCases()
}

onMounted(() => {
  void loadCases({})
})
</script>

<template>
  <div class="flex min-h-0 flex-1 flex-col gap-4">
    <CaseListFilters
      :is-loading="isLoading"
      @search="handleSearch"
      @reset="handleReset"
    />

    <div class="flex-1 overflow-auto rounded-lg border bg-card">
      <CaseListTable
        :cases="cases"
        :is-loading="isLoading"
        :error-message="errorMessage"
        :is-filtered="isFiltered"
        @retry="handleRetry"
      />
    </div>
  </div>
</template>
