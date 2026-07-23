
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { toast } from 'vue-sonner'
import SafeIcon from '@/components/common/SafeIcon.vue'
import VariableInsertPanel from './VariableInsertPanel.vue'
import TemplatePreview from './TemplatePreview.vue'
import { MOCK_DOCUMENT_TEMPLATES, MOCK_TEMPLATE_VARIABLES, TemplateType } from '@/data/document'

interface FormData {
  templateId: string
  name: string
  description: string
  contentTemplate: string
  isNew: boolean
}

const formData = ref<FormData>({
  templateId: '',
  name: '',
  description: '',
  contentTemplate: '',
  isNew: true,
})

const isSaving = ref(false)
const activeTab = ref('editor')

// 从URL查询参数获取模板ID（如果是编辑模式）
const getTemplateIdFromUrl = (): string | null => {
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search)
    return params.get('id')
  }
  return null
}

// 初始化表单数据
const initializeForm = () => {
  const templateId = getTemplateIdFromUrl()
  
  if (templateId) {
    // 编辑模式：加载现有模板
    const existingTemplate = MOCK_DOCUMENT_TEMPLATES.find(t => t.templateId === templateId && t.type === TemplateType.Custom)
    if (existingTemplate) {
      formData.value = {
        templateId: existingTemplate.templateId,
        name: existingTemplate.name,
        description: existingTemplate.description,
        contentTemplate: existingTemplate.contentTemplate,
        isNew: false,
      }
    }
  } else {
    // 新建模式：使用默认空值
    formData.value = {
      templateId: 'T' + Date.now().toString(),
      name: '',
      description: '',
      contentTemplate: '',
      isNew: true,
    }
  }
}

// SSG时初始化
initializeForm()

const pageTitle = computed(() => formData.value.isNew ? '创建新模板' : '编辑模板')

const handleSave = async () => {
  // 验证必填字段
  if (!formData.value.name.trim()) {
    toast.error('请输入模板名称')
    return
  }

  if (!formData.value.contentTemplate.trim()) {
    toast.error('请输入模板内容')
    return
  }

  isSaving.value = true

  try {
    // 模拟保存延迟
    await new Promise(resolve => setTimeout(resolve, 800))

    // 这里应该调用API保存模板
    console.log('保存模板:', formData.value)

    toast.success(formData.value.isNew ? '模板创建成功' : '模板更新成功')

    // 延迟后返回模板管理页面
    setTimeout(() => {
      if (typeof window !== 'undefined') {
        window.location.href = './document-template-management.html'
      }
    }, 500)
  } catch (error) {
    toast.error('保存失败，请重试')
    console.error('Save error:', error)
  } finally {
    isSaving.value = false
  }
}

const handleCancel = () => {
  if (typeof window !== 'undefined') {
    window.location.href = './document-template-management.html'
  }
}

const insertVariable = (variableName: string) => {
  // 在光标位置插入变量
  const textarea = document.querySelector('textarea[data-template-content]') as HTMLTextAreaElement
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const before = formData.value.contentTemplate.substring(0, start)
    const after = formData.value.contentTemplate.substring(end)
    formData.value.contentTemplate = before + variableName + after
    
    // 重新设置光标位置
    setTimeout(() => {
      textarea.focus()
      textarea.setSelectionRange(start + variableName.length, start + variableName.length)
    }, 0)
  }
}

onMounted(() => {
  if (typeof window !== 'undefined') {
    // 浏览器特定初始化
    initializeForm()
  }
})
</script>

<template>
  <div class="container mx-auto px-4 py-6">
    <div class="mb-6">
      <h2 class="text-2xl font-bold">{{ pageTitle }}</h2>
      <p class="text-muted-foreground mt-1">
        {{ formData.isNew ? '创建一个新的自定义文书模板' : '编辑现有的自定义文书模板' }}
      </p>
    </div>

    <Tabs v-model="activeTab" class="w-full">
      <TabsList class="grid w-full grid-cols-2">
        <TabsTrigger value="editor">编辑器</TabsTrigger>
        <TabsTrigger value="preview">预览</TabsTrigger>
      </TabsList>

      <!-- 编辑器标签页 -->
      <TabsContent value="editor" class="space-y-6">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- 左侧：编辑表单 -->
          <div class="lg:col-span-2 space-y-6">
            <!-- 模板基本信息 -->
            <Card>
              <CardHeader>
                <CardTitle>基本信息</CardTitle>
                <CardDescription>设置模板的名称和描述</CardDescription>
              </CardHeader>
              <CardContent class="space-y-4">
                <div class="space-y-2">
                  <Label for="template-name">模板名称 *</Label>
                  <Input
                    id="template-name"
                    v-model="formData.name"
                    placeholder="例如：民事起诉状（自定义版）"
                    class="w-full"
                  />
                </div>

                <div class="space-y-2">
                  <Label for="template-description">模板描述</Label>
                  <Textarea
                    id="template-description"
                    v-model="formData.description"
                    placeholder="简要描述此模板的用途和适用场景"
                    class="w-full min-h-20"
                  />
                </div>
              </CardContent>
            </Card>

            <!-- 模板内容编辑 -->
            <Card>
              <CardHeader>
                <CardTitle>模板内容</CardTitle>
                <CardDescription>编辑文书模板的主体内容，可插入占位符</CardDescription>
              </CardHeader>
              <CardContent class="space-y-4">
                <div class="space-y-2">
                  <Label for="template-content">内容 *</Label>
                  <Textarea
                    id="template-content"
                    data-template-content
                    v-model="formData.contentTemplate"
                    placeholder="输入模板内容，使用{{变量名}}格式插入占位符..."
                    class="w-full min-h-64 font-mono text-sm"
                  />
                  <p class="text-xs text-muted-foreground">
                    提示：在右侧面板选择变量并点击插入，或手动输入{{变量名}}格式
                  </p>
                </div>
              </CardContent>
            </Card>

            <!-- 操作按钮 -->
            <div class="flex gap-3 justify-end">
              <Button
                variant="outline"
                @click="handleCancel"
                :disabled="isSaving"
              >
                取消
              </Button>
              <Button
                @click="handleSave"
                :disabled="isSaving"
              >
                <SafeIcon v-if="isSaving" name="Loader2" :size="16" class="mr-2 animate-spin" />
                {{ isSaving ? '保存中...' : '保存模板' }}
              </Button>
            </div>
          </div>

          <!-- 右侧：变量插入面板 -->
          <div class="lg:col-span-1">
            <VariableInsertPanel
              :variables="MOCK_TEMPLATE_VARIABLES"
              @insert="insertVariable"
            />
          </div>
        </div>
      </TabsContent>

      <!-- 预览标签页 -->
      <TabsContent value="preview" class="space-y-6">
        <TemplatePreview
          :template-name="formData.name"
          :template-content="formData.contentTemplate"
          :template-description="formData.description"
        />
      </TabsContent>
    </Tabs>
  </div>
</template>

<style scoped>
/* 自定义样式 */
:deep(.sonner-toast) {
  font-family: inherit;
}
</style>
