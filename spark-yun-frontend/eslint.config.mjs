import js from '@eslint/js'
import prettier from 'eslint-config-prettier'
import vue from 'eslint-plugin-vue'
import globals from 'globals'
import tseslint from 'typescript-eslint'

export default tseslint.config(
    {
        ignores: [
            'dist/**',
            'coverage/**',
            'playwright-report/**',
            'test-results/**',
            'node_modules/**',
            'components.d.ts'
        ]
    },
    js.configs.recommended,
    ...tseslint.configs.recommended,
    ...vue.configs['flat/recommended'],
    prettier,
    {
        files: ['**/*.{ts,tsx,vue}'],
        languageOptions: {
            ecmaVersion: 'latest',
            sourceType: 'module',
            parserOptions: {
                parser: tseslint.parser,
                extraFileExtensions: ['.vue']
            },
            globals: {
                ...globals.browser,
                ...globals.es2025,
                __APP_VERSION__: 'readonly'
            }
        },
        rules: {
            'no-redeclare': 'off',
            'no-extra-boolean-cast': 'off',
            'no-undef': 'off',
            'no-unused-expressions': 'off',
            'no-useless-assignment': 'off',
            'preserve-caught-error': 'off',
            '@typescript-eslint/ban-ts-comment': 'off',
            '@typescript-eslint/no-explicit-any': 'off',
            '@typescript-eslint/no-namespace': 'off',
            '@typescript-eslint/no-redeclare': 'off',
            '@typescript-eslint/no-unsafe-function-type': 'off',
            '@typescript-eslint/no-unused-expressions': 'off',
            '@typescript-eslint/no-unused-vars': 'off',
            'vue/attributes-order': 'off',
            'vue/html-indent': 'off',
            'vue/html-self-closing': 'off',
            'vue/max-attributes-per-line': 'off',
            'vue/multi-word-component-names': 'off',
            'vue/no-mutating-props': 'off',
            'vue/no-template-shadow': 'off',
            'vue/no-v-html': 'off'
        }
    },
    {
        files: ['*.config.{js,mjs,ts}', 'orval.config.ts'],
        languageOptions: {
            globals: {
                ...globals.node
            }
        }
    },
    {
        files: ['tests/**/*.ts', 'e2e/**/*.ts'],
        rules: {
            '@typescript-eslint/no-empty-function': 'off'
        }
    }
)
