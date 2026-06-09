import { ArticleDetailPage, fetchArticleDetail } from "@/pages/article-detail";
import type { Metadata } from "next";

type PageProps = {
  params: Promise<{ id: string }>;
};

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
