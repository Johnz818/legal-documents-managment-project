
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import SafeIcon from '@/components/common/SafeIcon.vue'
import CaseTagForm from '@/components/case-tag-management/CaseTagForm.vue'
import { MOCK_CASE_TAGS, type CaseTagModel } from '@/data/case'

// State
const tags = ref<CaseTagModel[]>([...MOCK_CASE_TAGS])
const searchQuery = ref('')
const isCreateDialogOpen = ref(false)
const isEditDialogOpen = ref(false)
const isDeleteDialogOpen = ref(false)
const selectedTag = ref<CaseTagModel | null>(null)
const tagToDelete = ref<CaseTagModel | null>(null)

// Computed
const filteredTags = computed(() => {
  if (!searchQuery.value) return tags.value
  return tags.value.filter(tag =>
    tag.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// Methods
const handleCreateTag = (newTag: Omit<CaseTagModel, 'id'>) => {
  const id = `t${Date.now()}`
  tags.value.push({
    id,
    ...newTag,
  })
  isCreateDialogOpen.value = false
}

const handleEditTag = (updatedTag: CaseTagModel) => {
  const index = tags.value.findIndex(t => t.id === updatedTag.id)
  if (index !== -1) {
    tags.value[index] = updatedTag
  }
  isEditDialogOpen.value = false
  selectedTag.value = null
}

const openEditDialog = (tag: CaseTagModel) => {
  selectedTag.value = tag
  isEditDialogOpen.value = true
}

const openDeleteDialog = (tag: CaseTagModel) => {
  tagToDelete.value = tag
  isDeleteDialogOpen.value = true
}

const confirmDelete = () => {
  if (tagToDelete.value) {
    tags.value = tags.value.filter(t => t.id !== tagToDelete.value!.id)
    isDeleteDialogOpen.value = false
    tagToDelete.value = null
  }
}

// Color class mapping
const colorClassMap: Record<string, string> = {
  'bg-red-500': 'bg-red-100 text-red-800',
  'bg-yellow-500': 'bg-yellow-100 text-yellow-800',
  'bg-purple-500': 'bg-purple-100 text-purple-800',
  'bg-blue-500': 'bg-blue-100 text-blue-800',
  'bg-gray-500': 'bg-gray-100 text-gray-800',
  'bg-green-500': 'bg-green-100 text-green-800',
  'bg-pink-500': 'bg-pink-100 text-pink-800',
  'bg-indigo-500': 'bg-indigo-100 text-indigo-800',
}

const getTagDisplayClass = (bgClass: string) => {
  return colorClassMap[bgClass] || 'bg-gray-100 text-gray-800'
}
</script>

<template>
  <div class="flex flex-col gap-6 p-6">
    <!-- Header Section -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex-1">
        <Input
          v-model="searchQuery"
          placeholder="搜索标签名称..."
          class="max-w-sm"
        />
      </div>
      <Dialog v-model:open="isCreateDialogOpen">
        <DialogTrigger as-child>
          <Button>
            <SafeIcon name="Plus" :size="16" class="mr-2" />
            创建新标签
          </Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>创建新标签</DialogTitle>
            <DialogDescription>
              输入标签名称并选择颜色，用于案件分类和筛选
            </DialogDescription>
          </DialogHeader>
          <CaseTagForm
            @submit="handleCreateTag"
            @close="isCreateDialogOpen = false"
          />
        </DialogContent>
      </Dialog>
    </div>

    <!-- Tags Grid -->
    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      <Card
        v-for="tag in filteredTags"
        :key="tag.id"
        class="flex flex-col justify-between"
      >
        <CardHeader class="pb-3">
          <div class="flex items-start justify-between gap-2">
            <div class="flex-1">
              <CardTitle class="text-base">{{ tag.name }}</CardTitle>
              <CardDescription class="text-xs">ID: {{ tag.id }}</CardDescription>
            </div>
            <div :class="['h-8 w-8 rounded', tag.color]" />
          </div>
        </CardHeader>
        <CardContent>
          <div class="flex gap-2">
            <Badge :class="getTagDisplayClass(tag.color)">
              {{ tag.name }}
            </Badge>
          </div>
        </CardContent>
        <div class="flex gap-2 border-t p-3">
          <Button
            variant="outline"
            size="sm"
            class="flex-1"
            @click="openEditDialog(tag)"
          >
            <SafeIcon name="Edit2" :size="14" class="mr-1" />
            编辑
          </Button>
<Button
             variant="outline"
             size="sm"
             class="flex-1 text-destructive hover:text-destructive"
             @click="openDeleteDialog(tag)"
           >
             <SafeIcon name="Trash2" :size="14" class="mr-1" />
             删除
           </Button>
        </div>
      </Card>
    </div>

    <!-- Empty State -->
    <div v-if="filteredTags.length === 0" class="flex flex-col items-center justify-center gap-4 py-12">
      <div class="flex h-16 w-16 items-center justify-center rounded-full bg-muted">
        <SafeIcon name="Tags" :size="32" class="text-muted-foreground" />
      </div>
      <div class="text-center">
        <h3 class="font-semibold">{{ searchQuery ? '未找到匹配的标签' : '暂无标签' }}</h3>
        <p class="text-sm text-muted-foreground">
          {{ searchQuery ? '尝试修改搜索条件' : '点击上方按钮创建新标签' }}
        </p>
      </div>
    </div>

    <!-- Edit Dialog -->
    <Dialog v-model:open="isEditDialogOpen">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>编辑标签</DialogTitle>
          <DialogDescription>
            修改标签名称和颜色
          </DialogDescription>
        </DialogHeader>
        <CaseTagForm
          v-if="selectedTag"
          :initial-tag="selectedTag"
          @submit="handleEditTag"
          @close="isEditDialogOpen = false"
        />
      </DialogContent>
    </Dialog>

<!-- Delete Dialog -->
    <Dialog v-model:open="isDeleteDialogOpen">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>删除标签</DialogTitle>
          <DialogDescription>
            确定要删除标签"{{ tagToDelete?.name }}"吗？此操作无法撤销。
          </DialogDescription>
        </DialogHeader>
        <div class="flex gap-2 justify-end">
          <Button variant="outline" @click="isDeleteDialogOpen = false">
            取消
          </Button>
          <Button @click="confirmDelete" class="bg-destructive text-destructive-foreground hover:bg-destructive/90">
            删除
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>
