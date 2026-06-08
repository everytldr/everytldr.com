import "server-only";

import { EplTeam } from "@/shared/config";
import { A_DAY, A_SECOND, ensure, type Nullable } from "@/shared/lib";
import { cacheLife, cacheTag } from "next/cache";
import { z } from "zod";
import type { EplStanding } from "../model/epl-standing";

const STANDINGS_URL = "https://api.football-data.org/v4/competitions/PL/standings";
const REVALIDATE_SECONDS = 3600;

const standingsResponseSchema = z.object({
  standings: z.array(
    z.object({
      type: z.string(),
      table: z.array(
        z.object({
          position: z.number(),
          team: z.object({ id: z.number(), name: z.string() }),
          playedGames: z.number(),
          won: z.number(),
          draw: z.number(),
          lost: z.number(),
          goalDifference: z.number(),
          points: z.number(),
        }),
      ),
    }),
  ),
});

export async function fetchEplStandings(): Promise<EplStanding[]> {
  "use cache";

  cacheLife({ revalidate: REVALIDATE_SECONDS, expire: A_DAY / A_SECOND });
  cacheTag("epl-standings");

  const apiKey = ensure(process.env.FOOTBALL_DATA_API_KEY, "Missing FOOTBALL_DATA_API_KEY");
  const response = await fetch(STANDINGS_URL, {
    headers: { "X-Auth-Token": apiKey },
  });
  if (!response.ok) {
    throw new Error(`football-data.org responded ${response.status}`);
  }

  const parsed = standingsResponseSchema.safeParse(await response.json());
  if (!parsed.success) {
    throw new Error("football-data.org response failed schema validation");
  }

  const totalTable = parsed.data.standings.find((entry) => entry.type === "TOTAL")?.table;
  if (!totalTable) {
    throw new Error("football-data.org response missing TOTAL standings");
  }
  if (totalTable.length === 0) {
    throw new Error("football-data.org returned empty standings table");
  }

  return totalTable.map((row) => ({
    rank: row.position,
    team: mapFootballDataTeam(row.team.id),
    teamName: row.team.name,
    played: row.playedGames,
    win: row.won,
    draw: row.draw,
    loss: row.lost,
    goalDifference: row.goalDifference,
    points: row.points,
  }));
}

function mapFootballDataTeam(id: number): Nullable<EplTeam> {
  switch (id) {
    case 57:
      return EplTeam.Arsenal;
    case 58:
      return EplTeam.AstonVilla;
    case 61:
      return EplTeam.Chelsea;
    case 62:
      return EplTeam.Everton;
    case 63:
      return EplTeam.Fulham;
    case 64:
      return EplTeam.Liverpool;
    case 65:
      return EplTeam.ManchesterCity;
    case 66:
      return EplTeam.ManchesterUnited;
    case 67:
      return EplTeam.Newcastle;
    case 71:
      return EplTeam.Sunderland;
    case 73:
      return EplTeam.Tottenham;
    case 76:
      return EplTeam.Wolves;
    case 328:
      return EplTeam.Burnley;
    case 341:
      return EplTeam.Leeds;
    case 351:
      return EplTeam.NottinghamForest;
    case 354:
      return EplTeam.CrystalPalace;
    case 397:
      return EplTeam.Brighton;
    case 402:
      return EplTeam.Brentford;
    case 563:
      return EplTeam.WestHam;
    case 1044:
      return EplTeam.Bournemouth;
    default:
      return null;
  }
}
