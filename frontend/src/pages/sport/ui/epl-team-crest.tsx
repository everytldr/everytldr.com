import { EplTeam } from "@/shared/config";
import { cn } from "@/shared/lib";
import Arsenal from "./crests/arsenal.svg";
import AstonVilla from "./crests/aston-villa.svg";
import Bournemouth from "./crests/bournemouth.svg";
import Brentford from "./crests/brentford.svg";
import Brighton from "./crests/brighton.svg";
import Burnley from "./crests/burnley.svg";
import Chelsea from "./crests/chelsea.svg";
import CrystalPalace from "./crests/crystal-palace.svg";
import Everton from "./crests/everton.svg";
import Fulham from "./crests/fulham.svg";
import Leeds from "./crests/leeds.svg";
import Liverpool from "./crests/liverpool.svg";
import ManchesterCity from "./crests/manchester-city.svg";
import ManchesterUnited from "./crests/manchester-united.svg";
import Newcastle from "./crests/newcastle.svg";
import NottinghamForest from "./crests/nottingham-forest.svg";
import Sunderland from "./crests/sunderland.svg";
import Tottenham from "./crests/tottenham.svg";
import WestHam from "./crests/west-ham.svg";
import Wolves from "./crests/wolves.svg";

type EplTeamCrestProps = {
  className?: string;
  team: EplTeam;
};

export function EplTeamCrest({ className, team }: EplTeamCrestProps) {
  return (
    <div className={cn("inline-block size-md [&>svg]:size-full", className)} aria-hidden>
      {team === EplTeam.Arsenal ? (
        <Arsenal />
      ) : team === EplTeam.AstonVilla ? (
        <AstonVilla />
      ) : team === EplTeam.Bournemouth ? (
        <Bournemouth />
      ) : team === EplTeam.Brentford ? (
        <Brentford />
      ) : team === EplTeam.Brighton ? (
        <Brighton />
      ) : team === EplTeam.Burnley ? (
        <Burnley />
      ) : team === EplTeam.Chelsea ? (
        <Chelsea />
      ) : team === EplTeam.CrystalPalace ? (
        <CrystalPalace />
      ) : team === EplTeam.Everton ? (
        <Everton />
      ) : team === EplTeam.Fulham ? (
        <Fulham />
      ) : team === EplTeam.Leeds ? (
        <Leeds />
      ) : team === EplTeam.Liverpool ? (
        <Liverpool />
      ) : team === EplTeam.ManchesterCity ? (
        <ManchesterCity />
      ) : team === EplTeam.ManchesterUnited ? (
        <ManchesterUnited />
      ) : team === EplTeam.Newcastle ? (
        <Newcastle />
      ) : team === EplTeam.NottinghamForest ? (
        <NottinghamForest />
      ) : team === EplTeam.Sunderland ? (
        <Sunderland />
      ) : team === EplTeam.Tottenham ? (
        <Tottenham />
      ) : team === EplTeam.WestHam ? (
        <WestHam />
      ) : team === EplTeam.Wolves ? (
        <Wolves />
      ) : null}
    </div>
  );
}
