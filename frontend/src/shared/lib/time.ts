import { Locale } from "@/shared/i18n";

export const A_SECOND = 1_000;
export const A_MINUTE = 60 * A_SECOND;
export const AN_HOUR = 60 * A_MINUTE;
export const A_DAY = 24 * AN_HOUR;

export function formatDate(date: string | Date, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeZone: "UTC",
  }).format(new Date(date));
}

export function formatMonthDay(date: string | Date, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    month: "short",
    day: "numeric",
    timeZone: "UTC",
  }).format(new Date(date));
}

export function formatNumericMonthDay(date: string | Date): string {
  const parsed = new Date(date);
  const month = String(parsed.getUTCMonth() + 1).padStart(2, "0");
  const day = String(parsed.getUTCDate()).padStart(2, "0");

  return `${month}.${day}`;
}

export function formatWeekday(date: string | Date, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    weekday: locale === Locale.Ko ? "long" : "short",
    timeZone: "UTC",
  }).format(new Date(date));
}

export function formatDateWithWeekday(date: string | Date, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "full",
    timeZone: "UTC",
  }).format(new Date(date));
}
