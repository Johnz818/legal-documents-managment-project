
<script setup lang="ts">
import { SidebarProvider, SidebarInset } from '@/components/ui/sidebar'
import AppSidebar from '@/components/common/AppSidebar.vue'

defineProps<{
  headerHeight?: string
}>()
</script>

<template>
  <SidebarProvider 
    :style="{
      '--header-height': headerHeight || '64px'
    }"
  >
    <AppSidebar />
    <SidebarInset class="flex flex-col min-h-[calc(100vh-var(--header-height))]">
      <slot />
    </SidebarInset>
  </SidebarProvider>
</template>
