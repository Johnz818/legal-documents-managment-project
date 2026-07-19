
<script setup lang="ts">
import { Empty, EmptyHeader, EmptyTitle, EmptyDescription, EmptyContent } from '@/components/ui/empty'
import { Button } from '@/components/ui/button'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  variant?: 'cases' | 'documents' | 'notifications' | 'users' | 'default'
  title?: string
  description?: string
  actionLabel?: string
  actionHref?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
})

const variantConfig = {
  cases: {
    icon: 'FolderOpen',
    title: '暂无案件',
    description: '您还没有创建任何案件，点击下方按钮开始创建',
    actionLabel: '创建案件',
    actionHref: './case-create-manual.html',
  },
  documents: {
    icon: 'FileText',
    title: '暂无文书',
    description: '还没有生成任何文书，选择案件开始生成',
    actionLabel: '生成文书',
    actionHref: './document-generation-entry.html',
  },
  notifications: {
    icon: 'Bell',
    title: '暂无通知',
    description: '您目前没有任何待处理的通知',
  },
  users: {
    icon: 'Users',
    title: '暂无用户',
    description: '系统中还没有用户，点击下方按钮添加用户',
    actionLabel: '添加用户',
    actionHref: './user-create-edit.html',
  },
  default: {
    icon: 'Inbox',
    title: '暂无数据',
    description: '当前没有可显示的内容',
  },
}

const config = variantConfig[props.variant]
const finalTitle = props.title || config.title
const finalDescription = props.description || config.description
const finalActionLabel = props.actionLabel || config.actionLabel
const finalActionHref = props.actionHref || config.actionHref
</script>

<template>
  <Empty class="py-12">
    <EmptyHeader>
      <div class="flex h-20 w-20 items-center justify-center rounded-full bg-muted mx-auto mb-4">
        <SafeIcon :name="config.icon" :size="40" class="text-muted-foreground" />
      </div>
      <EmptyTitle>{{ finalTitle }}</EmptyTitle>
      <EmptyDescription>{{ finalDescription }}</EmptyDescription>
    </EmptyHeader>
    <EmptyContent v-if="finalActionLabel && finalActionHref">
      <Button as="a" :href="finalActionHref">
        {{ finalActionLabel }}
      </Button>
    </EmptyContent>
  </Empty>
</template>
