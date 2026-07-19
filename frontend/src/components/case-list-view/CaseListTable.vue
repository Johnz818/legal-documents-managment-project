
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { getCaseSummaryList, MOCK_CASE_TAGS } from '@/data/case'
import EmptyState from '@/components/common/EmptyState.vue'

// Get initial data
const cases = getCaseSummaryList()

// Selection state
const selectedIds = ref<Set<string>>(new Set())
const sortBy = ref<'caseNumber' | 'filingDate' | 'hearingDate'>('caseNumber')
const sortOrder = ref<'asc' | 'desc'>('asc')

// Computed properties
const allSelected = computed(() => {
  return cases.length > 0 && selectedIds.value.size === cases.length
})

const someSelected = computed(() => {
  return selectedIds.value.size > 0 && selectedIds.value.size < cases.length
})

const sortedCases = computed(() => {
  const sorted = [...cases].sort((a, b) => {
    let aVal: any = a[sortBy.value]
    let bVal: any = b[sortBy.value]

    if (aVal === null || aVal === undefined) aVal = ''
    if (bVal === null || bVal === undefined) bVal = ''

    if (typeof aVal === 'string') {
      aVal = aVal.toLowerCase()
      bVal = bVal.toLowerCase()
    }

    if (aVal < bVal) return sortOrder.value === 'asc' ? -1 : 1
    if (aVal > bVal) return sortOrder.value === 'asc' ? 1 : -1
    return 0
  })
  return sorted
})

// Toggle selection
const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedIds.value.clear()
  } else {
    selectedIds.value = new Set(cases.map(c => c.id))
  }
}

const toggleSelect = (caseId: string) => {
  if (selectedIds.value.has(caseId)) {
    selectedIds.value.delete(caseId)
  } else {
    selectedIds.value.add(caseId)
  }
}

// Sort handler
const handleSort = (column: 'caseNumber' | 'filingDate' | 'hearingDate') => {
  if (sortBy.value === column) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = column
    sortOrder.value = 'asc'
  }
}

// Get tag display
const getTagColor = (tagId: string) => {
  return MOCK_CASE_TAGS.find(t => t.id === tagId)?.color || 'bg-gray-500'
}

const getTagName = (tagId: string) => {
  return MOCK_CASE_TAGS.find(t => t.id === tagId)?.name || ''
}

// Format date
const formatDate = (dateStr: string | null) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}
</script>

<template>
  <div v-if="cases.length === 0" class="flex items-center justify-center h-96">
    <EmptyState variant="cases" />
  </div>

  <div v-else class="w-full overflow-x-auto">
    <Table>
      <TableHeader>
        <TableRow>
          <!-- Checkbox column -->
          <TableHead class="w-12">
            <Checkbox
              :checked="allSelected"
              :indeterminate="someSelected"
              @update:checked="toggleSelectAll"
            />
          </TableHead>

          <!-- Case number -->
          <TableHead class="cursor-pointer hover:bg-muted" @click="handleSort('caseNumber')">
            <div class="flex items-center gap-1">
              案号
              <SafeIcon
                v-if="sortBy === 'caseNumber'"
                :name="sortOrder === 'asc' ? 'ArrowUp' : 'ArrowDown'"
                :size="14"
              />
            </div>
          </TableHead>

          <!-- Court -->
          <TableHead>法院</TableHead>

          <!-- Parties -->
          <TableHead>当事人</TableHead>

          <!-- Case cause -->
          <TableHead>案由</TableHead>

          <!-- Stage -->
          <TableHead>案件阶段</TableHead>

          <!-- Lead attorney -->
          <TableHead>主办律师</TableHead>

          <!-- Filing date -->
          <TableHead class="cursor-pointer hover:bg-muted" @click="handleSort('filingDate')">
            <div class="flex items-center gap-1">
              立案时间
              <SafeIcon
                v-if="sortBy === 'filingDate'"
                :name="sortOrder === 'asc' ? 'ArrowUp' : 'ArrowDown'"
                :size="14"
              />
            </div>
          </TableHead>

          <!-- Hearing date -->
          <TableHead class="cursor-pointer hover:bg-muted" @click="handleSort('hearingDate')">
            <div class="flex items-center gap-1">
              开庭时间
              <SafeIcon
                v-if="sortBy === 'hearingDate'"
                :name="sortOrder === 'asc' ? 'ArrowUp' : 'ArrowDown'"
                :size="14"
              />
            </div>
          </TableHead>

          <!-- Tags -->
          <TableHead>标签</TableHead>

          <!-- Actions -->
          <TableHead class="w-12 text-right">操作</TableHead>
        </TableRow>
      </TableHeader>

      <TableBody>
        <TableRow v-for="caseItem in sortedCases" :key="caseItem.id" class="hover:bg-muted/50">
          <!-- Checkbox -->
          <TableCell>
            <Checkbox
              :checked="selectedIds.has(caseItem.id)"
              @update:checked="toggleSelect(caseItem.id)"
            />
          </TableCell>

          <!-- Case number (clickable) -->
          <TableCell class="font-medium">
            <a
              :href="`./case-detail-view.html?id=${caseItem.id}`"
              class="text-primary hover:underline"
            >
              {{ caseItem.caseNumber }}
            </a>
          </TableCell>

          <!-- Court -->
          <TableCell class="text-sm">{{ caseItem.courtName }}</TableCell>

          <!-- Parties -->
          <TableCell class="text-sm">
            <div class="max-w-xs truncate">
              {{ caseItem.plaintiff }} vs {{ caseItem.defendant }}
            </div>
          </TableCell>

          <!-- Case cause -->
          <TableCell class="text-sm">{{ caseItem.caseCause }}</TableCell>

          <!-- Stage -->
          <TableCell>
            <Badge variant="outline" class="text-xs">
              {{ caseItem.caseStage }}
            </Badge>
          </TableCell>

          <!-- Lead attorney -->
          <TableCell class="text-sm">{{ caseItem.leadAttorneyName }}</TableCell>

          <!-- Filing date -->
          <TableCell class="text-sm">{{ formatDate(caseItem.filingDate) }}</TableCell>

          <!-- Hearing date -->
          <TableCell class="text-sm">{{ formatDate(caseItem.hearingDate) }}</TableCell>

          <!-- Tags -->
          <TableCell>
            <div class="flex flex-wrap gap-1">
              <Badge
                v-for="tag in caseItem.tags"
                :key="tag.id"
                :class="`${tag.color} text-white text-xs`"
              >
                {{ tag.name }}
              </Badge>
            </div>
          </TableCell>

          <!-- Actions -->
          <TableCell class="text-right">
            <DropdownMenu>
              <DropdownMenuTrigger as-child>
                <Button variant="ghost" size="sm" class="h-8 w-8 p-0">
                  <SafeIcon name="MoreHorizontal" :size="16" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem as="a" :href="`./case-detail-view.html?id=${caseItem.id}`">
                  <SafeIcon name="Eye" :size="14" class="mr-2" />
                  查看详情
                </DropdownMenuItem>
                <DropdownMenuItem as="a" :href="`./document-generate-selection.html?caseId=${caseItem.id}`">
                  <SafeIcon name="FileText" :size="14" class="mr-2" />
                  生成文书
                </DropdownMenuItem>
                <DropdownMenuItem as="a" :href="`./case-detail-view-reminder-settings.html?caseId=${caseItem.id}`">
                  <SafeIcon name="Bell" :size="14" class="mr-2" />
                  设置提醒
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>
  </div>
</template>
