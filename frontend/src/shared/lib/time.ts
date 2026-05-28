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
