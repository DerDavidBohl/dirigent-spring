// @ts-check
const eslint = require('@eslint/js');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = tseslint.config(
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': ['error', {type: 'attribute', prefix: 'app', style: 'camelCase'}],
      '@angular-eslint/component-selector': ['error', {type: 'element', prefix: 'app', style: 'kebab-case'}],
      '@angular-eslint/prefer-standalone': 'error',
      '@angular-eslint/prefer-on-push-component-change-detection': 'error',
      // COD-005: Tailwind is the only styling mechanism.
      'no-restricted-syntax': [
        'error',
        {
          selector: "Decorator[expression.callee.name='Component'] Property[key.name=/^(styles|styleUrl|styleUrls)$/]",
          message: 'Component styles are not allowed; use Tailwind utility classes (COD-005).',
        },
        {
          selector: "ImportDeclaration[source.value=/^@angular\\/material/]",
          message: 'Angular Material is not allowed; use Tailwind and CDK primitives (COD-005).',
        },
      ],
    },
  },
  {
    files: ['**/*.html'],
    extends: [...angular.configs.templateRecommended, ...angular.configs.templateAccessibility],
    rules: {},
  },
);
