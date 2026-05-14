import { ArticleListInfinite } from "@/entities/article";
import { getListArticlesSuspenseInfiniteQueryOptions, getQueryClient } from "@/shared/api";
import { type EplTeam } from "@/shared/config";
import { cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
import { EplTeamFilter } from "./epl-team-filter";

type EplNewsTabProps = {
  className?: string;
  filter?: EplTeam;
};

export async function EplNewsTab({ className, filter }: EplNewsTabProps) {
  const categoryPrefix = filter ? `sport-football-epl-${filter}` : "sport-football-epl";

  const queryClient = getQueryClient();
  await queryClient.prefetchInfiniteQuery(
    getListArticlesSuspenseInfiniteQueryOptions({ categoryPrefix }),
  );

  return (
    <div className={cn("space-y-sm", className)}>
      <EplTeamFilter filter={filter} />
      <section>
        <HydrationBoundary state={dehydrate(queryClient)}>
          <ArticleListInfinite
            categoryPrefix={categoryPrefix}
            empty={
              <Translation
                className="rounded-md border border-hairline-soft bg-surface-soft px-2xl py-2xl text-center text-body-md text-meta"
                as="p"
                tKey="epl.news.empty-state"
              />
            }
          />
        </HydrationBoundary>
      </section>
    </div>
  );
}
