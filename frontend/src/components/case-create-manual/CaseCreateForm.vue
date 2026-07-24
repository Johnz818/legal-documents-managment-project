
<script setup lang="ts">
import { ref, computed } from 'vue'
import { useForm } from 'vee-validate'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
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
import SafeIcon from '@/components/common/SafeIcon.vue'
import { CASE_STATUS_OPTIONS, CASE_STATUS_VALUES } from '@/constants/caseStatus'
import { MOCK_USERS, UserRole } from '@/data/user'

// Validation schema
const validationSchema = z.object({
  caseNumber: z.string().min(1, '案号不能为空').min(5, '案号至少5个字符'),
  courtName: z.string().min(1, '法院名称不能为空'),
  plaintiff: z.string().min(1, '原告/申请人不能为空'),
  defendant: z.string().min(1, '被告/被申请人不能为空'),
  caseCause: z.string().min(1, '案由不能为空'),
  status: z.enum(CASE_STATUS_VALUES, {
    required_error: '案件阶段不能为空',
    invalid_type_error: '请选择支持的案件阶段',
  }),
  leadAttorneyId: z.string().min(1, '主办律师不能为空'),
  filingDate: z.string().optional(),
  hearingDate: z.string().optional(),
  judgmentDate: z.string().optional(),
  description: z.string().optional(),
})

const { handleSubmit, isSubmitting } = useForm({
  validationSchema,
  initialValues: {
    caseNumber: '',
    courtName: '',
    plaintiff: '',
    defendant: '',
    caseCause: '',
    status: undefined,
    leadAttorneyId: '',
    filingDate: '',
    hearingDate: '',
    judgmentDate: '',
    description: '',
  },
})

// Get lead attorneys list
const leadAttorneys = computed(() => {
  return MOCK_USERS.filter(u => u.role === UserRole.LeadAttorney)
})

// Handle form submission
const onSubmit = handleSubmit(async (values) => {
  try {
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // Prepare backend-aligned values for the future API integration ticket.
    const newCase = {
      id: `C${Date.now()}`,
      caseNumber: values.caseNumber,
      courtName: values.courtName,
      plaintiff: values.plaintiff,
      defendant: values.defendant,
      caseCause: values.caseCause,
      status: values.status,
      leadAttorneyId: values.leadAttorneyId,
      coAttorneysIds: [],
      filingDate: values.filingDate || null,
      hearingDate: values.hearingDate || null,
      judgmentDate: values.judgmentDate || null,
      tags: [],
      description: values.description,
    }
    
    console.log('New case created:', newCase)
    
    // Redirect to case list
    if (typeof window !== 'undefined') {
      window.location.href = './case-list-view.html'
    }
  } catch (error) {
    console.error('Failed to create case:', error)
  }
})

const isLoading = ref(false)

const handleCancel = () => {
  if (typeof window !== 'undefined') {
    window.location.href = './case-list-view.html'
  }
}
</script>

<template>
  <Form @submit="onSubmit" class="space-y-6">
    <!-- Basic Information Section -->
    <div class="space-y-4">
      <h3 class="text-lg font-semibold flex items-center gap-2">
        <SafeIcon name="FileText" :size="20" />
        基本信息
      </h3>
      
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- Case Number -->
        <FormField v-slot="{ componentField }" name="caseNumber">
          <FormItem>
            <FormLabel>案号 <span class="text-destructive">*</span></FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                placeholder="例如：(2024)京01民初188号"
                type="text"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Court Name -->
        <FormField v-slot="{ componentField }" name="courtName">
          <FormItem>
            <FormLabel>法院名称 <span class="text-destructive">*</span></FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                placeholder="例如：北京市第一中级人民法院"
                type="text"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Plaintiff -->
        <FormField v-slot="{ componentField }" name="plaintiff">
          <FormItem>
            <FormLabel>原告/申请人 <span class="text-destructive">*</span></FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                placeholder="输入原告姓名或单位"
                type="text"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Defendant -->
        <FormField v-slot="{ componentField }" name="defendant">
          <FormItem>
            <FormLabel>被告/被申请人 <span class="text-destructive">*</span></FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                placeholder="输入被告姓名或单位"
                type="text"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Case Cause -->
        <FormField v-slot="{ componentField }" name="caseCause">
          <FormItem>
            <FormLabel>案由 <span class="text-destructive">*</span></FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                placeholder="例如：房屋买卖合同纠纷"
                type="text"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Case Stage -->
        <FormField v-slot="{ componentField }" name="status">
          <FormItem>
            <FormLabel>案件阶段 <span class="text-destructive">*</span></FormLabel>
            <Select v-bind="componentField">
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="选择案件阶段" />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                <SelectItem
                  v-for="option in CASE_STATUS_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Lead Attorney -->
        <FormField v-slot="{ componentField }" name="leadAttorneyId">
          <FormItem>
            <FormLabel>主办律师 <span class="text-destructive">*</span></FormLabel>
            <Select v-bind="componentField">
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="选择主办律师" />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                <SelectItem v-for="attorney in leadAttorneys" :key="attorney.id" :value="attorney.id">
                  {{ attorney.name }}
                </SelectItem>
              </SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        </FormField>
      </div>
    </div>

    <!-- Optional Dates Section -->
    <div class="space-y-4">
      <h3 class="text-lg font-semibold flex items-center gap-2">
        <SafeIcon name="Calendar" :size="20" />
        重要日期（可选）
      </h3>
      
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <!-- Filing Date -->
        <FormField v-slot="{ componentField }" name="filingDate">
          <FormItem>
            <FormLabel>立案时间</FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                type="date"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Hearing Date -->
        <FormField v-slot="{ componentField }" name="hearingDate">
          <FormItem>
            <FormLabel>开庭时间</FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                type="date"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Judgment Date -->
        <FormField v-slot="{ componentField }" name="judgmentDate">
          <FormItem>
            <FormLabel>判决时间</FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                type="date"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>
      </div>
    </div>

    <!-- Description Section -->
    <div class="space-y-4">
      <h3 class="text-lg font-semibold flex items-center gap-2">
        <SafeIcon name="FileText" :size="20" />
        备注信息（可选）
      </h3>
      
      <FormField v-slot="{ componentField }" name="description">
        <FormItem>
          <FormLabel>案件描述</FormLabel>
          <FormControl>
            <Textarea
              v-bind="componentField"
              placeholder="输入案件的详细描述、特殊说明等信息"
              class="min-h-32"
            />
          </FormControl>
          <FormMessage />
        </FormItem>
      </FormField>
    </div>

    <!-- Action Buttons -->
    <div class="flex gap-3 justify-end pt-6 border-t">
      <Button
        type="button"
        variant="outline"
        @click="handleCancel"
        :disabled="isSubmitting"
      >
        取消
      </Button>
      <Button
        type="submit"
        :disabled="isSubmitting"
        class="gap-2"
      >
        <SafeIcon v-if="isSubmitting" name="Loader2" :size="16" class="animate-spin" />
        {{ isSubmitting ? '保存中...' : '保存案件' }}
      </Button>
    </div>
  </Form>
</template>
