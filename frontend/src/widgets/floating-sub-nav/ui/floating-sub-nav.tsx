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
import { Presence as PresencePrimitive } from "radix-ui/internal";
import { useEffect } from "react";
import { useRafState } from "react-use";

type FloatingSubNavProps = {
  className?: string;
  categoryGraph: CategoryGraph;
};

export function FloatingSubNav({ className, categoryGraph }: FloatingSubNavProps) {
  const params = useParams<{ slug?: CategorySlug }>();
  const pathname = usePathname();
  const categorySlug =
    params?.slug ?? findDedicatedRouteCategorySlug(pathname) ?? DEFAULT_CATEGORY_NODE.slug;
  const t = useTranslations();
  const [visible, setVisible] = useRafState(false);

  useEffect(() => {
    let lastY = window.scrollY;
    let lastVisible = false;

    const onScroll = () => {
      const SHOW_THRESHOLD_PX = 200;
      const HIDE_DELTA_PX = 8;

      const y = window.scrollY;
      let next = lastVisible;
      if (y <= SHOW_THRESHOLD_PX) {
        next = false;
      } else if (y > lastY) {
        next = true;
      } else if (y < lastY - HIDE_DELTA_PX) {
        next = false;
      }
      if (next !== lastVisible) {
        lastVisible = next;
        setVisible(next);
      }
      lastY = y;
    };

    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, [setVisible]);

  useEffect(() => {
    const root = document.documentElement;
    root.style.setProperty("--floating-subnav-height", visible ? "3.5rem" : "0rem");
    return () => {
      root.style.removeProperty("--floating-subnav-height");
    };
  }, [visible]);

  const category = findRootCategory(categoryGraph, categorySlug);

  return (
    <PresencePrimitive.Presence present={visible}>
      <div
        className={cn(
          "fixed inset-x-0 top-0 z-50 border-b border-hairline bg-canvas duration-200 ease-out will-change-transform",
          "data-[state=open]:animate-in data-[state=open]:slide-in-from-top",
          "data-[state=closed]:pointer-events-none data-[state=closed]:animate-out data-[state=closed]:slide-out-to-top",
          className,
        )}
        data-state={visible ? "open" : "closed"}
        aria-hidden={!visible}
      >
        <Container className="flex h-14 items-stretch overflow-x-scroll sm:justify-center">
          <nav className="flex" aria-label={t("header.aria-label.subcategories")}>
            <ul className="flex items-stretch gap-xl">
              {category.children &&
                category.children
                  .filter((node) => !isHiddenNode(node))
                  .map((child) => {
                    const isActive = child.slug === categorySlug;
                    return (
                      <li key={child.slug} className="flex">
                        <Link
                          className={cn(
                            "group inline-flex items-center gap-xs outline-none",
                            "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset",
                          )}
                          href={buildCategoryUrl(child)}
                          prefetch={false}
                          tabIndex={visible ? 0 : -1}
                          aria-current={isActive ? "page" : undefined}
                        >
                          <span
                            className={cn(
                              "size-2xs shrink-0 rounded-full transition-colors",
                              isActive ? "bg-ink" : "bg-transparent",
                            )}
                            aria-hidden="true"
                          />
                          <Translation
                            className={cn(
                              "text-nav-sm whitespace-nowrap transition-colors",
                              isActive ? "text-ink" : "text-meta group-hover:text-ink",
                            )}
                            tKey={`header.subcategory.${child.slug}`}
                          />
                        </Link>
                      </li>
                    );
                  })}
            </ul>
          </nav>
        </Container>
      </div>
    </PresencePrimitive.Presence>
  );
}
