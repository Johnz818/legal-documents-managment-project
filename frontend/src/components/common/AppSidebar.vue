
<script setup lang="ts">
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
} from '@/components/ui/sidebar'
import SafeIcon from '@/components/common/SafeIcon.vue'

const menuGroups = [
  {
    label: '案件管理',
    items: [
      { label: '案件列表', href: './case-list-view.html', icon: 'FolderOpen' },
      { label: '创建案件', href: './case-create-manual.html', icon: 'FilePlus' },
      { label: '批量导入', href: './case-import-batch.html', icon: 'Upload' },
      { label: '标签管理', href: './case-tag-management.html', icon: 'Tags' },
    ],
  },
  {
    label: '文书管理',
    items: [
      { label: '文书生成', href: './document-generation-entry.html', icon: 'FileText' },
      { label: '模板管理', href: './document-template-management.html', icon: 'Layout' },
    ],
  },
  {
    label: '提醒事项',
    items: [
      { label: '提醒仪表盘', href: './reminder-dashboard.html', icon: 'Bell' },
      { label: '案件日历', href: './case-calendar-view.html', icon: 'Calendar' },
      { label: '通知中心', href: './notification-center.html', icon: 'Inbox' },
    ],
  },
  {
    label: '系统管理',
    items: [
      { label: '用户管理', href: './user-management-dashboard.html', icon: 'Users' },
      { label: '角色权限', href: './permission-role-management.html', icon: 'Shield' },
    ],
  },
]
</script>

<template>
  <Sidebar 
    class="top-[--header-height] h-[calc(100vh-var(--header-height))]" 
    variant="inset"
  >
    <SidebarContent>
      <SidebarGroup v-for="group in menuGroups" :key="group.label">
        <SidebarGroupLabel>{{ group.label }}</SidebarGroupLabel>
        <SidebarGroupContent>
          <SidebarMenu>
            <SidebarMenuItem v-for="item in group.items" :key="item.href">
              <SidebarMenuButton as-child>
                <a :href="item.href">
                  <SafeIcon :name="item.icon" :size="16" />
                  <span>{{ item.label }}</span>
                </a>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroupContent>
      </SidebarGroup>
    </SidebarContent>
  </Sidebar>
</template>
