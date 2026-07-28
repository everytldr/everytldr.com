import { ArticleDetailPage, fetchArticleDetail } from "@/pages/article-detail";
import type { Locale } from "@/shared/i18n";
import { buildPageMetadata, markdownToPlainText } from "@/shared/lib";
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

  return buildPageMetadata({
    title: article.title,
    description: markdownToPlainText(article.summary),
    locale,
    path: `/articles/${id}`,
    article: { publishedTime: article.publishedAt },
  });
}

export default async function Page({ params }: PageProps) {
  const { locale, id } = await params;

  if (id === ARTICLE_ID_PLACEHOLDER) {
    notFound();
  }

  return <ArticleDetailPage articleId={id} locale={locale} />;
}
