/// <reference types="vitest/config" />

import { getViteConfig } from 'astro/config'

export default getViteConfig({
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json', 'lcov'],
      include: [
        'src/api/**/*.ts',
        'src/services/**/*.ts',
        'src/components/case-list-view/**/*.vue',
        'src/components/case-detail-view/CaseDetailDocuments.vue',
        'src/components/document-generate-selection/**/*.vue',
      ],
    },
  },
})
