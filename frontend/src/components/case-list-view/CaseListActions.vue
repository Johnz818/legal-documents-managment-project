
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from '@/components/ui/dialog'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { CaseStage, MOCK_CASE_TAGS } from '@/data/case'

// Batch action state
const selectedCount = ref(0)
const batchStageAction = ref<string>('')
const batchTagAction = ref<string>('')

// Get all case stages
const caseStages = Object.values(CaseStage)

// Handle batch stage update
const handleBatchStageUpdate = () => {
  console.log(`Updating ${selectedCount.value} cases to stage: ${batchStageAction.value}`)
  batchStageAction.value = ''
}

// Handle batch tag update
const handleBatchTagUpdate = () => {
  console.log(`Adding tag ${batchTagAction.value} to ${selectedCount.value} cases`)
  batchTagAction.value = ''
}

// Handle batch delete
const handleBatchDelete = () => {
  console.log(`Deleting ${selectedCount.value} cases`)
}

// Handle export
const handleExport = () => {
  console.log('Exporting cases to Excel')
}

// Show batch actions only if items are selected
const showBatchActions = computed(() => selectedCount.value > 0)
</script>

<template>
  <div v-if="showBatchActions" class="flex items-center gap-2 rounded-lg border border-primary/20 bg-primary/5 p-3">
    <SafeIcon name="AlertCircle" :size="16" class="text-primary" />
    <span class="text-sm font-medium">已选择 {{ selectedCount }} 个案件</span>

    <div class="ml-auto flex items-center gap-2">
      <!-- Batch stage update -->
      <div class="flex items-center gap-2">
        <Select v-model="batchStageAction">
          <SelectTrigger class="h-8 w-40">
            <SelectValue placeholder="修改阶段" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem v-for="stage in caseStages" :key="stage" :value="stage">
              {{ stage }}
            </SelectItem>
          </SelectContent>
        </Select>
        <Button
          v-if="batchStageAction"
          size="sm"
          variant="outline"
          @click="handleBatchStageUpdate"
          class="h-8"
        >
          应用
        </Button>
      </div>

      <!-- Batch tag update -->
      <div class="flex items-center gap-2">
        <Select v-model="batchTagAction">
          <SelectTrigger class="h-8 w-40">
            <SelectValue placeholder="添加标签" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem v-for="tag in MOCK_CASE_TAGS" :key="tag.id" :value="tag.id">
              {{ tag.name }}
            </SelectItem>
          </SelectContent>
        </Select>
        <Button
          v-if="batchTagAction"
          size="sm"
          variant="outline"
          @click="handleBatchTagUpdate"
          class="h-8"
        >
          应用
        </Button>
      </div>

      <!-- Export button -->
      <Button
        size="sm"
        variant="outline"
        @click="handleExport"
        class="h-8"
      >
        <SafeIcon name="Download" :size="16" class="mr-1" />
        导出
      </Button>

<!-- Delete button -->
      <Dialog>
        <DialogTrigger as-child>
          <Button
            size="sm"
            variant="destructive"
            class="h-8"
          >
            <SafeIcon name="Trash2" :size="16" class="mr-1" />
            删除
          </Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
            <DialogDescription>
              您确定要删除选中的 {{ selectedCount }} 个案件吗？此操作无法撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose as-child>
              <Button variant="outline">取消</Button>
            </DialogClose>
            <Button @click="handleBatchDelete" variant="destructive">
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  </div>
</template>
