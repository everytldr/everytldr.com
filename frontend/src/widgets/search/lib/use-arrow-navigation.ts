"use client";

import { useEffect, useRef } from "react";

export function useArrowNavigation<T extends HTMLElement = HTMLDivElement>() {
  const ref = useRef<T>(null);

  useEffect(() => {
    const container = ref.current;
    if (!container) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key !== "ArrowDown" && event.key !== "ArrowUp") {
        return;
      }

      const items = Array.from(container!.querySelectorAll<HTMLElement>("[data-search-nav]"));
      const currentIndex = items.indexOf(document.activeElement as HTMLElement);
      if (currentIndex === -1) {
        return;
      }

      const direction = event.key === "ArrowDown" ? 1 : -1;
      const nextIndex = Math.max(0, Math.min(items.length - 1, currentIndex + direction));
      if (nextIndex === currentIndex) {
        return;
      }

      event.preventDefault();
      items[nextIndex]?.focus();
    }

    container.addEventListener("keydown", handleKeyDown);
    return () => container.removeEventListener("keydown", handleKeyDown);
  }, []);

  return ref;
}
