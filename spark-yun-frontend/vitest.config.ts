import { mergeConfig, defineConfig } from 'vitest/config'

import viteConfig from './vite.config'

export default mergeConfig(
    viteConfig,
    defineConfig({
        test: {
            globals: true,
            environment: 'jsdom',
            setupFiles: ['tests/setup.ts'],
            include: ['src/**/*.{test,spec}.{ts,tsx}', 'tests/unit/**/*.{test,spec}.{ts,tsx}'],
            coverage: {
                provider: 'v8',
                reportsDirectory: 'coverage',
                reporter: ['text', 'json', 'html'],
                exclude: [
                    'dist/**',
                    'coverage/**',
                    'e2e/**',
                    'tests/**',
                    'src/**/*.d.ts',
                    'src/main.ts',
                    'src/app/api/**',
                    'src/app/type/**'
                ]
            }
        }
    })
)
