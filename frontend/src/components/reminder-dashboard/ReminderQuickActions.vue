
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import SafeIcon from '@/components/common/SafeIcon.vue'
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
    <!-- Calendar View Button -->
    <Button
      as="a"
      href="./case-calendar-view.html"
      variant="outline"
      class="h-auto py-4 flex flex-col items-center justify-center gap-2"
    >
      <SafeIcon name="Calendar" :size="24" class="text-primary" />
      <span class="text-sm font-medium">案件日历视图</span>
      <span class="text-xs text-muted-foreground">按月份查看关键时间节点</span>
    </Button>

    <!-- Notification Center Button -->
    <Button
      as="a"
      href="./notification-center.html"
      variant="outline"
      class="h-auto py-4 flex flex-col items-center justify-center gap-2"
    >
      <SafeIcon name="Bell" :size="24" class="text-primary" />
      <span class="text-sm font-medium">系统通知中心</span>
      <span class="text-xs text-muted-foreground">查看所有系统通知</span>
    </Button>

    <!-- Case List Button -->
    <Button
      as="a"
      href="./case-list-view.html"
      variant="outline"
      class="h-auto py-4 flex flex-col items-center justify-center gap-2"
    >
      <SafeIcon name="FolderOpen" :size="24" class="text-primary" />
      <span class="text-sm font-medium">返回案件列表</span>
      <span class="text-xs text-muted-foreground">查看所有案件信息</span>
    </Button>
  </div>
</template>
