import { cn } from "@/shared/lib";
import { Container, MarkdownContent } from "@/shared/ui";
import { type StaticPageContent } from "../api/read-static-page";

type StaticPageProps = {
  className?: string;
  content: StaticPageContent;
};

export function StaticPage({ className, content }: StaticPageProps) {
  return (
    <main className={cn("py-2xl pc:py-section", className)}>
      <Container className="space-y-xl" size="sm">
        <header className="border-b border-hairline-soft pb-lg">
          <h1 className="text-display-xl text-ink">{content.title}</h1>
        </header>
        <MarkdownContent markdown={content.markdown} />
      </Container>
    </main>
  );
}
