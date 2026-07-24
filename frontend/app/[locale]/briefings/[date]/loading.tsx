import { ArticleScrollRowSkeleton } from "@/entities/article";
import { Container, Skeleton } from "@/shared/ui";

const BRIEFING_SOURCES_SKELETON_SIZE = 5;

export default function Loading() {
  return (
    <main className="py-lg">
      <Container size="sm">
        <article className="space-y-xl">
          <div className="space-y-lg">
            <header className="space-y-sm">
              <Skeleton className="w-72 text-display-md">&nbsp;</Skeleton>
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

          <div className="space-y-sm border-t border-hairline-soft pt-lg">
            <Skeleton className="w-48 text-display-md">&nbsp;</Skeleton>
            <ArticleScrollRowSkeleton count={BRIEFING_SOURCES_SKELETON_SIZE} />
          </div>
        </article>
      </Container>
    </main>
  );
}
