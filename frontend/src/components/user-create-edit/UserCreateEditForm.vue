
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useForm } from 'vee-validate'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { UserRole, UserStatus, type UserModel, MOCK_USERS, getUserById } from '@/data/user'

// Validation schema
const validationSchema = z.object({
  name: z.string().min(2, '姓名至少需要2个字符').max(50, '姓名不能超过50个字符'),
  email: z.string().email('请输入有效的邮箱地址'),
  role: z.enum([UserRole.Admin, UserRole.LeadAttorney, UserRole.Assistant], {
    errorMap: () => ({ message: '请选择有效的角色' }),
  }),
  team: z.string().min(1, '请输入所属团队'),
  status: z.enum([UserStatus.Active, UserStatus.Disabled], {
    errorMap: () => ({ message: '请选择有效的状态' }),
  }),
})

const isLoading = ref(false)
const isEditMode = ref(false)
const currentUserId = ref<string | null>(null)
const originalEmail = ref<string>('')

const { handleSubmit, setValues } = useForm({
  validationSchema,
  initialValues: {
    name: '',
    email: '',
    role: UserRole.Assistant,
    team: '',
    status: UserStatus.Active,
  },
})

// Get userId from URL query parameter
const getUserIdFromUrl = (): string | null => {
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search)
    return params.get('userId')
  }
  return null
}

// Load user data for edit mode
const loadUserData = (userId: string) => {
  const user = getUserById(userId)
  if (user) {
    isEditMode.value = true
    currentUserId.value = userId
    originalEmail.value = user.email
    setValues({
      name: user.name,
      email: user.email,
      role: user.role,
      team: user.team,
      status: user.status,
    })
  }
}

// Initialize on mount
onMounted(() => {
  if (typeof window !== 'undefined') {
    const userId = getUserIdFromUrl()
    if (userId) {
      loadUserData(userId)
    }
  }
})

// Handle form submission
const onSubmit = handleSubmit(async (values) => {
  isLoading.value = true
  try {
    // Simulate API call
    await new Promise((resolve) => setTimeout(resolve, 1000))

    // In a real app, this would call an API endpoint
    if (isEditMode.value) {
      console.log('编辑用户:', { id: currentUserId.value, ...values })
    } else {
      console.log('创建新用户:', values)
    }

    // Redirect to user list
    if (typeof window !== 'undefined') {
      window.location.href = './user-list.html'
    }
  } catch (error) {
    console.error('保存用户失败:', error)
  } finally {
    isLoading.value = false
  }
})

// Handle cancel
const handleCancel = () => {
  if (typeof window !== 'undefined') {
    window.location.href = './user-list.html'
  }
}

const pageTitle = computed(() => (isEditMode.value ? '编辑用户' : '创建新用户'))
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <Card>
      <CardHeader>
        <CardTitle>{{ pageTitle }}</CardTitle>
        <CardDescription>
          {{ isEditMode ? '修改用户的基本信息和权限设置' : '填写以下信息创建新的系统用户' }}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Form @submit="onSubmit" class="space-y-6">
          <!-- Name Field -->
          <FormField v-slot="{ componentField }" name="name">
            <FormItem>
              <FormLabel>姓名 <span class="text-destructive">*</span></FormLabel>
              <FormControl>
                <Input
                  v-bind="componentField"
                  type="text"
                  placeholder="请输入用户姓名"
                  :disabled="isLoading"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <!-- Email Field -->
          <FormField v-slot="{ componentField }" name="email">
            <FormItem>
              <FormLabel>邮箱地址 <span class="text-destructive">*</span></FormLabel>
              <FormControl>
                <Input
                  v-bind="componentField"
                  type="email"
                  placeholder="请输入邮箱地址"
                  :disabled="isLoading"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <!-- Role Field -->
          <FormField v-slot="{ componentField }" name="role">
            <FormItem>
              <FormLabel>用户角色 <span class="text-destructive">*</span></FormLabel>
              <Select v-bind="componentField">
                <FormControl>
                  <SelectTrigger :disabled="isLoading">
                    <SelectValue placeholder="请选择用户角色" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem :value="UserRole.Admin">
                    {{ UserRole.Admin }}
                  </SelectItem>
                  <SelectItem :value="UserRole.LeadAttorney">
                    {{ UserRole.LeadAttorney }}
                  </SelectItem>
                  <SelectItem :value="UserRole.Assistant">
                    {{ UserRole.Assistant }}
                  </SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          </FormField>

          <!-- Team Field -->
          <FormField v-slot="{ componentField }" name="team">
            <FormItem>
              <FormLabel>所属团队 <span class="text-destructive">*</span></FormLabel>
              <FormControl>
                <Input
                  v-bind="componentField"
                  type="text"
                  placeholder="例如：民事诉讼一组、知识产权组"
                  :disabled="isLoading"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <!-- Status Field -->
          <FormField v-slot="{ componentField }" name="status">
            <FormItem>
              <FormLabel>账户状态 <span class="text-destructive">*</span></FormLabel>
              <Select v-bind="componentField">
                <FormControl>
                  <SelectTrigger :disabled="isLoading">
                    <SelectValue placeholder="请选择账户状态" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem :value="UserStatus.Active">
                    {{ UserStatus.Active }}
                  </SelectItem>
                  <SelectItem :value="UserStatus.Disabled">
                    {{ UserStatus.Disabled }}
                  </SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          </FormField>

          <!-- Form Actions -->
          <div class="flex gap-3 justify-end pt-6 border-t">
            <Button
              type="button"
              variant="outline"
              @click="handleCancel"
              :disabled="isLoading"
            >
              <SafeIcon name="X" :size="16" class="mr-2" />
              取消
            </Button>
            <Button type="submit" :disabled="isLoading">
              <SafeIcon v-if="!isLoading" name="Save" :size="16" class="mr-2" />
              <SafeIcon v-else name="Loader2" :size="16" class="mr-2 animate-spin" />
              {{ isEditMode ? '保存修改' : '创建用户' }}
            </Button>
          </div>
        </Form>
      </CardContent>
    </Card>

    <!-- Info Box -->
    <div class="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
      <div class="flex gap-3">
        <SafeIcon name="Info" :size="20" class="text-blue-600 flex-shrink-0 mt-0.5" />
        <div class="text-sm text-blue-800">
          <p class="font-semibold mb-1">提示信息</p>
          <ul class="list-disc list-inside space-y-1">
            <li>创建用户后，系统将自动生成初始密码并通过邮件发送</li>
            <li>用户可在首次登录时修改密码</li>
            <li>禁用账户后，该用户将无法登录系统</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>
