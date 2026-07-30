"use client";

import {
  type CategoryGraph,
  type CategorySlug,
  DEFAULT_CATEGORY_NODE,
  findDedicatedRouteCategorySlug,
  findRootCategory,
  isHiddenNode,
} from "@/shared/config";
import { Link, usePathname } from "@/shared/i18n";
import { buildCategoryUrl, cn } from "@/shared/lib";
import { Container, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useParams } from "next/navigation";

type CategoryNavProps = {
  className?: string;
  categoryGraph: CategoryGraph;
};

export function CategoryNav({ className, categoryGraph }: CategoryNavProps) {
  const t = useTranslations();
  const params = useParams<{ slug?: CategorySlug }>();
  const pathname = usePathname();

  const categorySlug =
    params?.slug ?? findDedicatedRouteCategorySlug(pathname) ?? DEFAULT_CATEGORY_NODE.slug;
  const category = findRootCategory(categoryGraph, categorySlug);

  return (
    <Container className={className}>
      <div className="divide-y divide-hairline rounded-lg border border-hairline bg-surface-soft">
        <nav
          className="scrollbar-hidden overflow-x-auto px-md"
          aria-label={t("header.aria-label.categories")}
        >
          <div className="flex h-12 items-stretch gap-2xs">
            {categoryGraph
              .filter((node) => !isHiddenNode(node))
              .map((node) => {
                const isActive = node.slug === category?.slug;
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
                    href={buildCategoryUrl(node)}
                    prefetch={false}
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
            {category?.children &&
              category.children
                .filter((node) => !isHiddenNode(node))
                .map((child) => {
                  const isActive = child.slug === categorySlug;
                  return (
                    <Link
                      key={child.slug}
                      className={cn(
                        "group inline-flex items-center gap-xs outline-none",
                        "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface-soft",
                      )}
                      href={buildCategoryUrl(child)}
                      prefetch={false}
                      aria-current={isActive ? "page" : undefined}
                    >
                      <span
                        className={cn(
                          "size-2xs shrink-0 rounded-full transition-colors duration-700",
                          isActive ? "bg-ink" : "bg-transparent",
                        )}
                        aria-hidden="true"
                      />
                      <Translation
                        className={cn(
                          "text-nav-sm whitespace-nowrap transition-colors duration-700",
                          isActive ? "text-ink" : "text-meta group-hover:text-ink",
                        )}
                        tKey={`header.subcategory.${child.slug}`}
                      />
                    </Link>
                  );
                })}
          </div>
        </nav>
      </div>
    </Container>
  );
}
