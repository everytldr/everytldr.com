import { listArticleComments } from "@/shared/api";
import type { Locale } from "@/shared/i18n";
import { assert, cn } from "@/shared/lib";
import { Skeleton, Translation } from "@/shared/ui";
import { CommentList } from "./comment-list";

type ArticleCommentsProps = {
  className?: string;
  articleId: string;
  locale: Locale;
};

export async function ArticleComments({ className, articleId, locale }: ArticleCommentsProps) {
  const response = await listArticleComments(articleId);
  assert(response.status === 200, "Failed to load article comments");

  const comments = response.data.items ?? [];

  return (
    <section className={cn("space-y-md border-t border-hairline-soft pt-lg", className)}>
      <Translation className="text-display-sm text-ink" as="h2" tKey="article-detail.comments" />

      <CommentList articleId={articleId} comments={comments} locale={locale} />
    </section>
  );
}

type ArticleCommentsErrorProps = {
  className?: string;
};

export function ArticleCommentsError({ className }: ArticleCommentsErrorProps) {
  return (
    <section className={cn("space-y-md border-t border-hairline-soft pt-lg", className)}>
      <Translation className="text-display-sm text-ink" as="h2" tKey="article-detail.comments" />
      <Translation
        className="rounded-md border border-hairline-soft bg-surface-soft p-2xl text-center text-body-md text-meta"
        as="p"
        tKey="article-detail.comments-error"
      />
    </section>
  );
}

type ArticleCommentsSkeletonProps = {
  className?: string;
};

export function ArticleCommentsSkeleton({ className }: ArticleCommentsSkeletonProps) {
  return (
    <section className={cn("space-y-md border-t border-hairline-soft pt-lg", className)}>
      <Skeleton className="h-7 w-24" />
      <div className="space-y-sm">
        <Skeleton className="h-24 w-full rounded-md" />
        <Skeleton className="h-5 w-16" />
        <Skeleton className="ml-md h-20 w-[calc(100%-(var(--spacing-md)))] rounded-md" />
      </div>
    </section>
  );
}
