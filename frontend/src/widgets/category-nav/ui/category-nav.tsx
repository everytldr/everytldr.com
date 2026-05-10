"use client";

import {
  CATEGORIES,
  DEFAULT_SUB_CATEGORY_SLUG,
  type SubCategorySlug,
  findRootCategory,
} from "@/shared/config";
import { Link } from "@/shared/i18n";
import { buildMainCategoryUrl, buildSubcategoryUrl, cn } from "@/shared/lib";
import { Container, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useParams } from "next/navigation";

type CategoryNavProps = {
  className?: string;
};

export function CategoryNav({ className }: CategoryNavProps) {
  const t = useTranslations();
  const { slug } = useParams<{ slug?: SubCategorySlug }>();
  const categorySlug = slug ?? DEFAULT_SUB_CATEGORY_SLUG;
  const category = findRootCategory(categorySlug);

  return (
    <Container className={className}>
      <div className="divide-y divide-hairline rounded-lg border border-hairline bg-surface-soft">
        <nav
          className="scrollbar-hidden overflow-x-auto px-md"
          aria-label={t("header.aria-label.categories")}
        >
          <div className="flex h-12 items-stretch gap-2xs">
            {CATEGORIES.map((node) => {
              const isActive = node.slug === category.slug;
              return (
                <Link
                  key={node.slug}
                  className={cn(
                    "inline-flex items-center border-b-2 px-md text-nav-md whitespace-nowrap transition-colors outline-none",
                    "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset",
                    isActive
                      ? "border-ink text-ink"
                      : "border-transparent text-meta hover:text-ink",
                  )}
                  href={buildMainCategoryUrl(node)}
                  aria-current={isActive ? "page" : undefined}
                >
                  <Translation tKey={`header.category.${node.slug}`} />
                </Link>
              );
            })}
          </div>
        </nav>

        <nav
          className="scrollbar-hidden overflow-x-auto px-md"
          aria-label={t("header.aria-label.subcategories")}
        >
          <div className="flex h-11 items-center gap-md">
            {category.subs.map((sub) => {
              const isActive = sub === categorySlug;
              return (
                <Link
                  key={sub}
                  className={cn(
                    "inline-flex items-center text-nav-sm whitespace-nowrap transition-colors outline-none",
                    "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface-soft",
                    isActive ? "text-ink" : "text-meta-soft hover:text-ink",
                  )}
                  href={buildSubcategoryUrl(category, sub)}
                  aria-current={isActive ? "page" : undefined}
                >
                  <Translation tKey={`header.subcategory.${sub}`} />
                </Link>
              );
            })}
          </div>
        </nav>
      </div>
    </Container>
  );
}
