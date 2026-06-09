import { Container } from "@/shared/ui";
import { type PropsWithChildren } from "react";

export default function Layout({ children }: PropsWithChildren) {
  return (
    <main className="py-xl">
      <Container size="sm">{children}</Container>
    </main>
  );
}
