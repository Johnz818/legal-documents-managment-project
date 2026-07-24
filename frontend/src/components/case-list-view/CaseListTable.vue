<script setup lang="ts">
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
import type { CaseSummaryResponse } from '@/types/case'

interface Props {
  cases: CaseSummaryResponse[]
  isLoading: boolean
  errorMessage: string
  isFiltered: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  retry: []
}>()
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
      <Button variant="outline" size="sm" @click="emit('retry')">
        重新加载
      </Button>
    </div>
  </div>

  <div v-else-if="cases.length === 0" class="flex h-96 items-center justify-center">
    <div v-if="isFiltered" class="flex max-w-md flex-col items-center gap-2 text-center">
      <SafeIcon name="SearchX" :size="28" class="text-muted-foreground" />
      <p class="font-medium">未找到匹配的案件</p>
      <p class="text-sm text-muted-foreground">请调整查询条件后重试。</p>
    </div>
    <EmptyState v-else variant="cases" />
  </div>

  <div v-else class="w-full overflow-x-auto">
    <Table>
      <TableHeader>
        <TableRow class="whitespace-nowrap">
          <TableHead>案号</TableHead>
          <TableHead>法院</TableHead>
          <TableHead>案由</TableHead>
          <TableHead>原告/申请人</TableHead>
          <TableHead>案件阶段</TableHead>
          <TableHead>主办律师</TableHead>
          <TableHead>立案日期</TableHead>
          <TableHead>开庭日期</TableHead>
          <TableHead>标签</TableHead>
          <TableHead class="w-12 text-right">操作</TableHead>
        </TableRow>
      </TableHeader>

      <TableBody>
        <TableRow v-for="caseItem in cases" :key="caseItem.id" class="hover:bg-muted/50">
          <TableCell class="font-medium">
            <a
              :href="`./case-detail-view.html?id=${caseItem.id}`"
              class="text-primary hover:underline"
            >
              {{ caseItem.caseNumber }}
            </a>
          </TableCell>

          <TableCell class="text-sm">{{ caseItem.courtName || '-' }}</TableCell>
          <TableCell class="text-sm">{{ caseItem.caseCause || '-' }}</TableCell>
          <TableCell class="text-sm">{{ caseItem.plaintiff }}</TableCell>
          <TableCell>
            <Badge variant="outline" class="text-xs">
              {{ caseItem.status }}
            </Badge>
          </TableCell>
          <TableCell class="text-sm">{{ caseItem.leadLawyerName }}</TableCell>
          <TableCell class="text-sm">{{ caseItem.filingDate || '-' }}</TableCell>
          <TableCell class="text-sm">{{ caseItem.hearingDate || '-' }}</TableCell>
          <TableCell class="text-sm text-muted-foreground">
            尚未接入
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
