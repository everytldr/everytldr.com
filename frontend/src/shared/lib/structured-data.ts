import { OG_IMAGE, SITE_NAME, SITE_URL } from "@/shared/config";
import { getPathname, type Locale } from "@/shared/i18n";

type JsonLd = Record<string, unknown>;

const ORGANIZATION_ID = `${SITE_URL}/#organization`;

const DEFAULT_IMAGE = `${SITE_URL}${OG_IMAGE.url}`;

const ORGANIZATION: JsonLd = {
  "@type": "Organization",
  "@id": ORGANIZATION_ID,
  name: SITE_NAME,
  url: SITE_URL,
  logo: {
    "@type": "ImageObject",
    url: DEFAULT_IMAGE,
    width: OG_IMAGE.width,
    height: OG_IMAGE.height,
  },
};

type NewsArticleJsonLdParams = {
  url: string;
  headline: string;
  description: string;
  datePublished: string;
  isBasedOn?: string;
  image?: string;
};

export function buildNewsArticleJsonLd(params: NewsArticleJsonLdParams): JsonLd {
  return {
    "@context": "https://schema.org",
    "@type": "NewsArticle",
    mainEntityOfPage: { "@type": "WebPage", "@id": params.url },
    headline: params.headline,
    description: params.description,
    image: [params.image ?? DEFAULT_IMAGE],
    datePublished: params.datePublished,
    dateModified: params.datePublished,
    ...(params.isBasedOn && { isBasedOn: params.isBasedOn }),
    author: { "@id": ORGANIZATION_ID },
    publisher: ORGANIZATION,
  };
}

export function buildSiteJsonLd(locale: Locale): JsonLd {
  const localeUrl = `${SITE_URL}${getPathname({ locale, href: "/" })}`;
  const searchUrl = `${SITE_URL}${getPathname({ locale, href: "/search" })}`;

  return {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "WebSite",
        "@id": `${SITE_URL}/#website`,
        url: localeUrl,
        name: SITE_NAME,
        publisher: { "@id": ORGANIZATION_ID },
        potentialAction: {
          "@type": "SearchAction",
          target: {
            "@type": "EntryPoint",
            urlTemplate: `${searchUrl}?q={query}`,
          },
          "query-input": "required name=query",
        },
      },
      ORGANIZATION,
    ],
  };
}

export function serializeJsonLd(data: JsonLd): string {
  return JSON.stringify(data).replace(/</g, "\\u003c");
}
