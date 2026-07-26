import { BriefingDetailPage, fetchBriefing } from "@/pages/briefing-detail";
import type { Locale } from "@/shared/i18n";
import { buildPageMetadata, toMetaDescription } from "@/shared/lib";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<{ locale: Locale; date: string }>;
};

const BRIEFING_DATE_PLACEHOLDER = "__placeholder__";
const BRIEFING_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

export function generateStaticParams() {
  return [{ date: BRIEFING_DATE_PLACEHOLDER }];
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale, date } = await params;

  if (!isBriefingDate(date)) {
    return {};
  }

  const briefing = await fetchBriefing(date, locale);

  return buildPageMetadata({
    title: briefing.title,
    description: toMetaDescription(briefing.content),
    locale,
    path: `/briefings/${date}`,
    article: { publishedTime: briefing.date },
  });
}

export default async function Page({ params }: PageProps) {
  const { locale, date } = await params;

  if (!isBriefingDate(date)) {
    notFound();
  }

  return <BriefingDetailPage date={date} locale={locale} />;
}

function isBriefingDate(date: string) {
  return BRIEFING_DATE_PATTERN.test(date);
}
