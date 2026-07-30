import { ArticleScrollRowSkeleton } from "@/entities/article";
import { BriefingArchiveLinkSkeleton, BriefingDateNavSkeleton } from "@/pages/briefing-detail";
import { Skeleton } from "@/shared/ui";
import { range } from "lodash-es";

const BRIEFING_SOURCES_SKELETON_SIZE = 5;
const BRIEFING_SECTION_SKELETON_SIZE = 3;

export default function Loading() {
  return (
    <article className="space-y-xl">
      <div className="space-y-lg">
        <BriefingArchiveLinkSkeleton />

        <header className="space-y-sm">
          <Skeleton className="w-72 text-display-md">&nbsp;</Skeleton>
          <div className="flex flex-col">
            <Skeleton className="w-full text-display-xl">&nbsp;</Skeleton>
            <Skeleton className="w-3/4 text-display-xl">&nbsp;</Skeleton>
          </div>
        </header>

        <div className="space-y-md">
          <div className="flex flex-col">
            <Skeleton className="w-full text-body-lg">&nbsp;</Skeleton>
            <Skeleton className="w-full text-body-lg">&nbsp;</Skeleton>
            <Skeleton className="w-4/5 text-body-lg">&nbsp;</Skeleton>
          </div>

          {range(BRIEFING_SECTION_SKELETON_SIZE).map((i) => (
            <div key={i} className="space-y-sm pt-md">
              <Skeleton className="w-1/2 text-display-md">&nbsp;</Skeleton>
              <div className="flex flex-col">
                <Skeleton className="w-full text-body-lg">&nbsp;</Skeleton>
                <Skeleton className="w-full text-body-lg">&nbsp;</Skeleton>
                <Skeleton className="w-full text-body-lg">&nbsp;</Skeleton>
                <Skeleton className="w-2/3 text-body-lg">&nbsp;</Skeleton>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="space-y-sm border-t border-hairline-soft pt-lg">
        <Skeleton className="w-48 text-display-md">&nbsp;</Skeleton>
        <ArticleScrollRowSkeleton count={BRIEFING_SOURCES_SKELETON_SIZE} />
      </div>

      <BriefingDateNavSkeleton />
    </article>
  );
}
