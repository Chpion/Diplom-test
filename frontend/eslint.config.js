// .eslint.config.js
import js from '@eslint/js'
import vue from 'eslint-plugin-vue'
import prettier from '@vue/eslint-config-prettier'

export default [
  js.configs.recommended,
  ...vue.configs['flat/recommended'],
  prettier,
  {
    rules: {
      'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      'no-empty-pattern': 'error',
      'vue/no-parsing-error': 'error',
      'vue/attributes-order': 'error',
      'no-console': 'off', // Отключить предупреждения о console.log
      'vue/no-unused-vars': 'warn',
    },
  },
]
