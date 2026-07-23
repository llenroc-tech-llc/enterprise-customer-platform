import tseslint from 'typescript-eslint';
import angular from '@angular-eslint/eslint-plugin';

export default tseslint.config(
  { ignores: ['dist/**', 'node_modules/**', 'android/**', 'ios/**'] },
  ...tseslint.configs.recommended,
  {
    files: ['src/**/*.ts'],
    plugins: { '@angular-eslint': angular },
    rules: {
      '@angular-eslint/prefer-standalone': 'error',
      '@typescript-eslint/no-explicit-any': 'error',
    },
  },
);
