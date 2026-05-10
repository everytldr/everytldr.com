import { Container } from "@/shared/ui";

type HomePageProps = {
  className?: string;
};

export function HomePage({ className }: HomePageProps) {
  return (
    <div className={className}>
      <main>
        <Container className="py-2xl">
          <p>hello world</p>
        </Container>
      </main>
    </div>
  );
}
