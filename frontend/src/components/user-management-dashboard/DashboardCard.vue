
<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface CardData {
  id: string
  title: string
  description: string
  icon: string
  actionLabel: string
  actionHref: string
  stats: {
    label: string
    value: string
  }
}

interface Props {
  card: CardData
}

defineProps<Props>()
</script>

<template>
  <Card class="flex flex-col hover:shadow-card transition-shadow">
    <CardHeader>
      <div class="flex items-start justify-between">
        <div class="flex-1">
          <CardTitle class="text-base">{{ card.title }}</CardTitle>
          <CardDescription class="mt-2">{{ card.description }}</CardDescription>
        </div>
        <div class="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10 flex-shrink-0">
          <SafeIcon :name="card.icon" :size="24" class="text-primary" />
        </div>
      </div>
    </CardHeader>

    <CardContent class="flex-1 flex flex-col justify-between">
      <!-- Stats -->
      <div class="mb-4 space-y-2">
        <p class="text-sm text-muted-foreground">{{ card.stats.label }}</p>
        <p class="text-2xl font-bold">{{ card.stats.value }}</p>
      </div>

      <!-- Action Button -->
      <Button as="a" :href="card.actionHref" class="w-full">
        {{ card.actionLabel }}
        <SafeIcon name="ArrowRight" :size="16" class="ml-2" />
      </Button>
    </CardContent>
  </Card>
</template>
