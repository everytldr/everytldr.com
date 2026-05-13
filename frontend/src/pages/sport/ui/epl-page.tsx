import { EplPageTab, type EplTeam } from "@/shared/config";
import { cn } from "@/shared/lib";
import { Container } from "@/shared/ui";
import { EplTabs } from "./epl-tabs";
import { EplTeamFilter } from "./epl-team-filter";

type EplPageProps = {
  className?: string;
  subSlug?: EplPageTab | EplTeam;
};

type ResolvedSubSlug =
  | { activeTab: EplPageTab.News; filter?: EplTeam }
  | { activeTab: EplPageTab.Record };

export function EplPage({ className, subSlug }: EplPageProps) {
  const resolved = resolveSubSlug(subSlug);

  return (
    <main className={cn("pt-lg", className)}>
      <Container className="space-y-sm">
        <EplTabs activeTab={resolved.activeTab} />

        {resolved.activeTab === EplPageTab.News ? (
          <EplTeamFilter filter={resolved.filter} />
        ) : (
          <p>Record</p>
        )}
      </Container>
    </main>
  );
}

function resolveSubSlug(subSlug?: EplPageTab | EplTeam): ResolvedSubSlug {
  if (subSlug === EplPageTab.Record) {
    return { activeTab: EplPageTab.Record };
  }
  if (subSlug === EplPageTab.News) {
    return { activeTab: EplPageTab.News };
  }

  return { activeTab: EplPageTab.News, filter: subSlug };
}
