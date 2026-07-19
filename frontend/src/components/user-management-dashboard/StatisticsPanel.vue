
<script setup lang="ts">
import { Card, CardContent } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Stats {
  totalUsers: number
  totalRoles: number
  activePermissions: number
  lastUpdated: string
}

interface Props {
  stats: Stats
}

defineProps<Props>()

const statisticsItems = [
  {
    id: 'users',
    label: '用户总数',
    icon: 'Users',
    color: 'bg-blue-50 text-blue-700',
    key: 'totalUsers',
  },
  {
    id: 'roles',
    label: '角色数量',
    icon: 'Shield',
    color: 'bg-purple-50 text-purple-700',
    key: 'totalRoles',
  },
  {
    id: 'permissions',
    label: '权限配置',
    icon: 'Lock',
    color: 'bg-green-50 text-green-700',
    key: 'activePermissions',
  },
]
</script>

<template>
  <div class="grid gap-4 md:grid-cols-3">
    <Card v-for="item in statisticsItems" :key="item.id">
      <CardContent class="pt-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-muted-foreground">{{ item.label }}</p>
            <p class="text-3xl font-bold mt-2">{{ stats[item.key as keyof Stats] }}</p>
          </div>
          <div :class="`flex h-12 w-12 items-center justify-center rounded-lg ${item.color}`">
            <SafeIcon :name="item.icon" :size="24" />
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
