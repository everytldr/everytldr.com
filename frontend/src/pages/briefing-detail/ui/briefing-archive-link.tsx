import { Link } from "@/shared/i18n";
import { buildBriefingArchiveUrl, cn } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { ChevronLeft } from "lucide-react";

type BriefingArchiveLinkProps = {
  className?: string;
};

export function BriefingArchiveLink({ className }: BriefingArchiveLinkProps) {
  return (
    <Link
      className={cn("group inline-flex items-center gap-xs outline-none", className)}
      href={buildBriefingArchiveUrl()}
      prefetch={false}
    >
      <span className="inline-flex size-9 items-center justify-center rounded-full bg-surface-soft p-xs text-ink transition-colors group-hover:bg-surface-strong group-focus-visible:ring-2 group-focus-visible:ring-primary group-active:bg-surface-pressed">
        <ChevronLeft className="pointer-events-none size-4.5" aria-hidden="true" />
      </span>
      <Translation
        className="text-body-sm text-meta transition-colors group-hover:text-primary"
        tKey="briefings.back-to-archive"
      />
    </Link>
  );
}
