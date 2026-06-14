export const SITE_NAME = "everytldr";

export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || "https://everytldr.com";

export const OG_IMAGE = {
  url: "/og-image.png",
  width: 1200,
  height: 630,
  alt: SITE_NAME,
} as const;

export const SITE_VERIFICATION = {
  bing: process.env.NEXT_PUBLIC_BING_SITE_VERIFICATION || "8DC3AF348043B85F0C201E607926C701",
  naver:
    process.env.NEXT_PUBLIC_NAVER_SITE_VERIFICATION || "c926cbc6f79ac4a97583a1861250bbb8875b46fb",
} as const;
