import { Skeleton } from "@/shared/ui";

export default function Loading() {
  return (
    <div className="flex flex-col gap-2xl">
      <Skeleton className="h-12 w-full rounded-md" />
      <Skeleton className="h-32 w-full rounded-md" />
    </div>
  );
}
