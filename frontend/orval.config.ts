import { defineConfig } from "orval";

export default defineConfig({
  api: {
    input: "../docs/openapi.json",
    output: {
      target: "src/shared/api/client.gen.ts",
      mode: "split",
      client: "react-query",
      httpClient: "fetch",
      override: {
        mutator: {
          path: "src/shared/api/fetcher.ts",
          name: "_fetch",
        },
        query: {
          useQuery: true,
          useSuspenseQuery: true,
        },
      },
      clean: false,
    },
    hooks: {
      afterAllFilesWrite: "prettier --write",
    },
  },
});
