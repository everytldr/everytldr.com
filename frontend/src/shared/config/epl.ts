export enum EplTabSlug {
  News = "news",
  Record = "record",
}

export enum EplTeam {
  Arsenal = "arsenal",
  AstonVilla = "aston_villa",
  Bournemouth = "bournemouth",
  Brentford = "brentford",
  Brighton = "brighton",
  Burnley = "burnley",
  Chelsea = "chelsea",
  CrystalPalace = "crystal_palace",
  Everton = "everton",
  Fulham = "fulham",
  Leeds = "leeds",
  Liverpool = "liverpool",
  ManchesterCity = "manchester_city",
  ManchesterUnited = "manchester_united",
  Newcastle = "newcastle",
  NottinghamForest = "nottingham_forest",
  Sunderland = "sunderland",
  Tottenham = "tottenham",
  WestHam = "west_ham",
  Wolves = "wolves",
}

export const EPL_BIG_SIX_TEAMS = [
  EplTeam.Arsenal,
  EplTeam.Chelsea,
  EplTeam.Liverpool,
  EplTeam.ManchesterCity,
  EplTeam.ManchesterUnited,
  EplTeam.Tottenham,
] as const;

export type BigSixTeam = (typeof EPL_BIG_SIX_TEAMS)[number];

const EPL_BIG_SIX_SET: ReadonlySet<EplTeam> = new Set(EPL_BIG_SIX_TEAMS);

export function isBigSixTeam(team: EplTeam): team is BigSixTeam {
  return EPL_BIG_SIX_SET.has(team);
}

export const EPL_TEAMS_ALPHABETICAL: readonly EplTeam[] = Object.values(EplTeam).slice().sort();
