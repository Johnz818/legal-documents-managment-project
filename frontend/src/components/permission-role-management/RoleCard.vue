
<script setup lang="ts">
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu'
import type { RoleModel } from '@/data/user'

interface Props {
  role: RoleModel
  isSelected?: boolean
}

interface Emits {
  select: [roleId: string]
  edit: [role: RoleModel]
  delete: [roleId: string]
}

defineProps<Props>()
defineEmits<Emits>()
</script>

<template>
  <Card
    :class="['cursor-pointer transition-all', isSelected ? 'ring-2 ring-primary' : 'hover:shadow-md']"
    @click="$emit('select', role.roleId)"
  >
    <CardHeader class="pb-3">
      <div class="flex items-start justify-between">
        <div class="flex-1">
          <CardTitle class="text-base">{{ role.roleName }}</CardTitle>
          <CardDescription class="text-xs mt-1">{{ role.description }}</CardDescription>
        </div>
        <DropdownMenu>
          <DropdownMenuTrigger as-child @click.stop>
            <Button variant="ghost" size="icon" class="h-8 w-8">
              <SafeIcon name="MoreVertical" :size="16" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem @click="$emit('edit', role)">
              <SafeIcon name="Edit" :size="16" class="mr-2" />
              编辑
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              @click="$emit('delete', role.roleId)"
              class="text-destructive focus:text-destructive"
            >
              <SafeIcon name="Trash" :size="16" class="mr-2" />
              删除
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </CardHeader>
    <CardContent>
      <div class="flex items-center justify-between">
        <span class="text-sm text-muted-foreground">成员数量</span>
        <Badge variant="secondary">{{ role.memberCount }} 人</Badge>
      </div>
    </CardContent>
  </Card>
</template>
