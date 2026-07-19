
<script setup lang="ts">
import type { DocumentTemplateModel } from '@/data/document'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  open: boolean
  template: DocumentTemplateModel
}

interface Emits {
  (e: 'close'): void
}

defineProps<Props>()
defineEmits<Emits>()
</script>

<template>
  <Dialog :open="open" @update:open="$emit('close')">
    <DialogContent class="max-w-2xl max-h-[80vh] flex flex-col">
      <DialogHeader>
        <DialogTitle>{{ template.name }}</DialogTitle>
        <DialogDescription>
          {{ template.description }}
        </DialogDescription>
      </DialogHeader>

      <ScrollArea class="flex-1 border rounded-md p-4 bg-muted/50">
        <div class="prose prose-sm max-w-none dark:prose-invert">
          <pre class="whitespace-pre-wrap break-words text-sm font-mono">{{ template.contentTemplate }}</pre>
        </div>
      </ScrollArea>

      <div class="flex gap-2 justify-end pt-4 border-t">
        <Button variant="outline" @click="$emit('close')">
          关闭
        </Button>
        <Button as="a" href="./document-generate-selection.html" class="gap-2">
          <SafeIcon name="FileText" :size="18" />
          使用此模板生成文书
        </Button>
      </div>
    </DialogContent>
  </Dialog>
</template>
