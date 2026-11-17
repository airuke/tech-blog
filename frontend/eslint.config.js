import vue from 'eslint-plugin-vue'
import globals from 'globals'
export default [
  { ignores: ['dist/**', 'node_modules/**'] },
  ...vue.configs['flat/recommended'],
  {
    files: ['src/**/*.{vue,js}'],
    languageOptions: { globals: { ...globals.browser }, ecmaVersion: 'latest' },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/multiline-html-element-content-newline': 'off',
      'vue/html-self-closing': 'off',
      'vue/html-indent': 'off',
      'vue/html-closing-bracket-newline': 'off',
      'vue/first-attribute-linebreak': 'off',
      'vue/attributes-order': 'off',
      'vue/html-closing-bracket-spacing': 'off',
      'vue/no-v-html': 'off',
      'vue/no-multi-spaces': 'off',
      'no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', caughtErrorsIgnorePattern: '^_' },
      ],
      'no-undef': 'error',
    },
  },
]
