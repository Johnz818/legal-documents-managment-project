
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Checkbox } from '@/components/ui/checkbox'
import SafeIcon from '@/components/common/SafeIcon.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { MOCK_USERS, UserRole, UserStatus, type UserModel } from '@/data/user'

// State
const searchQuery = ref('')
const selectedRole = ref<string>('')
const selectedStatus = ref<string>('')
const selectedUsers = ref<Set<string>>(new Set())

// Data
const users = ref<UserModel[]>(MOCK_USERS)

// Computed
const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchesSearch = 
      user.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.value.toLowerCase())
    
    const matchesRole = !selectedRole.value || user.role === selectedRole.value
    const matchesStatus = !selectedStatus.value || user.status === selectedStatus.value
    
    return matchesSearch && matchesRole && matchesStatus
  })
})

const isAllSelected = computed(() => {
  return filteredUsers.value.length > 0 && 
         selectedUsers.value.size === filteredUsers.value.length
})

const isIndeterminate = computed(() => {
  return selectedUsers.value.size > 0 && selectedUsers.value.size < filteredUsers.value.length
})

// Methods
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedUsers.value.clear()
  } else {
    filteredUsers.value.forEach(user => {
      selectedUsers.value.add(user.id)
    })
  }
}

const toggleSelectUser = (userId: string) => {
  if (selectedUsers.value.has(userId)) {
    selectedUsers.value.delete(userId)
  } else {
    selectedUsers.value.add(userId)
  }
}

const handleDisableSelected = () => {
  selectedUsers.value.forEach(userId => {
    const user = users.value.find(u => u.id === userId)
    if (user) {
      user.status = UserStatus.Disabled
    }
  })
  selectedUsers.value.clear()
}

const handleEnableSelected = () => {
  selectedUsers.value.forEach(userId => {
    const user = users.value.find(u => u.id === userId)
    if (user) {
      user.status = UserStatus.Active
    }
  })
  selectedUsers.value.clear()
}

const handleDeleteUser = (userId: string) => {
  const index = users.value.findIndex(u => u.id === userId)
  if (index > -1) {
    users.value.splice(index, 1)
    selectedUsers.value.delete(userId)
  }
}

const getRoleColor = (role: UserRole | string) => {
  switch (role) {
    case UserRole.Admin:
      return 'destructive'
    case UserRole.LeadAttorney:
      return 'default'
    case UserRole.Assistant:
      return 'secondary'
    default:
      return 'outline'
  }
}

const getStatusColor = (status: UserStatus) => {
  return status === UserStatus.Active ? 'default' : 'secondary'
}
</script>

<template>
  <div class="flex-1 flex flex-col gap-4 p-4">
    <!-- Filters & Actions -->
    <div class="flex flex-col gap-4 bg-card rounded-lg border p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <!-- Search -->
        <div class="flex-1">
          <Input
            v-model="searchQuery"
            placeholder="搜索用户名或邮箱..."
            class="w-full"
          />
        </div>

        <!-- Role Filter -->
        <Select v-model="selectedRole">
          <SelectTrigger class="w-full md:w-48">
            <SelectValue placeholder="按角色筛选" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">全部角色</SelectItem>
            <SelectItem :value="UserRole.Admin">{{ UserRole.Admin }}</SelectItem>
            <SelectItem :value="UserRole.LeadAttorney">{{ UserRole.LeadAttorney }}</SelectItem>
            <SelectItem :value="UserRole.Assistant">{{ UserRole.Assistant }}</SelectItem>
          </SelectContent>
        </Select>

        <!-- Status Filter -->
        <Select v-model="selectedStatus">
          <SelectTrigger class="w-full md:w-48">
            <SelectValue placeholder="按状态筛选" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">全部状态</SelectItem>
            <SelectItem :value="UserStatus.Active">{{ UserStatus.Active }}</SelectItem>
            <SelectItem :value="UserStatus.Disabled">{{ UserStatus.Disabled }}</SelectItem>
          </SelectContent>
        </Select>

        <!-- Create Button -->
        <Button as="a" href="./user-create-edit.html" class="w-full md:w-auto">
          <SafeIcon name="Plus" :size="16" class="mr-2" />
          创建用户
        </Button>
      </div>

      <!-- Batch Actions -->
      <div v-if="selectedUsers.size > 0" class="flex items-center gap-2 pt-2 border-t">
        <span class="text-sm text-muted-foreground">
          已选择 {{ selectedUsers.size }} 个用户
        </span>
        <Button
          variant="outline"
          size="sm"
          @click="handleDisableSelected"
        >
          <SafeIcon name="Ban" :size="16" class="mr-1" />
          禁用
        </Button>
        <Button
          variant="outline"
          size="sm"
          @click="handleEnableSelected"
        >
          <SafeIcon name="CheckCircle" :size="16" class="mr-1" />
          启用
        </Button>
      </div>
    </div>

    <!-- Table -->
    <div v-if="filteredUsers.length > 0" class="bg-card rounded-lg border overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead class="w-12">
              <Checkbox
                :checked="isAllSelected"
                :indeterminate="isIndeterminate"
                @update:checked="toggleSelectAll"
              />
            </TableHead>
            <TableHead>用户名</TableHead>
            <TableHead>邮箱</TableHead>
            <TableHead>角色</TableHead>
            <TableHead>所属团队</TableHead>
            <TableHead>状态</TableHead>
            <TableHead>创建时间</TableHead>
            <TableHead class="text-right">操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow v-for="user in filteredUsers" :key="user.id">
            <TableCell>
              <Checkbox
                :checked="selectedUsers.has(user.id)"
                @update:checked="toggleSelectUser(user.id)"
              />
            </TableCell>
            <TableCell class="font-medium">{{ user.name }}</TableCell>
            <TableCell class="text-sm text-muted-foreground">{{ user.email }}</TableCell>
            <TableCell>
              <Badge :variant="getRoleColor(user.role)">
                {{ user.role }}
              </Badge>
            </TableCell>
            <TableCell class="text-sm">{{ user.team }}</TableCell>
            <TableCell>
              <Badge :variant="getStatusColor(user.status)">
                {{ user.status }}
              </Badge>
            </TableCell>
            <TableCell class="text-sm text-muted-foreground">{{ user.createdAt }}</TableCell>
            <TableCell class="text-right">
              <DropdownMenu>
                <DropdownMenuTrigger as-child>
                  <Button variant="ghost" size="icon">
                    <SafeIcon name="MoreHorizontal" :size="16" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem as="a" :href="`./user-create-edit.html?id=${user.id}`">
                    <SafeIcon name="Edit" :size="16" class="mr-2" />
                    编辑
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    v-if="user.status === UserStatus.Active"
                    @click="() => { user.status = UserStatus.Disabled }"
                  >
                    <SafeIcon name="Ban" :size="16" class="mr-2" />
                    禁用
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    v-else
                    @click="() => { user.status = UserStatus.Active }"
                  >
                    <SafeIcon name="CheckCircle" :size="16" class="mr-2" />
                    启用
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    class="text-destructive"
                    @click="handleDeleteUser(user.id)"
                  >
                    <SafeIcon name="Trash2" :size="16" class="mr-2" />
                    删除
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>

    <!-- Empty State -->
    <div v-else class="flex-1 flex items-center justify-center">
      <EmptyState variant="users" />
    </div>

    <!-- Results Info -->
    <div v-if="filteredUsers.length > 0" class="text-sm text-muted-foreground px-4">
      显示 {{ filteredUsers.length }} / {{ users.length }} 个用户
    </div>
  </div>
</template>
