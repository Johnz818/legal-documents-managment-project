
<script setup lang="ts">
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { MOCK_ADMIN_PERMISSION_SET, type RoleModel } from '@/data/user'

interface Props {
  roles: RoleModel[]
}

defineProps<Props>()

// 定义所有可用的权限
const allPermissions = [
  { entity: 'User', action: 'create', label: '创建用户' },
  { entity: 'User', action: 'read', label: '查看用户' },
  { entity: 'User', action: 'update', label: '编辑用户' },
  { entity: 'User', action: 'delete', label: '删除用户' },
  { entity: 'Case', action: 'create', label: '创建案件' },
  { entity: 'Case', action: 'read', label: '查看案件' },
  { entity: 'Case', action: 'update', label: '编辑案件' },
  { entity: 'Case', action: 'delete', label: '删除案件' },
  { entity: 'Document', action: 'create', label: '生成文书' },
  { entity: 'Document', action: 'read', label: '查看文书' },
  { entity: 'Document', action: 'update', label: '编辑文书' },
  { entity: 'Document', action: 'delete', label: '删除文书' },
  { entity: 'Reminder', action: 'create', label: '创建提醒' },
  { entity: 'Reminder', action: 'read', label: '查看提醒' },
  { entity: 'Reminder', action: 'update', label: '编辑提醒' },
  { entity: 'Audit_Log', action: 'read', label: '查看审计日志' },
]

// 模拟获取角色权限
const getRolePermissions = (roleId: string) => {
  if (roleId === MOCK_ADMIN_PERMISSION_SET.roleId) {
    return MOCK_ADMIN_PERMISSION_SET.permissions
  }
  return []
}

const hasPermission = (roleId: string, entity: string, action: string) => {
  const permissions = getRolePermissions(roleId)
  return permissions.some(p => p.entity === entity && p.action === action)
}
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>权限矩阵</CardTitle>
    </CardHeader>
    <CardContent>
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b">
              <th class="text-left py-3 px-4 font-medium">权限项</th>
              <th
                v-for="role in roles"
                :key="role.roleId"
                class="text-center py-3 px-4 font-medium"
              >
                <div class="flex flex-col items-center gap-1">
                  <span class="text-xs">{{ role.roleName }}</span>
                  <Badge variant="outline" class="text-xs">{{ role.memberCount }}</Badge>
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(permission, index) in allPermissions"
              :key="`${permission.entity}-${permission.action}`"
              :class="index % 2 === 0 ? 'bg-muted/30' : ''"
            >
              <td class="py-3 px-4 font-medium text-left">
                <div class="flex flex-col">
                  <span>{{ permission.label }}</span>
                  <span class="text-xs text-muted-foreground">{{ permission.entity }}</span>
                </div>
              </td>
              <td
                v-for="role in roles"
                :key="`${role.roleId}-${permission.entity}-${permission.action}`"
                class="text-center py-3 px-4"
              >
                <div class="flex justify-center">
                  <Checkbox
                    :checked="hasPermission(role.roleId, permission.entity, permission.action)"
                    disabled
                  />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 图例 -->
      <div class="mt-6 pt-4 border-t flex items-center gap-4 text-sm">
        <div class="flex items-center gap-2">
          <Checkbox checked disabled />
          <span>已授予权限</span>
        </div>
        <div class="flex items-center gap-2">
          <Checkbox disabled />
          <span>未授予权限</span>
        </div>
      </div>
    </CardContent>
  </Card>
</template>
