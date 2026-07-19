
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'

const notificationCount = ref(0)

onMounted(() => {
  if (typeof window !== 'undefined') {
    notificationCount.value = 3
  }
})

const menuItems = [
  { label: '案件管理', href: './case-list-view.html' },
  { label: '文书生成', href: './document-generation-entry.html' },
  { label: '提醒事项', href: './reminder-dashboard.html' },
  { label: '用户管理', href: './user-management-dashboard.html' },
]
</script>

<template>
  <header class="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
    <div class="container flex h-16 items-center justify-between px-4">
      <!-- Logo & Title -->
      <div class="flex items-center gap-6">
        <a href="./index.html" class="flex items-center gap-2">
          <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <SafeIcon name="Scale" :size="20" />
          </div>
          <span class="hidden font-semibold sm:inline-block">诉讼案件管理系统</span>
        </a>

        <!-- Desktop Navigation -->
        <nav class="hidden md:flex items-center gap-1">
          <Button
            v-for="item in menuItems"
            :key="item.href"
            variant="ghost"
            as="a"
            :href="item.href"
            class="text-sm"
          >
            {{ item.label }}
          </Button>
        </nav>
      </div>

      <!-- Right Actions -->
      <div class="flex items-center gap-2">
        <!-- Notifications -->
        <Button variant="ghost" size="icon" as="a" href="./notification-center.html" class="relative">
          <SafeIcon name="Bell" :size="20" />
          <Badge
            v-if="notificationCount > 0"
            variant="destructive"
            class="absolute -right-1 -top-1 h-5 w-5 rounded-full p-0 text-xs flex items-center justify-center"
          >
            {{ notificationCount }}
          </Badge>
        </Button>

        <!-- User Menu -->
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="ghost" size="icon">
              <SafeIcon name="User" :size="20" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" class="w-56">
            <DropdownMenuLabel>我的账户</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem as="a" href="./user-profile.html">
              <SafeIcon name="User" :size="16" class="mr-2" />
              个人信息
            </DropdownMenuItem>
            <DropdownMenuItem as="a" href="./settings.html">
              <SafeIcon name="Settings" :size="16" class="mr-2" />
              系统设置
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem>
              <SafeIcon name="LogOut" :size="16" class="mr-2" />
              退出登录
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        <!-- Mobile Menu -->
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="ghost" size="icon" class="md:hidden">
              <SafeIcon name="Menu" :size="20" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" class="w-56">
            <DropdownMenuLabel>导航菜单</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              v-for="item in menuItems"
              :key="item.href"
              as="a"
              :href="item.href"
            >
              {{ item.label }}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  </header>
</template>
