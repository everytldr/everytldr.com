"use client";

import {
  EPL_BIG_SIX_TEAMS,
  EPL_TEAMS_ALPHABETICAL,
  type EplTeam,
  isBigSixTeam,
} from "@/shared/config";
import { Link, useRouter } from "@/shared/i18n";
import { buildEplFilterUrl, cn } from "@/shared/lib";
import { Chip, ResponsiveSelector, Translation } from "@/shared/ui";
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
    <nav
      className={cn("scrollbar-hidden overflow-x-auto", className)}
      aria-label={t("epl.aria-label.team-filter")}
    >
      <div className="flex h-12 items-center gap-2xs">
        <Chip asChild isActive={!filter}>
          <Link href={buildEplFilterUrl()} aria-current={!filter ? "page" : undefined}>
            <Translation tKey="epl.all-teams" />
          </Link>
        </Chip>

        {EPL_BIG_SIX_TEAMS.map((team) => {
          const isActive = filter === team;
          return (
            <Chip key={team} asChild isActive={isActive}>
              <Link href={buildEplFilterUrl(team)} aria-current={isActive ? "page" : undefined}>
                <EplTeamCrest className="hidden md:inline-block" team={team} />
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
                <span className="inline-flex items-center gap-2xs">
                  <EplTeamCrest className="hidden md:inline-block" team={team} />
                  <Translation tKey={`epl.team.${team}`} />
                </span>
              ),
            })),
          ]}
          renderMobileTrigger={({ isOpen, open }) => (
            <Chip
              isActive={isTriggerActive}
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
              isActive={isTriggerActive}
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
    </nav>
  );

  function handleSelect(next: TeamPickerValue) {
    router.push(next === ALL_VALUE ? buildEplFilterUrl() : buildEplFilterUrl(next));
  }
}
