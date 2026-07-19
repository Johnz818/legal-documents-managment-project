
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Badge } from '@/components/ui/badge'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import NotificationItem from '@/components/notification-center/NotificationItem.vue'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { NotificationModel } from '@/data/reminder'
import { MOCK_NOTIFICATIONS } from '@/data/reminder'

const notifications = ref<NotificationModel[]>(MOCK_NOTIFICATIONS)
const activeTab = ref<'all' | 'unread' | 'alert' | 'info' | 'success'>('all')

const unreadCount = computed(() => notifications.value.filter(n => !n.isRead).length)

const filteredNotifications = computed(() => {
  let filtered = notifications.value

  if (activeTab.value === 'unread') {
    filtered = filtered.filter(n => !n.isRead)
  } else if (activeTab.value !== 'all') {
    filtered = filtered.filter(n => n.type === activeTab.value)
  }

  return filtered.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
})

const markAsRead = (notificationId: string) => {
  const notification = notifications.value.find(n => n.notificationId === notificationId)
  if (notification) {
    notification.isRead = true
  }
}

const markAllAsRead = () => {
  notifications.value.forEach(n => {
    n.isRead = true
  })
}

const deleteNotification = (notificationId: string) => {
  notifications.value = notifications.value.filter(n => n.notificationId !== notificationId)
}

const deleteAllRead = () => {
  notifications.value = notifications.value.filter(n => !n.isRead)
}

const getTypeLabel = (type: string) => {
  const typeMap: Record<string, string> = {
    alert: '警告',
    info: '信息',
    success: '成功',
  }
  return typeMap[type] || type
}

const getTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    alert: 'destructive',
    info: 'default',
    success: 'default',
  }
  return colorMap[type] || 'default'
}
</script>

<template>
  <div class="flex flex-col h-full">
    <PageHeader
      title="系统通知中心"
      description="查看和管理所有系统通知和提醒"
      :breadcrumbs="[
        { label: '首页', href: './case-list-view.html' },
        { label: '提醒事项', href: './reminder-dashboard.html' },
        { label: '通知中心' },
      ]"
    >
      <template #actions>
        <Button
          v-if="unreadCount > 0"
          variant="outline"
          size="sm"
          @click="markAllAsRead"
        >
          <SafeIcon name="Check" :size="16" class="mr-2" />
          全部标记为已读
        </Button>
        <Button
          v-if="notifications.some(n => n.isRead)"
          variant="outline"
          size="sm"
          @click="deleteAllRead"
        >
          <SafeIcon name="Trash2" :size="16" class="mr-2" />
          删除已读
        </Button>
        <Button as="a" href="./reminder-dashboard.html" variant="outline" size="sm">
          <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
          返回仪表盘
        </Button>
      </template>
    </PageHeader>

    <div class="flex-1 overflow-auto">
      <div class="container mx-auto px-4 py-6">
        <!-- Tabs for filtering -->
        <Tabs :value="activeTab" @update:value="(val) => activeTab = val" class="w-full">
          <TabsList class="grid w-full grid-cols-5">
            <TabsTrigger value="all">
              全部
              <Badge v-if="notifications.length > 0" variant="secondary" class="ml-2">
                {{ notifications.length }}
              </Badge>
            </TabsTrigger>
            <TabsTrigger value="unread">
              未读
              <Badge v-if="unreadCount > 0" variant="destructive" class="ml-2">
                {{ unreadCount }}
              </Badge>
            </TabsTrigger>
            <TabsTrigger value="alert">
              警告
              <Badge
                v-if="notifications.filter(n => n.type === 'alert').length > 0"
                variant="destructive"
                class="ml-2"
              >
                {{ notifications.filter(n => n.type === 'alert').length }}
              </Badge>
            </TabsTrigger>
            <TabsTrigger value="info">
              信息
              <Badge
                v-if="notifications.filter(n => n.type === 'info').length > 0"
                variant="secondary"
                class="ml-2"
              >
                {{ notifications.filter(n => n.type === 'info').length }}
              </Badge>
            </TabsTrigger>
            <TabsTrigger value="success">
              成功
              <Badge
                v-if="notifications.filter(n => n.type === 'success').length > 0"
                variant="secondary"
                class="ml-2"
              >
                {{ notifications.filter(n => n.type === 'success').length }}
              </Badge>
            </TabsTrigger>
          </TabsList>

          <TabsContent :value="activeTab" class="mt-6">
            <!-- Empty State -->
            <div v-if="filteredNotifications.length === 0" class="py-12">
              <EmptyState variant="notifications" />
            </div>

            <!-- Notification List -->
            <div v-else class="space-y-3">
              <NotificationItem
                v-for="notification in filteredNotifications"
                :key="notification.notificationId"
                :notification="notification"
                :type-label="getTypeLabel(notification.type)"
                :type-color="getTypeColor(notification.type)"
                @mark-as-read="markAsRead"
                @delete="deleteNotification"
              />
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.tabs) {
  @apply w-full;
}
</style>
