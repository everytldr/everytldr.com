import { ArticleDetailPage, fetchArticleDetail } from "@/pages/article-detail";
import { listArticles } from "@/shared/api";
import type { Metadata } from "next";

type PageProps = {
  params: Promise<{ id: string }>;
};

export async function generateStaticParams() {
  const response = await listArticles({ size: 10 });
  if (
    response.status !== 200 ||
    !Array.isArray(response.data.items) ||
    response.data.items.length === 0
  ) {
    throw new Error("Failed to fetch article static params");
  }

  const ids = response.data.items?.map((article) => ({ id: article.id })) ?? [];
  return ids;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { id } = await params;
  const article = await fetchArticleDetail(id);

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
  const { id } = await params;

  return <ArticleDetailPage articleId={id} />;
}
