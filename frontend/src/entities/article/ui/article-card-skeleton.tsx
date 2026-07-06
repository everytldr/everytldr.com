import { cn } from "@/shared/lib";
import { Skeleton } from "@/shared/ui";

type ArticleCardSkeletonProps = {
  className?: string;
};

export function ArticleCardSkeleton({ className }: ArticleCardSkeletonProps) {
  return (
    <div
      className={cn(
        "flex min-w-0 flex-col gap-2xs border-b border-hairline-soft py-md last:border-b-0",
        className,
      )}
    >
      <Skeleton className="w-16 text-micro">&nbsp;</Skeleton>
      <Skeleton className="w-full text-display-sm">&nbsp;</Skeleton>
      <div className="flex flex-col">
        <Skeleton className="w-full text-body-sm">&nbsp;</Skeleton>
        <Skeleton className="w-3/4 text-body-sm">&nbsp;</Skeleton>
      </div>
      <Skeleton className="w-1/3 text-caption">&nbsp;</Skeleton>
    </div>
  );
}
