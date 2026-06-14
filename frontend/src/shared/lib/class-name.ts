import { clsx, type ClassValue } from "clsx";
import { extendTailwindMerge } from "tailwind-merge";

const twMerge = extendTailwindMerge({
  extend: {
    theme: {
      text: [
        "hero-display",
        "display-xl",
        "display-lg",
        "display-md",
        "display-sm",
        "title-md",
        "title-sm",
        "body-lg",
        "body-md",
        "body-sm",
        "caption",
        "caption-mono",
        "micro",
        "button-md",
        "button-sm",
        "nav-md",
        "nav-sm",
      ],
    },
  },
});

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
