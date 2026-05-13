import { fetchEplStandings } from "../api/fetch-epl-standings";
import { EplStandingsTable } from "./epl-standings-table";

type EplStandingsSectionProps = {
  className?: string;
};

export async function EplStandingsSection({ className }: EplStandingsSectionProps) {
  const standings = await fetchEplStandings();

  return (
    <section className={className}>
      <EplStandingsTable standings={standings} />
    </section>
  );
}
