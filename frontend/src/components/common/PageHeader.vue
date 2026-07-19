
<script setup lang="ts">
import {
  Breadcrumb,
  BreadcrumbList,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb'

interface BreadcrumbItem {
  label: string
  href?: string
}

interface Props {
  title: string
  description?: string
  breadcrumbs?: BreadcrumbItem[]
}

defineProps<Props>()
</script>

<template>
  <header class="flex h-16 shrink-0 items-center gap-2 border-b px-4">
    <div class="flex flex-1 items-center justify-between">
      <div class="flex flex-col gap-1">
        <Breadcrumb v-if="breadcrumbs && breadcrumbs.length > 0">
          <BreadcrumbList>
            <template v-for="(item, index) in breadcrumbs" :key="index">
              <BreadcrumbItem>
                <BreadcrumbLink v-if="item.href" :href="item.href">
                  {{ item.label }}
                </BreadcrumbLink>
                <BreadcrumbPage v-else>
                  {{ item.label }}
                </BreadcrumbPage>
              </BreadcrumbItem>
              <BreadcrumbSeparator v-if="index < breadcrumbs.length - 1" />
            </template>
          </BreadcrumbList>
        </Breadcrumb>
        <div>
          <h1 class="text-lg font-semibold">{{ title }}</h1>
          <p v-if="description" class="text-sm text-muted-foreground">{{ description }}</p>
        </div>
      </div>
      
      <div class="flex items-center gap-2">
        <slot name="actions" />
      </div>
    </div>
  </header>
</template>
