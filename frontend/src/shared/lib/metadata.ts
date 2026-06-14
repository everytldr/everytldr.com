import { OG_IMAGE, SITE_NAME } from "@/shared/config";
import { getPathname, type Locale, locales } from "@/shared/i18n";
import type { Metadata } from "next";

type OgImage = {
  url: string;
  width?: number;
  height?: number;
  alt?: string;
};

type BuildPageMetadataParams = {
  title?: string;
  description: string;
  locale: Locale;
  path?: string;
  images?: OgImage[];
  article?: { publishedTime: string };
};

export function buildPageMetadata({
  title,
  description,
  locale,
  path,
  images,
  article,
}: BuildPageMetadataParams): Metadata {
  const canonical = path === undefined ? undefined : getPathname({ locale, href: path });
  const resolvedImages = images ?? [OG_IMAGE];
  const base = {
    siteName: SITE_NAME,
    title,
    description,
    url: canonical,
    locale,
    images: resolvedImages,
  };

  return {
    title,
    description,
    alternates:
      path === undefined
        ? undefined
        : {
            canonical,
            languages: Object.fromEntries(
              locales.map((value) => [value, getPathname({ locale: value, href: path })]),
            ),
          },
    openGraph: article
      ? { type: "article", publishedTime: article.publishedTime, ...base }
      : { type: "website", ...base },
    twitter: {
      card: "summary_large_image",
      title,
      description,
      images: resolvedImages.map((image) => image.url),
    },
  };
}
