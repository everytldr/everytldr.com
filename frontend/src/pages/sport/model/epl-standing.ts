import type { EplTeam } from "@/shared/config";
import type { Nullable } from "@/shared/lib";

export type EplStanding = {
  rank: number;
  team: Nullable<EplTeam>;
  teamName: string;
  played: number;
  win: number;
  draw: number;
  loss: number;
  goalDifference: number;
  points: number;
};
