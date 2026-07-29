import {
  findRoutableCategoryNode,
  resolveLeafCategorySlug,
  resolveMainCategorySlug,
  type CategoryNode,
} from "@/shared/config";
import { buildCategoryUrl, cn, type Optional } from "@/shared/lib";
import { ConditionalLink, Translation } from "@/shared/ui";
import { ChevronRight } from "lucide-react";
import { useTranslations } from "next-intl";
import type { PropsWithChildren } from "react";

type ArticleCategoryPathProps = {
  className?: string;
  category: string;
};

export function ArticleCategoryPath({ className, category }: ArticleCategoryPathProps) {
  const t = useTranslations("article-detail");
  const main = resolveMainCategorySlug(category);
  const leaf = resolveLeafCategorySlug(category);
  const hasSubcategory = category.includes("-");

  return (
    <nav
      className={cn("flex min-w-0 flex-wrap items-center gap-2xs text-caption", className)}
      aria-label={t("category-path")}
    >
      <CategorySegment node={findRoutableCategoryNode(main)}>
        <Translation tKey={`header.category.${main}`} />
      </CategorySegment>
      {hasSubcategory && (
        <>
          <ChevronRight className="size-3.5 shrink-0 text-meta-soft" aria-hidden="true" />
          <CategorySegment node={findRoutableCategoryNode(leaf)}>
            <Translation tKey={`header.subcategory.${leaf}`} />
          </CategorySegment>
        </>
      )}
    </nav>
  );
}

type CategorySegmentProps = PropsWithChildren<{
  className?: string;
  node: Optional<CategoryNode>;
}>;

function CategorySegment({ className, node, children }: CategorySegmentProps) {
  return node ? (
    <ConditionalLink
      className={cn(
        "rounded-xs text-meta underline-offset-4 outline-none hover:text-primary hover:underline focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas active:text-primary-pressed",
        className,
      )}
      href={buildCategoryUrl(node)}
    >
      {children}
    </ConditionalLink>
  ) : (
    <span className={cn("text-meta", className)}>{children}</span>
  );
}
