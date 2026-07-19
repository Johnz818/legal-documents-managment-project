
<script setup lang="ts">
import { ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import { ScrollArea } from '@/components/ui/scroll-area'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { RolePermissionSetModel, PermissionModel } from '@/data/user'

interface Props {
  open: boolean
  role: RolePermissionSetModel
}

interface Emits {
  save: [permissions: PermissionModel[]]
  close: []
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 定义权限分组
const permissionGroups = [
  {
    category: '用户管理',
    permissions: [
      { action: 'create', entity: 'User', label: '创建用户' },
      { action: 'read', entity: 'User', label: '查看用户' },
      { action: 'update', entity: 'User', label: '编辑用户' },
      { action: 'delete', entity: 'User', label: '删除用户' },
    ],
  },
  {
    category: '案件管理',
    permissions: [
      { action: 'create', entity: 'Case', label: '创建案件' },
      { action: 'read', entity: 'Case', label: '查看案件' },
      { action: 'update', entity: 'Case', label: '编辑案件' },
      { action: 'delete', entity: 'Case', label: '删除案件' },
    ],
  },
  {
    category: '文书管理',
    permissions: [
      { action: 'create', entity: 'Document', label: '生成文书' },
      { action: 'read', entity: 'Document', label: '查看文书' },
      { action: 'update', entity: 'Document', label: '编辑文书' },
      { action: 'delete', entity: 'Document', label: '删除文书' },
    ],
  },
  {
    category: '提醒管理',
    permissions: [
      { action: 'create', entity: 'Reminder', label: '创建提醒' },
      { action: 'read', entity: 'Reminder', label: '查看提醒' },
      { action: 'update', entity: 'Reminder', label: '编辑提醒' },
    ],
  },
  {
    category: '系统管理',
    permissions: [
      { action: 'read', entity: 'Audit_Log', label: '查看审计日志' },
      { action: 'management', entity: 'Role_Permission', label: '角色权限配置' },
    ],
  },
]

const selectedPermissions = ref<Set<string>>(new Set())

watch(
  () => props.role,
  (newRole) => {
    selectedPermissions.value = new Set(
      newRole.permissions.map(p => `${p.entity}-${p.action}`)
    )
  },
  { immediate: true }
)

const togglePermission = (entity: string, action: string) => {
  const key = `${entity}-${action}`
  if (selectedPermissions.value.has(key)) {
    selectedPermissions.value.delete(key)
  } else {
    selectedPermissions.value.add(key)
  }
}

const isPermissionSelected = (entity: string, action: string) => {
  return selectedPermissions.value.has(`${entity}-${action}`)
}

const handleSave = () => {
  const permissions: PermissionModel[] = []
  
  permissionGroups.forEach(group => {
    group.permissions.forEach(perm => {
      if (isPermissionSelected(perm.entity, perm.action)) {
        permissions.push({
          action: perm.action,
          entity: perm.entity,
          label: perm.label,
        })
      }
    })
  })
  
  emit('save', permissions)
}

const handleClose = () => {
  emit('close')
}
</script>

<template>
  <Dialog :open="open" @update:open="handleClose">
    <DialogContent class="sm:max-w-[600px]">
      <DialogHeader>
        <DialogTitle>
          编辑权限 - {{ role.roleName }}
        </DialogTitle>
      </DialogHeader>

      <ScrollArea class="h-[400px] pr-4">
        <div class="space-y-4">
          <div
            v-for="group in permissionGroups"
            :key="group.category"
            class="space-y-3"
          >
            <h4 class="font-semibold text-sm">{{ group.category }}</h4>
            <div class="grid grid-cols-2 gap-3 pl-4">
              <div
                v-for="permission in group.permissions"
                :key="`${permission.entity}-${permission.action}`"
                class="flex items-center space-x-2"
              >
                <Checkbox
                  :id="`perm-${permission.entity}-${permission.action}`"
                  :checked="isPermissionSelected(permission.entity, permission.action)"
                  @update:checked="togglePermission(permission.entity, permission.action)"
                />
                <Label
                  :for="`perm-${permission.entity}-${permission.action}`"
                  class="text-sm font-normal cursor-pointer"
                >
                  {{ permission.label }}
                </Label>
              </div>
            </div>
          </div>
        </div>
      </ScrollArea>

      <DialogFooter>
        <Button variant="outline" @click="handleClose">
          取消
        </Button>
        <Button @click="handleSave">
          <SafeIcon name="Save" :size="16" class="mr-2" />
          保存权限
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
