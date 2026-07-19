
<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import DashboardCard from '@/components/user-management-dashboard/DashboardCard.vue'
import StatisticsPanel from '@/components/user-management-dashboard/StatisticsPanel.vue'

// Mock data for dashboard statistics
const dashboardStats = ref({
  totalUsers: 24,
  totalRoles: 5,
  activePermissions: 48,
  lastUpdated: '2024-01-15 14:30',
})

// Mock data for quick action cards
const actionCards = ref([
  {
    id: 'user_management',
    title: '用户列表管理',
    description: '创建、编辑、禁用用户账户，管理用户信息和角色分配',
    icon: 'Users',
    actionLabel: '进入用户管理',
    actionHref: './user-list.html',
    stats: {
      label: '活跃用户',
      value: '18',
    },
  },
  {
    id: 'role_permission',
    title: '角色与权限设置',
    description: '定义用户角色，配置细粒度权限，确保系统安全',
    icon: 'ShieldCheck',
    actionLabel: '管理角色权限',
    actionHref: './permission-role-management.html',
    stats: {
      label: '已配置角色',
      value: '5',
    },
  },
  {
    id: 'case_management',
    title: '案件管理员权限',
    description: '查看和管理案件相关的权限配置，确保数据访问控制',
    icon: 'Gavel',
    actionLabel: '查看案件权限',
    actionHref: './case-detail-view.html',
    stats: {
      label: '权限配置',
      value: '12',
    },
  },
])

// Mock data for recent activities
const recentActivities = ref([
  {
    id: 1,
    type: 'user_created',
    description: '新用户 张三 已创建',
    timestamp: '2024-01-15 10:30',
    icon: 'UserPlus',
  },
  {
    id: 2,
    type: 'role_updated',
    description: '角色 "主办律师" 权限已更新',
    timestamp: '2024-01-14 15:45',
    icon: 'Edit',
  },
  {
    id: 3,
    type: 'user_disabled',
    description: '用户 李四 已禁用',
    timestamp: '2024-01-14 09:20',
    icon: 'UserX',
  },
  {
    id: 4,
    type: 'permission_granted',
    description: '用户 王五 被授予 "案件编辑" 权限',
    timestamp: '2024-01-13 14:15',
    icon: 'CheckCircle',
  },
])

const breadcrumbs = [
  { label: '系统管理', href: '#' },
  { label: '用户管理与权限控制中心' },
]
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- Page Header -->
    <PageHeader
      title="用户管理与权限控制中心"
      description="集中管理用户账户、角色分配和权限配置，维护系统安全与团队协作"
      :breadcrumbs="breadcrumbs"
    />

    <!-- Main Content -->
    <div class="flex-1 overflow-auto">
      <div class="container mx-auto px-4 py-6 space-y-6">
        <!-- Statistics Panel -->
        <StatisticsPanel :stats="dashboardStats" />

        <!-- Quick Action Cards -->
        <div class="space-y-4">
          <h2 class="text-lg font-semibold">快速操作</h2>
          <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            <DashboardCard
              v-for="card in actionCards"
              :key="card.id"
              :card="card"
            />
          </div>
        </div>

        <!-- Recent Activities -->
        <Card>
          <CardHeader>
            <CardTitle>最近活动</CardTitle>
            <CardDescription>系统中最近的用户和权限相关操作</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-4">
              <div
                v-for="activity in recentActivities"
                :key="activity.id"
                class="flex items-start gap-4 pb-4 border-b last:border-b-0 last:pb-0"
              >
                <!-- Activity Icon -->
                <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-muted flex-shrink-0">
                  <SafeIcon :name="activity.icon" :size="20" class="text-muted-foreground" />
                </div>

                <!-- Activity Content -->
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium">{{ activity.description }}</p>
                  <p class="text-xs text-muted-foreground mt-1">{{ activity.timestamp }}</p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- System Information -->
        <Card>
          <CardHeader>
            <CardTitle>系统信息</CardTitle>
            <CardDescription>系统配置和安全状态概览</CardDescription>
          </CardHeader>
          <CardContent>
            <div class="grid gap-4 md:grid-cols-2">
              <div class="space-y-2">
                <p class="text-sm text-muted-foreground">最后更新时间</p>
                <p class="text-sm font-medium">{{ dashboardStats.lastUpdated }}</p>
              </div>
              <div class="space-y-2">
                <p class="text-sm text-muted-foreground">系统状态</p>
                <div class="flex items-center gap-2">
                  <Badge variant="outline" class="bg-green-50 text-green-700 border-green-200">
                    <SafeIcon name="CheckCircle" :size="14" class="mr-1" />
                    正常运行
                  </Badge>
                </div>
              </div>
              <div class="space-y-2">
                <p class="text-sm text-muted-foreground">数据备份状态</p>
                <div class="flex items-center gap-2">
                  <Badge variant="outline" class="bg-blue-50 text-blue-700 border-blue-200">
                    <SafeIcon name="Database" :size="14" class="mr-1" />
                    已备份
                  </Badge>
                </div>
              </div>
              <div class="space-y-2">
                <p class="text-sm text-muted-foreground">安全审计</p>
                <Button variant="outline" size="sm" as="a" href="./audit-log.html">
                  查看审计日志
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>
