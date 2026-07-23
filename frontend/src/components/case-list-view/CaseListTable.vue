<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
import EmptyState from '@/components/common/EmptyState.vue'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { getCases } from '@/services/caseService'
import type { CaseSummaryResponse } from '@/types/case'

const cases = ref<CaseSummaryResponse[]>([])
const isLoading = ref(true)
const errorMessage = ref('')
const selectedIds = ref<Set<number>>(new Set())
const sortBy = ref<'caseNumber' | 'createdAt' | 'updatedAt'>('createdAt')
const sortOrder = ref<'asc' | 'desc'>('desc')

const allSelected = computed(() => {
  return cases.value.length > 0 && selectedIds.value.size === cases.value.length
})

const someSelected = computed(() => {
  return selectedIds.value.size > 0 && selectedIds.value.size < cases.value.length
})

const sortedCases = computed(() => {
  return [...cases.value].sort((firstCase, secondCase) => {
    const firstValue = firstCase[sortBy.value].toLowerCase()
    const secondValue = secondCase[sortBy.value].toLowerCase()

    if (firstValue < secondValue) return sortOrder.value === 'asc' ? -1 : 1
    if (firstValue > secondValue) return sortOrder.value === 'asc' ? 1 : -1
    return 0
  })
})

const loadCases = async () => {
  isLoading.value = true
  errorMessage.value = ''
  selectedIds.value = new Set()

  try {
    cases.value = await getCases()
  } catch {
    cases.value = []
    errorMessage.value = '案件数据加载失败，请确认后端服务可用后重试。'
  } finally {
    isLoading.value = false
  }
}

const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(cases.value.map(caseItem => caseItem.id))
  }
}

const toggleSelect = (caseId: number) => {
  const nextSelectedIds = new Set(selectedIds.value)

  if (nextSelectedIds.has(caseId)) {
    nextSelectedIds.delete(caseId)
  } else {
    nextSelectedIds.add(caseId)
  }

  selectedIds.value = nextSelectedIds
}

const handleSort = (column: 'caseNumber' | 'createdAt' | 'updatedAt') => {
  if (sortBy.value === column) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = column
    sortOrder.value = column === 'caseNumber' ? 'asc' : 'desc'
  }
}

const formatDateTime = (dateTime: string) => {
  return new Date(dateTime).toLocaleString('zh-CN')
}

onMounted(loadCases)
</script>

<template>
  <div v-if="isLoading" class="flex h-96 items-center justify-center">
    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <SafeIcon name="LoaderCircle" :size="18" class="animate-spin" />
      正在加载案件...
    </div>
  </div>

  <div v-else-if="errorMessage" class="flex h-96 items-center justify-center">
    <div class="flex max-w-md flex-col items-center gap-3 text-center">
      <SafeIcon name="CircleAlert" :size="28" class="text-destructive" />
      <p class="text-sm text-muted-foreground">{{ errorMessage }}</p>
      <Button variant="outline" size="sm" @click="loadCases">
        重新加载
      </Button>
    </div>
  </div>

  <div v-else-if="cases.length === 0" class="flex h-96 items-center justify-center">
    <EmptyState variant="cases" />
  </div>

  <div v-else class="w-full overflow-x-auto">
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead class="w-12">
            <Checkbox
              :checked="allSelected"
              :indeterminate="someSelected"
              @update:checked="toggleSelectAll"
            />
          </TableHead>

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

          <TableHead>案件名称</TableHead>
          <TableHead>案件状态</TableHead>
          <TableHead>法院</TableHead>
          <TableHead>主办律师</TableHead>

          <TableHead class="cursor-pointer hover:bg-muted" @click="handleSort('createdAt')">
            <div class="flex items-center gap-1">
              创建时间
              <SafeIcon
                v-if="sortBy === 'createdAt'"
                :name="sortOrder === 'asc' ? 'ArrowUp' : 'ArrowDown'"
                :size="14"
              />
            </div>
          </TableHead>

          <TableHead class="cursor-pointer hover:bg-muted" @click="handleSort('updatedAt')">
            <div class="flex items-center gap-1">
              更新时间
              <SafeIcon
                v-if="sortBy === 'updatedAt'"
                :name="sortOrder === 'asc' ? 'ArrowUp' : 'ArrowDown'"
                :size="14"
              />
            </div>
          </TableHead>

          <TableHead>归档状态</TableHead>
          <TableHead class="w-12 text-right">操作</TableHead>
        </TableRow>
      </TableHeader>

      <TableBody>
        <TableRow v-for="caseItem in sortedCases" :key="caseItem.id" class="hover:bg-muted/50">
          <TableCell>
            <Checkbox
              :checked="selectedIds.has(caseItem.id)"
              @update:checked="toggleSelect(caseItem.id)"
            />
          </TableCell>

          <TableCell class="font-medium">
            <a
              :href="`./case-detail-view.html?id=${caseItem.id}`"
              class="text-primary hover:underline"
            >
              {{ caseItem.caseNumber }}
            </a>
          </TableCell>

          <TableCell class="text-sm">{{ caseItem.caseName }}</TableCell>
          <TableCell>
            <Badge variant="outline" class="text-xs">
              {{ caseItem.status }}
            </Badge>
          </TableCell>
          <TableCell class="text-sm">{{ caseItem.courtName || '-' }}</TableCell>
          <TableCell class="text-sm">{{ caseItem.leadLawyerName || '-' }}</TableCell>
          <TableCell class="text-sm">{{ formatDateTime(caseItem.createdAt) }}</TableCell>
          <TableCell class="text-sm">{{ formatDateTime(caseItem.updatedAt) }}</TableCell>
          <TableCell>
            <Badge :variant="caseItem.archived ? 'secondary' : 'outline'" class="text-xs">
              {{ caseItem.archived ? '已归档' : '未归档' }}
            </Badge>
          </TableCell>

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
