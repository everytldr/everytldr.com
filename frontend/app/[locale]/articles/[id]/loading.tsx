import {
  ArticleCommentsSkeleton,
  ArticleLikeButtonSkeleton,
  RelatedArticlesSkeleton,
} from "@/pages/article-detail";
import { HIDE_ADSENSE } from "@/shared/config";
import { Skeleton } from "@/shared/ui";

export default function Loading() {
  return (
    <article className="space-y-xl">
      <div className="space-y-lg">
        <header className="space-y-sm">
          <Skeleton className="h-4 w-40" />
          <div className="space-y-sm">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-3/4" />
          </div>
        </header>

        <div className="space-y-sm">
          <Skeleton className="h-5 w-full" />
          <Skeleton className="h-5 w-full" />
          <Skeleton className="h-5 w-5/6" />
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-sm">
        <ArticleLikeButtonSkeleton />
        <Skeleton className="w-28 text-body-sm">&nbsp;</Skeleton>
      </div>

      <RelatedArticlesSkeleton className="border-t border-hairline-soft pt-lg" />

      {!HIDE_ADSENSE && <Skeleton className="min-h-24 w-full" />}

      <ArticleCommentsSkeleton className="border-t border-hairline-soft pt-lg" />
    </article>
  );
}
