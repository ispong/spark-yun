import globals from 'globals'
import pluginVue from 'eslint-plugin-vue'
import { configureVueProject, defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

const isProduction = process.env.NODE_ENV === 'production' || process.env.VITE_NODE_ENV === 'production'

configureVueProject({
    scriptLangs: ['ts', 'js', 'tsx', 'jsx'],
    rootDir: import.meta.dirname
})

export default defineConfigWithVueTs(
    {
        name: 'spark-yun/files',
        files: ['**/*.{vue,js,jsx,cjs,mjs,ts,tsx,cts,mts}']
    },
    {
        name: 'spark-yun/ignores',
        ignores: [
            'dist/**',
            'node_modules/**',
            'playwright-report/**',
            'test-results/**',
            'coverage/**',
            'components.d.ts'
        ]
    },
    pluginVue.configs['flat/recommended'],
    vueTsConfigs.recommended,
    {
        name: 'spark-yun/rules',
        languageOptions: {
            ecmaVersion: 2020,
            sourceType: 'module',
            globals: {
                ...globals.browser,
                ...globals.node
            }
        },
        rules: {
            'vue/no-template-shadow': 'off',
            'vue/no-mutating-props': [
                'error',
                {
                    shallowOnly: true
                }
            ],
            'no-console': isProduction ? 'warn' : 'off',
            'no-debugger': isProduction ? 'warn' : 'off',
            indent: 'off',
            quotes: ['warn', 'single'],
            semi: ['warn', 'never'],
            'comma-dangle': ['warn', 'never'],
            'space-before-function-paren': ['warn', 'never'],
            'func-call-spacing': ['warn', 'never'],
            'space-before-blocks': ['warn', 'always'],
            'block-spacing': ['warn', 'always'],
            'key-spacing': [
                'warn',
                {
                    beforeColon: false,
                    afterColon: true
                }
            ],
            'comma-spacing': [
                'warn',
                {
                    before: false,
                    after: true
                }
            ],
            'semi-spacing': 'warn',
            'array-bracket-spacing': ['warn', 'always'],
            'space-in-parens': ['warn', 'never'],
            'object-curly-spacing': ['warn', 'always'],
            'keyword-spacing': [
                'warn',
                {
                    before: true,
                    after: true
                }
            ],
            'space-infix-ops': 'warn',
            'spaced-comment': ['warn', 'always'],
            'lines-around-comment': [
                'warn',
                {
                    beforeBlockComment: true
                }
            ],
            'eol-last': ['warn', 'always'],
            'object-curly-newline': [
                'warn',
                {
                    ObjectExpression: 'always',
                    ObjectPattern: 'always',
                    ImportDeclaration: 'never',
                    ExportDeclaration: 'always'
                }
            ],
            'one-var-declaration-per-line': ['warn', 'always'],
            curly: 'warn',
            'no-unused-vars': 'off',
            '@typescript-eslint/no-unused-vars': 'off',
            '@typescript-eslint/no-explicit-any': 'off',
            '@typescript-eslint/no-unused-expressions': 'off',
            '@typescript-eslint/no-unsafe-function-type': 'off',
            '@typescript-eslint/ban-ts-comment': 'off',
            'prefer-const': 'warn',
            'no-empty': 'warn',
            'vue/comment-directive': 'off',
            'vue/multi-word-component-names': 'off'
        }
    },
    skipFormatting
)
