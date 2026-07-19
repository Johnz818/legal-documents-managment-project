
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { ReminderType, ReminderMethod, type ReminderModel } from '@/data/reminder'
import { CaseStage } from '@/data/case'

interface Props {
  reminder?: ReminderModel | null
  caseId: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  save: [data: any]
  cancel: []
}>()

// Form state
const title = ref('')
const description = ref('')
const targetDate = ref('')
const targetTime = ref('09:00')
const reminderType = ref<ReminderType>(ReminderType.Custom)
const linkedStage = ref<string>('N/A')
const selectedMethods = ref<ReminderMethod[]>([ReminderMethod.System])
const isCompleted = ref(false)

// Initialize form with reminder data if editing
watch(() => props.reminder, (reminder) => {
  if (reminder) {
    title.value = reminder.title
    targetDate.value = reminder.targetDate.split(' ')[0]
    targetTime.value = reminder.targetDate.split(' ')[1] || '09:00'
    reminderType.value = reminder.type
    linkedStage.value = reminder.linkedStage
    selectedMethods.value = reminder.reminderMethod
    isCompleted.value = reminder.isCompleted
  } else {
    // Reset form for new reminder
    title.value = ''
    description.value = ''
    targetDate.value = ''
    targetTime.value = '09:00'
    reminderType.value = ReminderType.Custom
    linkedStage.value = 'N/A'
    selectedMethods.value = [ReminderMethod.System]
    isCompleted.value = false
  }
}, { immediate: true })

const toggleMethod = (method: ReminderMethod) => {
  const index = selectedMethods.value.indexOf(method)
  if (index > -1) {
    selectedMethods.value.splice(index, 1)
  } else {
    selectedMethods.value.push(method)
  }
}

const isMethodSelected = (method: ReminderMethod) => {
  return selectedMethods.value.includes(method)
}

const handleSubmit = () => {
  if (!title.value || !targetDate.value) {
    alert('请填写提醒标题和时间')
    return
  }

  const formData = {
    title: title.value,
    description: description.value,
    targetDate: `${targetDate.value} ${targetTime.value}`,
    type: reminderType.value,
    linkedStage: linkedStage.value,
    reminderMethod: selectedMethods.value,
    isCompleted: isCompleted.value,
  }

  emit('save', formData)
}

const caseStages = Object.values(CaseStage)
</script>

<template>
  <form @submit.prevent="handleSubmit" class="space-y-6">
    <!-- Title -->
    <div class="space-y-2">
      <Label for="title">提醒标题 *</Label>
      <Input
        id="title"
        v-model="title"
        placeholder="例如：开庭提醒、证据提交截止"
        required
      />
    </div>

    <!-- Description -->
    <div class="space-y-2">
      <Label for="description">提醒描述</Label>
      <Textarea
        id="description"
        v-model="description"
        placeholder="添加更多细节信息（可选）"
        rows="3"
      />
    </div>

    <!-- Date and Time -->
    <div class="grid grid-cols-2 gap-4">
      <div class="space-y-2">
        <Label for="date">提醒日期 *</Label>
        <Input
          id="date"
          v-model="targetDate"
          type="date"
          required
        />
      </div>
      <div class="space-y-2">
        <Label for="time">提醒时间 *</Label>
        <Input
          id="time"
          v-model="targetTime"
          type="time"
          required
        />
      </div>
    </div>

    <!-- Reminder Type -->
    <div class="space-y-2">
      <Label for="type">提醒类型</Label>
      <Select v-model="reminderType">
        <SelectTrigger id="type">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem :value="ReminderType.KeyDate">
            {{ ReminderType.KeyDate }}
          </SelectItem>
          <SelectItem :value="ReminderType.Custom">
            {{ ReminderType.Custom }}
          </SelectItem>
          <SelectItem :value="ReminderType.SystemAlert">
            {{ ReminderType.SystemAlert }}
          </SelectItem>
        </SelectContent>
      </Select>
    </div>

    <!-- Linked Stage -->
    <div class="space-y-2">
      <Label for="stage">关联案件阶段</Label>
      <Select v-model="linkedStage">
        <SelectTrigger id="stage">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="N/A">无关联</SelectItem>
          <SelectItem v-for="stage in caseStages" :key="stage" :value="stage">
            {{ stage }}
          </SelectItem>
        </SelectContent>
      </Select>
    </div>

    <!-- Reminder Methods -->
    <div class="space-y-3">
      <Label>提醒方式</Label>
      <div class="space-y-2">
        <div class="flex items-center space-x-2">
          <Checkbox
            id="system"
            :checked="isMethodSelected(ReminderMethod.System)"
            @update:checked="toggleMethod(ReminderMethod.System)"
          />
          <Label for="system" class="font-normal cursor-pointer flex items-center gap-2">
            <SafeIcon name="Bell" :size="16" />
            {{ ReminderMethod.System }}
          </Label>
        </div>
        <div class="flex items-center space-x-2">
          <Checkbox
            id="email"
            :checked="isMethodSelected(ReminderMethod.Email)"
            @update:checked="toggleMethod(ReminderMethod.Email)"
          />
          <Label for="email" class="font-normal cursor-pointer flex items-center gap-2">
            <SafeIcon name="Mail" :size="16" />
            {{ ReminderMethod.Email }}
          </Label>
        </div>
      </div>
    </div>

    <!-- Completed Status -->
    <div class="flex items-center space-x-2">
      <Checkbox
        id="completed"
        v-model:checked="isCompleted"
      />
      <Label for="completed" class="font-normal cursor-pointer">
        标记为已完成
      </Label>
    </div>

    <!-- Form Actions -->
    <div class="flex gap-2 justify-end pt-4 border-t">
      <Button type="button" variant="outline" @click="emit('cancel')">
        取消
      </Button>
      <Button type="submit">
        <SafeIcon name="Save" :size="16" class="mr-2" />
        保存提醒
      </Button>
    </div>
  </form>
</template>
