import { Container } from "@/shared/ui";
import { Header } from "./header";

export function HomePage() {
  return (
    <>
      <Header />
      <main>
        <Container className="py-16">
          <p>hello world</p>
        </Container>
      </main>
    </>
  );
}
