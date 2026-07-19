
<script setup lang="ts">
import { ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { RoleModel } from '@/data/user'

interface Props {
  open: boolean
  role?: RoleModel | null
}

interface Emits {
  save: [roleData: { roleName: string; description: string }]
  close: []
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const roleName = ref('')
const description = ref('')

watch(
  () => props.role,
  (newRole) => {
    if (newRole) {
      roleName.value = newRole.roleName as string
      description.value = newRole.description
    } else {
      roleName.value = ''
      description.value = ''
    }
  }
)

const handleSave = () => {
  if (!roleName.value.trim()) {
    return
  }
  emit('save', {
    roleName: roleName.value,
    description: description.value,
  })
}

const handleClose = () => {
  roleName.value = ''
  description.value = ''
  emit('close')
}
</script>

<template>
  <Dialog :open="open" @update:open="handleClose">
    <DialogContent class="sm:max-w-[425px]">
      <DialogHeader>
        <DialogTitle>
          {{ role ? '编辑角色' : '创建新角色' }}
        </DialogTitle>
      </DialogHeader>

      <div class="space-y-4 py-4">
        <div class="space-y-2">
          <Label for="role-name">角色名称</Label>
          <Input
            id="role-name"
            v-model="roleName"
            placeholder="例如：主办律师、助理人员"
          />
        </div>

        <div class="space-y-2">
          <Label for="role-description">角色描述</Label>
          <Textarea
            id="role-description"
            v-model="description"
            placeholder="描述该角色的职责和权限范围"
            class="min-h-[100px]"
          />
        </div>
      </div>

      <DialogFooter>
        <Button variant="outline" @click="handleClose">
          取消
        </Button>
        <Button @click="handleSave" :disabled="!roleName.trim()">
          <SafeIcon name="Save" :size="16" class="mr-2" />
          保存
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
