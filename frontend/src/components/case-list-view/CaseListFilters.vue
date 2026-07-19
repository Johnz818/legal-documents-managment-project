
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { CaseStage, MOCK_CASE_TAGS } from '@/data/case'
import { MOCK_USERS, UserRole } from '@/data/user'

// Filter state
const searchQuery = ref('')
const selectedStage = ref<string>('')
const selectedAttorney = ref<string>('')
const selectedTags = ref<string[]>([])

// Get attorneys for filter
const attorneys = computed(() => {
  return MOCK_USERS.filter(u => u.role === UserRole.LeadAttorney)
})

// Get all case stages
const caseStages = Object.values(CaseStage)

// Reset filters
const resetFilters = () => {
  searchQuery.value = ''
  selectedStage.value = ''
  selectedAttorney.value = ''
  selectedTags.value = []
}

// Check if any filter is active
const hasActiveFilters = computed(() => {
  return (
    searchQuery.value ||
    selectedStage.value ||
    selectedAttorney.value ||
    selectedTags.value.length > 0
  )
})

// Toggle tag selection
const toggleTag = (tagId: string) => {
  const index = selectedTags.value.indexOf(tagId)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tagId)
  }
}
</script>

<template>
  <div class="flex flex-col gap-3 rounded-lg border bg-card p-4">
    <!-- Search and basic filters -->
    <div class="flex flex-col gap-3 md:flex-row md:items-end md:gap-2">
      <!-- Search input -->
      <div class="flex-1">
        <label class="text-sm font-medium mb-1 block">搜索案号、法院或当事人</label>
        <Input
          v-model="searchQuery"
          placeholder="输入关键词..."
          class="h-9"
        />
      </div>

      <!-- Case stage filter -->
      <div class="w-full md:w-48">
        <label class="text-sm font-medium mb-1 block">案件阶段</label>
        <Select v-model="selectedStage">
          <SelectTrigger class="h-9">
            <SelectValue placeholder="选择阶段" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">全部阶段</SelectItem>
            <SelectItem v-for="stage in caseStages" :key="stage" :value="stage">
              {{ stage }}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      <!-- Attorney filter -->
      <div class="w-full md:w-48">
        <label class="text-sm font-medium mb-1 block">主办律师</label>
        <Select v-model="selectedAttorney">
          <SelectTrigger class="h-9">
            <SelectValue placeholder="选择律师" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">全部律师</SelectItem>
            <SelectItem v-for="attorney in attorneys" :key="attorney.id" :value="attorney.id">
              {{ attorney.name }}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      <!-- Tags filter popover -->
      <Popover>
        <PopoverTrigger as-child>
          <Button variant="outline" size="sm" class="h-9 w-full md:w-auto">
            <SafeIcon name="Filter" :size="16" class="mr-1" />
            标签
            <span v-if="selectedTags.length > 0" class="ml-1 text-xs font-semibold">
              ({{ selectedTags.length }})
            </span>
          </Button>
        </PopoverTrigger>
        <PopoverContent class="w-56" align="start">
          <div class="space-y-3">
            <h4 class="font-medium text-sm">选择标签</h4>
            <div class="space-y-2">
              <div v-for="tag in MOCK_CASE_TAGS" :key="tag.id" class="flex items-center gap-2">
                <Checkbox
                  :id="`tag-${tag.id}`"
                  :checked="selectedTags.includes(tag.id)"
                  @update:checked="toggleTag(tag.id)"
                />
                <Label :for="`tag-${tag.id}`" class="flex items-center gap-2 cursor-pointer flex-1">
                  <span :class="`h-2 w-2 rounded-full ${tag.color}`" />
                  {{ tag.name }}
                </Label>
              </div>
            </div>
          </div>
        </PopoverContent>
      </Popover>

      <!-- Reset button -->
      <Button
        v-if="hasActiveFilters"
        variant="ghost"
        size="sm"
        @click="resetFilters"
        class="h-9"
      >
        <SafeIcon name="X" :size="16" class="mr-1" />
        重置
      </Button>
    </div>

    <!-- Active filters display -->
    <div v-if="hasActiveFilters" class="flex flex-wrap items-center gap-2 text-sm">
      <span class="text-muted-foreground">活跃筛选：</span>
      <div v-if="searchQuery" class="inline-flex items-center gap-1 rounded-full bg-secondary px-2 py-1">
        <span>搜索: {{ searchQuery }}</span>
      </div>
      <div v-if="selectedStage" class="inline-flex items-center gap-1 rounded-full bg-secondary px-2 py-1">
        <span>{{ selectedStage }}</span>
      </div>
      <div v-if="selectedAttorney" class="inline-flex items-center gap-1 rounded-full bg-secondary px-2 py-1">
        <span>{{ attorneys.find(a => a.id === selectedAttorney)?.name }}</span>
      </div>
      <div
        v-for="tagId in selectedTags"
        :key="tagId"
        class="inline-flex items-center gap-1 rounded-full bg-secondary px-2 py-1"
      >
        <span>{{ MOCK_CASE_TAGS.find(t => t.id === tagId)?.name }}</span>
      </div>
    </div>
  </div>
</template>
