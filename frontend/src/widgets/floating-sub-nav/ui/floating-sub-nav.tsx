"use client";

import {
  DEFAULT_CATEGORY_NODE,
  findRootCategory,
  isHiddenNode,
  type CategorySlug,
} from "@/shared/config";
import { Link } from "@/shared/i18n";
import { buildCategoryUrl, cn } from "@/shared/lib";
import { Container, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useParams } from "next/navigation";
import { useEffect } from "react";
import { useRafState } from "react-use";

type FloatingSubNavProps = {
  className?: string;
};

export function FloatingSubNav({ className }: FloatingSubNavProps) {
  const params = useParams<{ slug?: CategorySlug }>();
  const categorySlug = params?.slug ?? DEFAULT_CATEGORY_NODE.slug;
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

  const category = findRootCategory(categorySlug);

  return (
    <div
      className={cn(
        "fixed inset-x-0 top-0 z-50 border-b border-hairline bg-canvas transition-transform duration-200 ease-out will-change-transform",
        visible ? "translate-y-0" : "pointer-events-none -translate-y-full",
        className,
      )}
      aria-hidden={!visible}
    >
      <Container className="flex h-14 items-stretch justify-center pc:h-16">
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
                          "inline-flex items-stretch text-nav-md whitespace-nowrap transition-colors outline-none",
                          "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset",
                          isActive ? "text-ink" : "text-meta hover:text-ink",
                        )}
                        href={buildCategoryUrl(child)}
                        tabIndex={visible ? 0 : -1}
                        aria-current={isActive ? "page" : undefined}
                      >
                        <Translation
                          className={cn(
                            "flex items-center border-b-2 transition-colors",
                            isActive ? "border-ink" : "border-transparent",
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
  );
}
