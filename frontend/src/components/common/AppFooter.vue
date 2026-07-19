
<script setup lang="ts">
import { ref, onMounted } from 'vue'

const currentYear = ref(2024)

onMounted(() => {
  if (typeof window !== 'undefined') {
    currentYear.value = new Date().getFullYear()
  }
})

const footerLinks = [
  { name: '关于我们', href: '#about' },
  { name: '隐私政策', href: '#privacy' },
  { name: '服务条款', href: '#terms' },
  { name: '联系我们', href: '#contact' },
]
</script>

<template>
  <footer class="border-t bg-background mt-auto">
    <div class="container mx-auto px-4 py-6">
      <div class="flex flex-col md:flex-row justify-between items-center gap-4">
        <!-- Links -->
        <div class="flex flex-wrap justify-center md:justify-start gap-x-6 gap-y-2">
          <a
            v-for="link in footerLinks"
            :key="link.name"
            :href="link.href"
            class="text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            {{ link.name }}
          </a>
        </div>

        <!-- Copyright -->
        <p class="text-sm text-muted-foreground whitespace-nowrap">
          © {{ currentYear }} 诉讼案件管理系统 版权所有
        </p>
      </div>
    </div>
  </footer>
</template>
