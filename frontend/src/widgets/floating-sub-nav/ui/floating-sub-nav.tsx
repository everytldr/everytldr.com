"use client";

import { type SubCategorySlug, findRootCategory } from "@/shared/config";
import { Link } from "@/shared/i18n";
import { buildSubcategoryUrl, cn } from "@/shared/lib";
import { Container, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useEffect } from "react";
import { useRafState } from "react-use";

type FloatingSubNavProps = {
  className?: string;
  categorySlug: SubCategorySlug;
};

export function FloatingSubNav({ className, categorySlug }: FloatingSubNavProps) {
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
      <Container className="flex h-14 items-stretch justify-center md:h-16">
        <nav className="flex" aria-label={t("header.aria-label.subcategories")}>
          <ul className="flex items-stretch gap-xl">
            {category.subs.map((sub) => {
              const isActive = sub === categorySlug;
              return (
                <li key={sub} className="flex">
                  <Link
                    className={cn(
                      "inline-flex items-stretch text-title-md whitespace-nowrap transition-colors outline-none",
                      "focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset",
                      isActive ? "text-ink" : "text-meta hover:text-ink",
                    )}
                    href={buildSubcategoryUrl(category, sub)}
                    tabIndex={visible ? 0 : -1}
                    aria-current={isActive ? "page" : undefined}
                  >
                    <Translation
                      className={cn(
                        "flex items-center border-b-2 transition-colors",
                        isActive ? "border-ink" : "border-transparent",
                      )}
                      tKey={`header.subcategory.${sub}`}
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
