import { Container, ScrollReset } from "@/shared/ui";
import { type PropsWithChildren } from "react";

export default function Layout({ children }: PropsWithChildren) {
  return (
    <main className="py-xl">
      <ScrollReset />
      <Container size="sm">{children}</Container>
    </main>
  );
}
