import { defaultLocale, locales } from "@/shared/i18n/locale";
import { defineRouting } from "next-intl/routing";

export const routing = defineRouting({
  locales,
  defaultLocale,
  localePrefix: "as-needed",
  localeCookie: {
    name: "locale",
  },
});
