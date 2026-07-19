
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import RoleCard from '@/components/permission-role-management/RoleCard.vue'
import PermissionMatrix from '@/components/permission-role-management/PermissionMatrix.vue'
import RoleEditDialog from '@/components/permission-role-management/RoleEditDialog.vue'
import PermissionSelector from '@/components/permission-role-management/PermissionSelector.vue'
import { MOCK_ROLES, MOCK_ADMIN_PERMISSION_SET, type RoleModel, type RolePermissionSetModel } from '@/data/user'

const roles = ref<RoleModel[]>(MOCK_ROLES)
const selectedRoleId = ref<string | null>(MOCK_ROLES[0]?.roleId || null)
const showRoleDialog = ref(false)
const editingRole = ref<RoleModel | null>(null)
const showPermissionEditor = ref(false)

// 获取选中角色的权限配置
const selectedRolePermissions = computed(() => {
  if (!selectedRoleId.value) return null
  
  // 模拟获取该角色的权限配置
  if (selectedRoleId.value === MOCK_ADMIN_PERMISSION_SET.roleId) {
    return MOCK_ADMIN_PERMISSION_SET
  }
  
  // 其他角色返回基础权限集
  const role = roles.value.find(r => r.roleId === selectedRoleId.value)
  if (!role) return null
  
  return {
    roleId: role.roleId,
    roleName: role.roleName,
    description: role.description,
    memberCount: role.memberCount,
    permissions: []
  } as RolePermissionSetModel
})

const handleCreateRole = () => {
  editingRole.value = null
  showRoleDialog.value = true
}

const handleEditRole = (role: RoleModel) => {
  editingRole.value = role
  showRoleDialog.value = true
}

const handleSaveRole = (roleData: any) => {
  if (editingRole.value) {
    // 编辑现有角色
    const index = roles.value.findIndex(r => r.roleId === editingRole.value!.roleId)
    if (index !== -1) {
      roles.value[index] = { ...roles.value[index], ...roleData }
    }
  } else {
    // 创建新角色
    const newRole: RoleModel = {
      roleId: `r${String(roles.value.length + 1).padStart(2, '0')}`,
      roleName: roleData.roleName,
      description: roleData.description,
      memberCount: 0
    }
    roles.value.push(newRole)
  }
  showRoleDialog.value = false
  editingRole.value = null
}

const handleDeleteRole = (roleId: string) => {
  roles.value = roles.value.filter(r => r.roleId !== roleId)
  if (selectedRoleId.value === roleId) {
    selectedRoleId.value = roles.value[0]?.roleId || null
  }
}

const handleSelectRole = (roleId: string) => {
  selectedRoleId.value = roleId
}

const handleEditPermissions = () => {
  showPermissionEditor.value = true
}

const handleSavePermissions = (permissions: any[]) => {
  // 保存权限配置
  showPermissionEditor.value = false
}

const breadcrumbs = [
  { label: '用户管理中心', href: './user-management-dashboard.html' },
  { label: '角色与权限管理' }
]
</script>

<template>
  <div class="flex flex-col h-full">
    <PageHeader 
      title="角色与权限管理"
      description="定义用户角色并分配系统功能和数据访问权限"
      :breadcrumbs="breadcrumbs"
    >
      <template #actions>
        <Button as="a" href="./user-management-dashboard.html" variant="outline">
          <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
          返回用户管理
        </Button>
        <Button @click="handleCreateRole">
          <SafeIcon name="Plus" :size="16" class="mr-2" />
          创建新角色
        </Button>
      </template>
    </PageHeader>

    <div class="flex-1 overflow-auto">
      <div class="container mx-auto px-4 py-6">
        <Tabs :value="selectedRoleId || ''" class="w-full">
          <!-- 角色列表标签页 -->
          <TabsList class="grid w-full grid-cols-2 mb-6">
            <TabsTrigger value="roles-list">角色列表</TabsTrigger>
            <TabsTrigger value="permissions-matrix">权限矩阵</TabsTrigger>
          </TabsList>

          <!-- 角色列表视图 -->
          <TabsContent value="roles-list" class="space-y-6">
            <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              <RoleCard
                v-for="role in roles"
                :key="role.roleId"
                :role="role"
                :is-selected="selectedRoleId === role.roleId"
                @select="handleSelectRole"
                @edit="handleEditRole"
                @delete="handleDeleteRole"
              />
            </div>

            <!-- 选中角色的权限详情 -->
            <div v-if="selectedRolePermissions" class="mt-8">
              <Card>
                <CardHeader class="flex flex-row items-center justify-between">
                  <div>
                    <CardTitle>{{ selectedRolePermissions.roleName }}</CardTitle>
                    <CardDescription>{{ selectedRolePermissions.description }}</CardDescription>
                  </div>
                  <Button @click="handleEditPermissions" size="sm">
                    <SafeIcon name="Edit" :size="16" class="mr-2" />
                    编辑权限
                  </Button>
                </CardHeader>
                <CardContent>
                  <div class="space-y-4">
                    <div class="flex items-center justify-between">
                      <span class="text-sm font-medium">成员数量</span>
                      <Badge variant="secondary">{{ selectedRolePermissions.memberCount }} 人</Badge>
                    </div>
                    
                    <div class="border-t pt-4">
                      <h4 class="font-medium mb-3">已分配权限</h4>
                      <div v-if="selectedRolePermissions.permissions.length > 0" class="space-y-2">
                        <div
                          v-for="permission in selectedRolePermissions.permissions"
                          :key="`${permission.entity}-${permission.action}`"
                          class="flex items-center justify-between p-3 bg-muted rounded-lg"
                        >
                          <div>
                            <p class="font-medium text-sm">{{ permission.label }}</p>
                            <p class="text-xs text-muted-foreground">{{ permission.entity }} - {{ permission.action }}</p>
                          </div>
                          <SafeIcon name="Check" :size="16" class="text-green-600" />
                        </div>
                      </div>
                      <div v-else class="text-center py-6 text-muted-foreground">
                        <p>暂无权限配置</p>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          <!-- 权限矩阵视图 -->
          <TabsContent value="permissions-matrix">
            <PermissionMatrix :roles="roles" />
          </TabsContent>
        </Tabs>
      </div>
    </div>

    <!-- 角色编辑对话框 -->
    <RoleEditDialog
      :open="showRoleDialog"
      :role="editingRole"
      @save="handleSaveRole"
      @close="showRoleDialog = false"
    />

    <!-- 权限编辑对话框 -->
    <PermissionSelector
      v-if="selectedRolePermissions"
      :open="showPermissionEditor"
      :role="selectedRolePermissions"
      @save="handleSavePermissions"
      @close="showPermissionEditor = false"
    />
  </div>
</template>
