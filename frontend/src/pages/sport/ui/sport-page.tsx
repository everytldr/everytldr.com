import { type EplTeam, SubCategorySlug } from "@/shared/config";
import { Container } from "@/shared/ui";

type SportPageProps = { className?: string } & (
  | { categorySlug: SubCategorySlug.NBA; filter?: unknown } // TODO: MVP 이후에 구현 예정
  | { categorySlug: SubCategorySlug.EPL; filter?: EplTeam }
);

export function SportPage(props: SportPageProps) {
  const { className, categorySlug } = props;

  if (categorySlug === SubCategorySlug.EPL) {
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
