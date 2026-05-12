"use client";

import { useKey } from "react-use";

export function useArrowNavigation() {
  useKey(
    (event) => event.key === "ArrowDown" || event.key === "ArrowUp",
    (event) => {
      const items = Array.from(document.querySelectorAll<HTMLElement>("[data-search-nav]"));
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
    },
  );
}
