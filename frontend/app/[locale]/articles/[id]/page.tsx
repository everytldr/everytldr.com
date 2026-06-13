import { ArticleDetailPage, fetchArticleDetail } from "@/pages/article-detail";
import { buildOgImageUrl } from "@/shared/lib";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

type PageProps = {
  params: Promise<{ id: string }>;
};

const ARTICLE_ID_PLACEHOLDER = "__placeholder__";

export function generateStaticParams() {
  return [{ id: ARTICLE_ID_PLACEHOLDER }];
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { id } = await params;

  if (id === ARTICLE_ID_PLACEHOLDER) {
    return {};
  }

  const article = await fetchArticleDetail(id);

  return {
    title: article.title,
    description: article.summary,
    openGraph: {
      title: article.title,
      description: article.summary,
      images: article.thumbnailUrl ? [buildOgImageUrl(article.thumbnailUrl)] : undefined,
    },
  };
}

export default async function Page({ params }: PageProps) {
  const { id } = await params;

  if (id === ARTICLE_ID_PLACEHOLDER) {
    notFound();
  }

  return <ArticleDetailPage articleId={id} />;
}
