import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import eslintPluginPrettierRecommended from "eslint-plugin-prettier/recommended";
import { defineConfig, globalIgnores } from "eslint/config";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  eslintPluginPrettierRecommended,
  {
    rules: {
      curly: ["error", "all"],
      "@typescript-eslint/consistent-type-imports": [
        "error",
        {
          prefer: "type-imports",
          fixStyle: "inline-type-imports",
          disallowTypeAnnotations: true,
        },
      ],
      "no-restricted-syntax": [
        "error",
        {
          selector: "ExportNamedDeclaration[source=null][declaration=null]",
          message:
            "Place 'export' inline on the declaration (export function/const/...) instead of a trailing 'export { ... }'. Re-exports with 'from' are allowed.",
        },
      ],
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: [
                "@/app/*/**",
                "@/pages/*/**",
                "@/widgets/*/**",
                "@/features/*/**",
                "@/entities/*/**",
                "@/shared/*/**",
              ],
              message:
                "Bypasses the public API. Import from '@/<layer>/<slice|segment>' (its index.ts). For same-segment files, use a relative path like './file' to avoid a circular import through index.ts.",
            },
            {
              group: ["../../**"],
              message:
                "Cross-slice/layer relative import bypasses the public API. Use '@/<layer>/<slice|segment>' instead.",
            },
            {
              group: ["**/api/fetcher", "**/api/fetcher.*"],
              importNames: ["_fetch"],
              message:
                "_fetch is the orval mutator. Do not call it directly — use the generated API client under '@/shared/api/generated/...'.",
            },
          ],
        },
      ],
    },
  },
  globalIgnores([
    // INFO: Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    "**/*.gen.ts",
    "**/*.gen.schemas.ts",
  ]),
]);

export default eslintConfig;
