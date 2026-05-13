"use client";

import {
  EPL_BIG_SIX_TEAMS,
  EPL_TEAMS_ALPHABETICAL,
  type EplTeam,
  isBigSixTeam,
} from "@/shared/config";
import { Link, useRouter } from "@/shared/i18n";
import { buildEplFilterUrl, cn } from "@/shared/lib";
import { Chip, ResponsiveSelector, ScrollableRow, Translation } from "@/shared/ui";
import { ChevronDown } from "lucide-react";
import { useTranslations } from "next-intl";
import { EplTeamCrest } from "./epl-team-crest";

type EplTeamFilterProps = {
  className?: string;
  filter?: EplTeam;
};

const ALL_VALUE = "all" as const;
type TeamPickerValue = typeof ALL_VALUE | EplTeam;

export function EplTeamFilter({ className, filter }: EplTeamFilterProps) {
  const t = useTranslations();
  const router = useRouter();

  const isTriggerActive = !!filter && !isBigSixTeam(filter);
  const pickerValue: TeamPickerValue = filter ?? ALL_VALUE;

  return (
    <nav className={cn(className)} aria-label={t("epl.aria-label.team-filter")}>
      <ScrollableRow>
        <div className="flex h-12 items-center gap-2xs">
          <Chip asChild isSelected={!filter}>
            <Link href={buildEplFilterUrl()} aria-current={!filter ? "page" : undefined}>
              <Translation tKey="epl.all-teams" />
            </Link>
          </Chip>

          {EPL_BIG_SIX_TEAMS.map((team) => {
            const isSelected = filter === team;
            return (
              <Chip key={team} asChild isSelected={isSelected}>
                <Link href={buildEplFilterUrl(team)} aria-current={isSelected ? "page" : undefined}>
                  <EplTeamCrest
                    className={cn("not-pc:hidden dark:drop-stroke", isSelected && "drop-stroke")}
                    team={team}
                  />
                  <Translation tKey={`epl.team-short.${team}`} />
                </Link>
              </Chip>
            );
          })}

          <ResponsiveSelector
            className="contents"
            multiple={false}
            value={pickerValue}
            title={t("epl.picker.title")}
            options={[
              { value: ALL_VALUE, content: <Translation tKey="epl.all-teams" /> },
              ...EPL_TEAMS_ALPHABETICAL.map((team) => ({
                value: team,
                content: (
                  <span className="inline-flex items-center gap-sm">
                    <EplTeamCrest className="dark:drop-stroke" team={team} />
                    <Translation tKey={`epl.team.${team}`} />
                  </span>
                ),
              })),
            ]}
            renderMobileTrigger={({ isOpen, open }) => (
              <Chip
                isSelected={isTriggerActive}
                type="button"
                aria-haspopup="dialog"
                aria-label={t("epl.aria-label.team-picker")}
                onClick={open}
              >
                {isTriggerActive ? (
                  <Translation tKey={`epl.team.${filter}`} />
                ) : (
                  <Translation tKey="epl.more-teams" />
                )}
                <ChevronDown
                  className={cn("size-4 transition-transform", isOpen && "rotate-180")}
                  aria-hidden
                />
              </Chip>
            )}
            renderDesktopTrigger={({ isOpen }) => (
              <Chip
                isSelected={isTriggerActive}
                type="button"
                aria-label={t("epl.aria-label.team-picker")}
              >
                {isTriggerActive ? (
                  <Translation tKey={`epl.team.${filter}`} />
                ) : (
                  <Translation tKey="epl.more-teams" />
                )}
                <ChevronDown
                  className={cn("size-4 transition-transform", isOpen && "rotate-180")}
                  aria-hidden
                />
              </Chip>
            )}
            onChange={handleSelect}
          />
        </div>
      </ScrollableRow>
    </nav>
  );

  function handleSelect(next: TeamPickerValue) {
    router.push(next === ALL_VALUE ? buildEplFilterUrl() : buildEplFilterUrl(next));
  }
}
