
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { CaseSummaryModel } from '@/data/case'

interface Props {
  cases: CaseSummaryModel[]
  selectedCaseId: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [caseId: string]
}>()

const searchQuery = ref('')

const filteredCases = computed(() => {
  if (!searchQuery.value) return props.cases
  
  const query = searchQuery.value.toLowerCase()
  return props.cases.filter(c => 
    c.caseNumber.toLowerCase().includes(query) ||
    c.courtName.toLowerCase().includes(query) ||
    c.plaintiff.toLowerCase().includes(query) ||
    c.defendant.toLowerCase().includes(query) ||
    c.caseCause.toLowerCase().includes(query)
  )
})

const handleSelect = (caseId: string) => {
  emit('select', caseId)
}
</script>

<template>
  <div class="space-y-4">
    <!-- Search Input -->
    <div class="relative">
      <SafeIcon 
        name="Search" 
        :size="16" 
        class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
      />
      <Input 
        v-model="searchQuery"
        placeholder="搜索案号、法院、当事人..."
        class="pl-9"
      />
    </div>

    <!-- Cases List -->
    <ScrollArea class="h-[400px] border rounded-lg p-4">
      <div class="space-y-2">
        <div 
          v-for="caseItem in filteredCases" 
          :key="caseItem.id"
          class="p-3 border rounded-lg cursor-pointer transition-all hover:bg-accent"
          :class="selectedCaseId === caseItem.id ? 'bg-primary/10 border-primary' : ''"
          @click="handleSelect(caseItem.id)"
        >
          <div class="flex items-start justify-between gap-2">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <p class="font-semibold text-sm truncate">{{ caseItem.caseNumber }}</p>
                <Badge variant="outline" class="text-xs shrink-0">
                  {{ caseItem.caseStage }}
                </Badge>
              </div>
              <p class="text-xs text-muted-foreground mb-2">{{ caseItem.courtName }}</p>
              <div class="grid grid-cols-2 gap-2 text-xs">
                <p><span class="text-muted-foreground">原告：</span>{{ caseItem.plaintiff }}</p>
                <p><span class="text-muted-foreground">被告：</span>{{ caseItem.defendant }}</p>
              </div>
              <p class="text-xs text-muted-foreground mt-1">
                <span class="text-muted-foreground">主办律师：</span>{{ caseItem.leadAttorneyName }}
              </p>
            </div>
            <div 
              class="flex h-5 w-5 items-center justify-center rounded-full border shrink-0 mt-1"
              :class="selectedCaseId === caseItem.id ? 'bg-primary border-primary' : 'border-muted-foreground'"
            >
              <SafeIcon 
                v-if="selectedCaseId === caseItem.id"
                name="Check" 
                :size="14" 
                color="white"
              />
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="filteredCases.length === 0" class="flex flex-col items-center justify-center py-8 text-center">
          <SafeIcon name="FolderOpen" :size="32" class="text-muted-foreground mb-2" />
          <p class="text-sm text-muted-foreground">未找到匹配的案件</p>
        </div>
      </div>
    </ScrollArea>

    <!-- Info -->
    <p class="text-xs text-muted-foreground">
      共 {{ filteredCases.length }} 个案件
    </p>
  </div>
</template>
