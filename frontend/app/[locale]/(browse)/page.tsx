import { HomePage } from "@/pages/home";
import { locales } from "@/shared/i18n";

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export default function Page() {
  return <HomePage />;
}
