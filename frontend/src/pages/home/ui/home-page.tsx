import { type SubCategorySlug } from "@/shared/config";
import { Container } from "@/shared/ui";
import { FloatingSubNav } from "@/widgets/floating-sub-nav";
import { CategoryNav } from "./category-nav";

type HomePageProps = {
  className?: string;
  categorySlug: SubCategorySlug;
};

export function HomePage({ className, categorySlug }: HomePageProps) {
  return (
    <div className={className}>
      <CategoryNav categorySlug={categorySlug} />
      <FloatingSubNav categorySlug={categorySlug} />
      <main>
        <Container className="py-2xl">
          <p>hello world</p>
        </Container>
      </main>
    </div>
  );
}
