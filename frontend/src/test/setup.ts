import { afterEach, vi } from 'vitest'
import { config } from '@vue/test-utils'

config.global.renderStubDefaultSlot = true

afterEach(() => {
  vi.clearAllMocks()
  vi.unstubAllGlobals()
})
