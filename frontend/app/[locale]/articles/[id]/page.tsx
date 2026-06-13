import { ArticleDetailPage, fetchArticleDetail } from "@/pages/article-detail";
import type { Locale } from "@/shared/i18n";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<{ locale: Locale; id: string }>;
};

const ARTICLE_ID_PLACEHOLDER = "__placeholder__";

export function generateStaticParams() {
  return [{ id: ARTICLE_ID_PLACEHOLDER }];
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale, id } = await params;

  if (id === ARTICLE_ID_PLACEHOLDER) {
    return {};
  }

  const article = await fetchArticleDetail(id, locale);

  return {
    title: article.title,
    description: article.summary,
    openGraph: {
      title: article.title,
      description: article.summary,
      images: article.thumbnailUrl ? [article.thumbnailUrl] : undefined,
    },
  };
}

export default async function Page({ params }: PageProps) {
  const { locale, id } = await params;

  if (id === ARTICLE_ID_PLACEHOLDER) {
    notFound();
  }

  return <ArticleDetailPage articleId={id} locale={locale} />;
}
