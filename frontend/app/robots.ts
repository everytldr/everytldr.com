import { BLOCKED_CATEGORY_SLUGS, SITE_URL } from "@/shared/config";
import { locales } from "@/shared/i18n";
import type { MetadataRoute } from "next";

export default function robots(): MetadataRoute.Robots {
  const disallow = locales.flatMap((locale) =>
    BLOCKED_CATEGORY_SLUGS.map((slug) => `/${locale}/${slug}`),
  );

  return {
    rules: {
      userAgent: "*",
      allow: "/",
      ...(disallow.length > 0 && { disallow }),
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
    host: SITE_URL,
  };
}
