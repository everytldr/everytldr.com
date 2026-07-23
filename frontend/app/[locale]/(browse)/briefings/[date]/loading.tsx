import { ArticleCardSkeleton } from "@/entities/article";
import { Container, Skeleton } from "@/shared/ui";
import { range } from "lodash-es";

export default function Loading() {
  return (
    <main className="py-lg">
      <Container size="sm">
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

          <div className="space-y-sm border-t border-hairline-soft pt-lg">
            <Skeleton className="w-48 text-display-md">&nbsp;</Skeleton>
            <ul>
              {range(3).map((i) => (
                <li key={i}>
                  <ArticleCardSkeleton />
                </li>
              ))}
            </ul>
          </div>
        </article>
      </Container>
    </main>
  );
}
