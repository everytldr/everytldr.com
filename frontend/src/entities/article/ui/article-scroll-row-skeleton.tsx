import { cn } from "@/shared/lib";
import { Skeleton } from "@/shared/ui";
import { range } from "lodash-es";

type ArticleScrollRowSkeletonProps = {
  className?: string;
  count: number;
};

export function ArticleScrollRowSkeleton({ className, count }: ArticleScrollRowSkeletonProps) {
  return (
    <div className={cn("-mx-md pc:-mx-xl", className)}>
      <div className="scrollbar-hidden overflow-x-auto px-md pc:px-xl">
        <ul className="flex items-stretch gap-sm">
          {range(count).map((i) => (
            <li key={i} className="w-56 shrink-0 sm:w-64">
              <div className="flex h-full min-w-0 flex-col gap-2xs rounded-md border border-hairline bg-canvas p-md dark:bg-surface-soft">
                <Skeleton className="w-16 rounded-xs text-micro">&nbsp;</Skeleton>
                <div className="flex flex-col">
                  <Skeleton className="w-full text-title-md">&nbsp;</Skeleton>
                  <Skeleton className="w-full text-title-md">&nbsp;</Skeleton>
                  <Skeleton className="w-2/3 text-title-md">&nbsp;</Skeleton>
                </div>
                <Skeleton className="mt-auto w-1/2 text-caption">&nbsp;</Skeleton>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
