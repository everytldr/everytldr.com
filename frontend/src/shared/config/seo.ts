export const SITE_NAME = "everytldr";

export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://everytldr.com";

export const OG_IMAGE = {
  url: "/og-image.png",
  width: 1200,
  height: 630,
  alt: SITE_NAME,
} as const;
