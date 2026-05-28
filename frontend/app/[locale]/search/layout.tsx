import { Container, Skeleton } from "@/shared/ui";
import { type PropsWithChildren, Suspense } from "react";

export default function Layout({ children }: PropsWithChildren) {
  return <Suspense fallback={<Fallback />}>{children}</Suspense>;
}

type FallbackProps = {
  className?: string;
};

function Fallback({ className }: FallbackProps) {
  return (
    <main className={className}>
      <Container className="flex max-w-180 flex-col gap-2xl py-xl">
        <Skeleton className="h-12 w-full rounded-md" />
        <Skeleton className="h-32 w-full rounded-md" />
      </Container>
    </main>
  );
}
