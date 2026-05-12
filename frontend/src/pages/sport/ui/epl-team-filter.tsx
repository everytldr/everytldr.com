"use client";

import {
  EPL_BIG_SIX_TEAMS,
  EPL_TEAMS_ALPHABETICAL,
  type EplTeam,
  isBigSixTeam,
} from "@/shared/config";
import { Link, useRouter } from "@/shared/i18n";
import { buildEplFilterUrl, cn } from "@/shared/lib";
import { ResponsiveSelector, Translation } from "@/shared/ui";
import { ChevronDown } from "lucide-react";
import { useTranslations } from "next-intl";
import { type ComponentProps, type PropsWithChildren } from "react";
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
        <PillLink href={buildEplFilterUrl()} isActive={!filter}>
          <Translation tKey="epl.all-teams" />
        </PillLink>

        {EPL_BIG_SIX_TEAMS.map((team) => {
          const isActive = filter === team;
          return (
            <PillLink key={team} href={buildEplFilterUrl(team)} isActive={isActive}>
              <EplTeamCrest className="hidden md:inline-block" team={team} />
              <Translation tKey={`epl.team-short.${team}`} />
            </PillLink>
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
            <PickerTrigger
              isActive={isTriggerActive}
              isOpen={isOpen}
              aria-haspopup="dialog"
              aria-label={t("epl.aria-label.team-picker")}
              onClick={open}
            >
              {isTriggerActive ? (
                <Translation tKey={`epl.team.${filter}`} />
              ) : (
                <Translation tKey="epl.more-teams" />
              )}
            </PickerTrigger>
          )}
          renderDesktopTrigger={() => (
            <PickerTrigger isActive={isTriggerActive} aria-label={t("epl.aria-label.team-picker")}>
              {isTriggerActive ? (
                <Translation tKey={`epl.team.${filter}`} />
              ) : (
                <Translation tKey="epl.more-teams" />
              )}
            </PickerTrigger>
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

type PillLinkProps = PropsWithChildren<{
  className?: string;
  href: string;
  isActive: boolean;
}>;

function PillLink({ className, href, isActive, children }: PillLinkProps) {
  return (
    <Link
      className={cn(
        "inline-flex h-9 items-center gap-2xs rounded-full px-md text-button-sm whitespace-nowrap transition-colors outline-none",
        "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas",
        isActive
          ? "border border-transparent bg-ink text-on-ink active:bg-ink/90"
          : "border border-hairline bg-canvas text-body hover:bg-surface-soft hover:text-ink active:bg-surface-strong active:text-ink dark:hover:bg-surface-strong dark:active:bg-surface-pressed",
        className,
      )}
      href={href}
      aria-current={isActive ? "page" : undefined}
    >
      {children}
    </Link>
  );
}

type PickerTriggerProps = ComponentProps<"button"> & {
  isActive: boolean;
  isOpen?: boolean;
};

function PickerTrigger({
  className,
  isActive,
  isOpen = false,
  children,
  ...props
}: PickerTriggerProps) {
  return (
    <button
      className={cn(
        "group inline-flex h-9 items-center gap-2xs rounded-full px-md text-button-sm whitespace-nowrap transition-colors outline-none",
        "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas",
        isActive
          ? "border border-transparent bg-ink text-on-ink active:bg-ink/90"
          : "border border-hairline bg-canvas text-body hover:bg-surface-soft hover:text-ink active:bg-surface-strong active:text-ink dark:hover:bg-surface-strong dark:active:bg-surface-pressed",
        className,
      )}
      type="button"
      data-state={isOpen ? "open" : "closed"}
      {...props}
    >
      {children}
      <ChevronDown
        className="size-4 transition-transform group-data-[state=open]:rotate-180"
        aria-hidden
      />
    </button>
  );
}
