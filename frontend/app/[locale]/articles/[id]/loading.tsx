import { ArticleCommentsSkeleton, ArticleLikeButtonSkeleton } from "@/pages/article-detail";
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

      <div className="flex flex-wrap items-center gap-sm border-t border-hairline-soft pt-lg">
        <ArticleLikeButtonSkeleton />
        <Skeleton className="h-11 w-28 rounded-sm" />
      </div>

      <Skeleton className="min-h-24 w-full" />

      <ArticleCommentsSkeleton />
    </article>
  );
}
