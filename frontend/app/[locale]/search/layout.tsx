import { Container } from "@/shared/ui";
import { type PropsWithChildren } from "react";

export default function Layout({ children }: PropsWithChildren) {
  return (
    <main>
      <Container className="py-xl" size="sm">
        {children}
      </Container>
    </main>
  );
}
