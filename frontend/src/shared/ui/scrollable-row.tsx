"use client";

import { useIsCoarsePointer } from "@/shared/hooks";
import { cn, type Nullable, type Optional } from "@/shared/lib";
import { isEqual } from "lodash-es";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useTranslations } from "next-intl";
import { type PropsWithChildren, useEffect, useRef, useState } from "react";
import { IconButton } from "./icon-button";

enum Direction {
  Previous = -1,
  Next = 1,
}

type ScrollableRowProps = PropsWithChildren<{
  className?: string;
  scrollerClassName?: string;
  scrollStep?: "item" | "viewport";
  fade?: boolean;
}>;

const FADE_WIDTH = "2rem";

export function ScrollableRow({
  className,
  scrollerClassName,
  scrollStep = "viewport",
  fade = false,
  children,
}: ScrollableRowProps) {
  const t = useTranslations("common.aria-label");
  const isCoarsePointer = useIsCoarsePointer();
  const scrollerRef = useRef<HTMLDivElement>(null);
  const [{ canScrollPrev, canScrollNext }, setOverflow] = useState({
    canScrollPrev: false,
    canScrollNext: false,
  });

  useEffect(() => {
    const el = scrollerRef.current;
    if (!el) {
      return;
    }

    function update() {
      if (!el) {
        return;
      }
      const max = el.scrollWidth - el.clientWidth;
      const next = {
        canScrollPrev: el.scrollLeft > 1,
        canScrollNext: el.scrollLeft < max - 1,
      };
      setOverflow((current) => {
        return isEqual(current, next) ? current : next;
      });
    }

    update();
    el.addEventListener("scroll", update, { passive: true });
    const observer = new ResizeObserver(update);
    observer.observe(el);

    return () => {
      el.removeEventListener("scroll", update);
      observer.disconnect();
    };
  }, []);

  const showArrows = !isCoarsePointer;

  return (
    <div className={cn("relative", className)}>
      <div
        ref={scrollerRef}
        className={cn("scrollbar-hidden overflow-x-auto", scrollerClassName)}
        style={fade ? { maskImage: buildFadeMask(canScrollPrev, canScrollNext) } : undefined}
      >
        {children}
      </div>
      {showArrows && canScrollPrev && (
        <IconButton
          className="absolute top-1/2 left-0 z-10 -translate-x-1/2 -translate-y-1/2 border border-hairline shadow-floating"
          Icon={ChevronLeft}
          aria-label={t("scroll-previous")}
          onClick={handleScroll(Direction.Previous)}
        />
      )}
      {showArrows && canScrollNext && (
        <IconButton
          className="absolute top-1/2 right-0 z-10 translate-x-1/2 -translate-y-1/2 border border-hairline shadow-floating"
          Icon={ChevronRight}
          aria-label={t("scroll-next")}
          onClick={handleScroll(Direction.Next)}
        />
      )}
    </div>
  );

  function handleScroll(direction: Direction) {
    return () => {
      const el = scrollerRef.current;
      if (!el) {
        return;
      }
      const distance = scrollStep === "item" ? findItemPitch(el) : null;
      el.scrollBy({
        left: direction * (distance ?? el.clientWidth * 0.85),
        behavior: "smooth",
      });
    };
  }
}

function buildFadeMask(fadePrev: boolean, fadeNext: boolean): Optional<string> {
  if (!fadePrev && !fadeNext) {
    return undefined;
  }

  const start = fadePrev ? `transparent 0, black ${FADE_WIDTH}` : "black 0";
  const end = fadeNext ? `black calc(100% - ${FADE_WIDTH}), transparent 100%` : "black 100%";
  return `linear-gradient(to right, ${start}, ${end})`;
}

function findItemPitch(scroller: HTMLDivElement): Nullable<number> {
  const [first, second] = Array.from(scroller.firstElementChild?.children ?? []);
  if (!first || !second) {
    return null;
  }

  const pitch = second.getBoundingClientRect().left - first.getBoundingClientRect().left;
  return pitch > 0 ? pitch : null;
}
