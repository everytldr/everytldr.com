export enum Locale {
  En = "en",
  Ko = "ko",
}

export const locales = Object.values(Locale);
export const defaultLocale = Locale.En;

export function isLocale(value: string): value is Locale {
  switch (value) {
    case Locale.En:
    case Locale.Ko:
      return true;
    default:
      return false;
  }
}
