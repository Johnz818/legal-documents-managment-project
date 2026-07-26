
<script setup lang="ts">
import { computed, ref } from 'vue'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { CASE_STATUS_OPTIONS } from '@/constants/caseStatus'
import type { CaseArchiveState, CaseSearchCriteria, CaseStatusCode } from '@/types/case'

interface Props {
  isLoading: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  search: [criteria: CaseSearchCriteria]
  reset: []
}>()

const caseNumberPrefix = ref('')
const caseNamePrefix = ref('')
const selectedStatus = ref<CaseStatusCode | 'ALL'>('ALL')
const leadLawyerName = ref('')
const archiveState = ref<CaseArchiveState>('ACTIVE')

const hasActiveFilters = computed(() => {
  return Boolean(
    caseNumberPrefix.value
      || caseNamePrefix.value
      || selectedStatus.value !== 'ALL'
      || leadLawyerName.value
      || archiveState.value === 'ARCHIVED',
  )
})

const buildCriteria = (): CaseSearchCriteria => {
  const criteria: CaseSearchCriteria = {}
  const normalizedCaseNumber = caseNumberPrefix.value.trim()
  const normalizedCaseName = caseNamePrefix.value.trim()
  const normalizedLeadLawyerName = leadLawyerName.value.trim()

  if (normalizedCaseNumber) {
    criteria.caseNumberPrefix = normalizedCaseNumber
  }
  if (normalizedCaseName) {
    criteria.caseNamePrefix = normalizedCaseName
  }
  if (selectedStatus.value !== 'ALL') {
    criteria.status = selectedStatus.value
  }
  if (normalizedLeadLawyerName) {
    criteria.leadLawyerName = normalizedLeadLawyerName
  }
  if (archiveState.value === 'ARCHIVED') {
    criteria.archiveState = 'ARCHIVED'
  }

  return criteria
}

const submitSearch = () => {
  emit('search', buildCriteria())
}

const resetFilters = () => {
  caseNumberPrefix.value = ''
  caseNamePrefix.value = ''
  selectedStatus.value = 'ALL'
  leadLawyerName.value = ''
  archiveState.value = 'ACTIVE'
  emit('reset')
}
</script>

<template>
  <form
    class="flex flex-col gap-3 rounded-lg border bg-card p-4"
    @submit.prevent="submitSearch"
  >
    <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-5">
      <div>
        <label class="mb-1 block text-sm font-medium">案号前缀</label>
        <Input
          v-model="caseNumberPrefix"
          placeholder="例如：(2016)浙01"
          class="h-9"
        />
      </div>

      <div>
        <label class="mb-1 block text-sm font-medium">案件名称前缀</label>
        <Input
          v-model="caseNamePrefix"
          placeholder="输入案件名称开头"
          class="h-9"
        />
      </div>

      <div>
        <label class="mb-1 block text-sm font-medium">案件阶段</label>
        <Select v-model="selectedStatus">
          <SelectTrigger class="h-9">
            <SelectValue placeholder="选择阶段" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">全部阶段</SelectItem>
            <SelectItem
              v-for="option in CASE_STATUS_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div>
        <label class="mb-1 block text-sm font-medium">主办律师</label>
        <Input
          v-model="leadLawyerName"
          placeholder="输入完整姓名"
          class="h-9"
        />
      </div>

      <div>
        <label class="mb-1 block text-sm font-medium">归档状态</label>
        <Select v-model="archiveState">
          <SelectTrigger class="h-9">
            <SelectValue placeholder="选择归档状态" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ACTIVE">在办案件</SelectItem>
            <SelectItem value="ARCHIVED">已归档案件</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>

    <div class="flex justify-end gap-2">
      <Button
        v-if="hasActiveFilters"
        variant="ghost"
        size="sm"
        type="button"
        :disabled="isLoading"
        class="h-9"
        @click="resetFilters"
      >
        <SafeIcon name="X" :size="16" class="mr-1" />
        重置
      </Button>
      <Button
        type="submit"
        size="sm"
        class="h-9"
        :disabled="isLoading"
      >
        <SafeIcon name="Search" :size="16" class="mr-1" />
        查询
      </Button>
    </div>
  </form>
</template>
