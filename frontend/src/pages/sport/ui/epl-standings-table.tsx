import { cn } from "@/shared/lib";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Translation,
} from "@/shared/ui";
import { useTranslations } from "next-intl";
import type { EplStanding } from "../model/epl-standing";
import { EplTeamCrest } from "./epl-team-crest";

type EplStandingsTableProps = {
  className?: string;
  standings: ReadonlyArray<EplStanding>;
};

export function EplStandingsTable({ className, standings }: EplStandingsTableProps) {
  const t = useTranslations();

  return (
    <Table className={className} aria-label={t("epl.aria-label.standings")}>
      <TableHeader>
        <TableRow>
          <TableHead className="sticky left-0 z-10 w-2xl border-l-2 border-transparent bg-canvas text-right">
            <Translation tKey="epl.record.column.rank" />
          </TableHead>
          <TableHead className="sticky left-2xl z-10 w-45 border-r border-hairline bg-canvas">
            <Translation tKey="epl.record.column.team" />
          </TableHead>
          <TableHead className="text-right">
            <Translation tKey="epl.record.column.played" />
          </TableHead>
          <TableHead className="text-right">
            <Translation tKey="epl.record.column.win" />
          </TableHead>
          <TableHead className="text-right">
            <Translation tKey="epl.record.column.draw" />
          </TableHead>
          <TableHead className="text-right">
            <Translation tKey="epl.record.column.loss" />
          </TableHead>
          <TableHead className="text-right">
            <Translation tKey="epl.record.column.goal-diff" />
          </TableHead>
          <TableHead className="pr-md text-right">
            <Translation tKey="epl.record.column.points" />
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {standings.map((row) => (
          <TableRow
            key={row.rank}
            className="group hover:bg-surface-soft dark:hover:bg-surface-strong"
          >
            <TableCell
              className={cn(
                "sticky left-0 z-10 w-2xl border-l-2 bg-canvas text-right text-caption-mono text-meta tabular-nums group-hover:bg-surface-soft dark:group-hover:bg-surface-strong",
                row.rank <= 4
                  ? "border-tint-sky-fg"
                  : row.rank === 5
                    ? "border-tint-peach-fg"
                    : row.rank >= 18
                      ? "border-tint-rose-fg"
                      : "border-transparent",
              )}
            >
              {row.rank}
            </TableCell>
            <TableCell className="sticky left-2xl z-10 w-45 border-r border-hairline bg-canvas group-hover:bg-surface-soft dark:group-hover:bg-surface-strong">
              <span className="inline-flex items-center gap-xs align-middle">
                {row.team ? (
                  <>
                    <EplTeamCrest className="dark:drop-stroke" team={row.team} />
                    <Translation className="text-body-sm text-ink" tKey={`epl.team.${row.team}`} />
                  </>
                ) : (
                  <span className="text-body-sm text-ink">{row.teamName}</span>
                )}
              </span>
            </TableCell>
            <TableCell className="text-right text-caption-mono text-body tabular-nums">
              {row.played}
            </TableCell>
            <TableCell className="text-right text-caption-mono text-body tabular-nums">
              {row.win}
            </TableCell>
            <TableCell className="text-right text-caption-mono text-body tabular-nums">
              {row.draw}
            </TableCell>
            <TableCell className="text-right text-caption-mono text-body tabular-nums">
              {row.loss}
            </TableCell>
            <TableCell className="text-right text-caption-mono text-body tabular-nums">
              {formatGoalDifference(row.goalDifference)}
            </TableCell>
            <TableCell className="pr-md text-right text-caption-mono font-semibold text-ink tabular-nums">
              {row.points}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function formatGoalDifference(diff: number): string {
  return diff > 0 ? `+${diff}` : `${diff}`;
}
