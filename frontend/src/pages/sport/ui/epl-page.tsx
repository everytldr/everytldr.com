import { type EplTeam } from "@/shared/config";
import { Container } from "@/shared/ui";

type EplPageProps = {
  className?: string;
  filter?: EplTeam;
};

export function EplPage({ className, filter }: EplPageProps) {
  return (
    <main className={className}>
      <Container className="py-2xl">
        <p>EPL: {filter ?? "all"}</p>
      </Container>
    </main>
  );
}
