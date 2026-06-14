import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";
import { defaultLocale } from "./src/shared/i18n/locale";

const withNextIntl = createNextIntlPlugin("./src/shared/i18n/request.ts");

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export default withNextIntl({
  output: "standalone",
  outputFileTracingIncludes: {
    "/*": ["./docs/pages/*.md"],
  },
  cacheComponents: true,
  turbopack: {
    rules: {
      "*.svg": {
        loaders: [
          {
            loader: "@svgr/webpack",
            options: {
              svgoConfig: {
                plugins: [
                  {
                    name: "preset-default",
                    params: { overrides: { removeViewBox: false } },
                  },
                  "prefixIds",
                ],
              },
            },
          },
        ],
        as: "*.js",
      },
    },
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${BACKEND_URL}/api/:path*`,
      },
    ];
  },
  async redirects() {
    return [
      {
        source: "/feed.xml",
        destination: `/${defaultLocale}/feed.xml`,
        permanent: true,
      },
    ];
  },
}) satisfies NextConfig;
