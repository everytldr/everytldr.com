export const STATIC_PAGE_URLS = {
  about: process.env.NEXT_PUBLIC_FOOTER_ABOUT_URL || "/about",
  privacy: process.env.NEXT_PUBLIC_FOOTER_PRIVACY_URL || "/privacy",
  terms: process.env.NEXT_PUBLIC_FOOTER_TERMS_URL || "/terms",
} as const;
