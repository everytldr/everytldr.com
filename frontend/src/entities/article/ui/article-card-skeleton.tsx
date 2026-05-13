import { cn } from "@/shared/lib";
import { Skeleton } from "@/shared/ui";

type ArticleCardSkeletonProps = {
  className?: string;
};

export function ArticleCardSkeleton({ className }: ArticleCardSkeletonProps) {
  return (
    <div
      className={cn(
        "flex items-start gap-md border-b border-hairline-soft py-md last:border-b-0",
        className,
      )}
    >
      <div className="flex min-w-0 flex-1 flex-col gap-2xs">
        <div className="flex flex-col">
          <Skeleton className="w-full text-display-sm">&nbsp;</Skeleton>
          <Skeleton className="w-2/3 text-display-sm">&nbsp;</Skeleton>
        </div>
        <Skeleton className="w-3/4 text-body-sm">&nbsp;</Skeleton>
        <Skeleton className="w-1/3 text-caption">&nbsp;</Skeleton>
      </div>
      <Skeleton className="size-24 shrink-0 rounded-md" />
    </div>
  );
}
