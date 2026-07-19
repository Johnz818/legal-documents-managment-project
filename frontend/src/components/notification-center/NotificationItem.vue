
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { NotificationModel } from '@/data/reminder'
import { getCaseById } from '@/data/case'

interface Props {
  notification: NotificationModel
  typeLabel: string
  typeColor: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  markAsRead: [notificationId: string]
  delete: [notificationId: string]
}>()

const caseInfo = computed(() => {
  if (props.notification.caseId) {
    return getCaseById(props.notification.caseId)
  }
  return null
})

const getTypeIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    alert: 'AlertCircle',
    info: 'Info',
    success: 'CheckCircle',
  }
  return iconMap[type] || 'Bell'
}

const getTypeIconColor = (type: string) => {
  const colorMap: Record<string, string> = {
    alert: '#ef4444',
    info: '#3b82f6',
    success: '#10b981',
  }
  return colorMap[type] || '#6b7280'
}

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`

  return date.toLocaleDateString('zh-CN')
}

const handleMarkAsRead = () => {
  emit('markAsRead', props.notification.notificationId)
}

const handleDelete = () => {
  emit('delete', props.notification.notificationId)
}

const handleNavigateToCase = () => {
  if (props.notification.caseId) {
    window.location.href = `./case-detail-view.html?id=${props.notification.caseId}`
  }
}
</script>

<template>
  <Card
    :class="[
      'transition-all hover:shadow-md',
      !notification.isRead ? 'border-primary/50 bg-primary/5' : '',
    ]"
  >
    <CardContent class="p-4">
      <div class="flex gap-4">
        <!-- Icon -->
        <div class="flex-shrink-0 pt-1">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-full"
            :style="{ backgroundColor: getTypeIconColor(notification.type) + '20' }"
          >
            <SafeIcon
              :name="getTypeIcon(notification.type)"
              :size="20"
              :color="getTypeIconColor(notification.type)"
            />
          </div>
        </div>

        <!-- Content -->
        <div class="flex-1 min-w-0">
          <div class="flex items-start justify-between gap-2">
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <Badge :variant="typeColor" class="text-xs">
                  {{ typeLabel }}
                </Badge>
                <span class="text-xs text-muted-foreground">
                  {{ formatTime(notification.timestamp) }}
                </span>
                <div
                  v-if="!notification.isRead"
                  class="h-2 w-2 rounded-full bg-primary"
                />
              </div>
              <p class="text-sm text-foreground leading-relaxed">
                {{ notification.message }}
              </p>
              <div v-if="caseInfo" class="mt-2 text-xs text-muted-foreground">
                <span class="font-medium">相关案件：</span>
                <span>{{ caseInfo.caseNumber }} - {{ caseInfo.caseCause }}</span>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex items-center gap-2 mt-3">
            <Button
              v-if="!notification.isRead"
              variant="ghost"
              size="sm"
              @click="handleMarkAsRead"
              class="text-xs h-8"
            >
              <SafeIcon name="Check" :size="14" class="mr-1" />
              标记为已读
            </Button>
            <Button
              v-if="caseInfo"
              variant="ghost"
              size="sm"
              @click="handleNavigateToCase"
              class="text-xs h-8 text-primary hover:text-primary"
            >
              <SafeIcon name="ExternalLink" :size="14" class="mr-1" />
              查看案件
            </Button>
            <Button
              variant="ghost"
              size="sm"
              @click="handleDelete"
              class="text-xs h-8 text-destructive hover:text-destructive ml-auto"
            >
              <SafeIcon name="Trash2" :size="14" />
            </Button>
          </div>
        </div>
      </div>
    </CardContent>
  </Card>
</template>
