import { StaticPage, readStaticPage } from "@/pages/static-page";
import { type Locale, locales } from "@/shared/i18n";
import { buildPageMetadata } from "@/shared/lib";
import type { Metadata } from "next";

type PageProps = {
  params: Promise<{ locale: Locale }>;
};

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale } = await params;
  const content = readStaticPage("about", locale);

  return buildPageMetadata({
    title: `${content.title} · everytldr`,
    description: content.description,
    locale,
    path: "/about",
  });
}

export default async function Page({ params }: PageProps) {
  const { locale } = await params;
  const content = readStaticPage("about", locale);

  return <StaticPage content={content} />;
}
