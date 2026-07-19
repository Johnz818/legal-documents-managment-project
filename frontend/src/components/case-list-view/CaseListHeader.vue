
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import SafeIcon from '@/components/common/SafeIcon.vue'

const actions = [
  {
    label: '创建案件',
    icon: 'Plus',
    href: './case-create-manual.html',
    variant: 'default' as const,
  },
  {
    label: '批量导入',
    icon: 'Upload',
    href: './case-import-batch.html',
    variant: 'outline' as const,
  },
  {
    label: '标签管理',
    icon: 'Tags',
    href: './case-tag-management.html',
    variant: 'outline' as const,
  },
]
</script>

<template>
  <div class="flex items-center justify-between gap-2">
    <div class="flex-1" />
    <div class="flex items-center gap-2">
      <Button
        v-for="action in actions"
        :key="action.label"
        :variant="action.variant"
        size="sm"
        as="a"
        :href="action.href"
      >
        <SafeIcon :name="action.icon" :size="16" class="mr-1" />
        {{ action.label }}
      </Button>
    </div>
  </div>
</template>
