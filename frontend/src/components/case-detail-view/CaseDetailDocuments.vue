
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Empty, EmptyHeader, EmptyTitle, EmptyDescription, EmptyContent } from '@/components/ui/empty'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { MOCK_DOCUMENTS } from '@/data/document'

interface Props {
  caseId: string
}

const props = defineProps<Props>()

const caseDocuments = computed(() => {
  return MOCK_DOCUMENTS.filter(doc => doc.caseId === props.caseId)
})

const handleViewDocument = (documentId: string) => {
  if (typeof window !== 'undefined') {
    window.location.href = `./document-preview-edit.html?documentId=${documentId}`
  }
}
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>相关文书</CardTitle>
    </CardHeader>
    <CardContent>
      <div v-if="caseDocuments.length > 0" class="space-y-4">
        <div
          v-for="doc in caseDocuments"
          :key="doc.documentId"
          class="flex items-center justify-between p-4 border rounded-lg hover:bg-muted/50 transition-colors"
        >
          <div class="flex items-center gap-3 flex-1">
            <SafeIcon name="FileText" :size="20" class="text-muted-foreground" />
            <div class="flex-1 min-w-0">
              <p class="font-medium truncate">{{ doc.docName }}</p>
              <p class="text-xs text-muted-foreground">
                生成于 {{ doc.generatedDate }} | 阶段: {{ doc.stage }}
              </p>
            </div>
          </div>
          <Button
            variant="outline"
            size="sm"
            @click="handleViewDocument(doc.documentId)"
          >
            查看
          </Button>
        </div>
      </div>
      <div v-else>
        <Empty class="py-8">
          <EmptyHeader>
            <div class="flex h-16 w-16 items-center justify-center rounded-full bg-muted mx-auto mb-4">
              <SafeIcon name="FileText" :size="32" class="text-muted-foreground" />
            </div>
            <EmptyTitle>暂无文书</EmptyTitle>
            <EmptyDescription>该案件还没有生成任何文书</EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button
              as="a"
              href="./document-generation-entry.html"
              size="sm"
            >
              生成文书
            </Button>
          </EmptyContent>
        </Empty>
      </div>
    </CardContent>
  </Card>
</template>
