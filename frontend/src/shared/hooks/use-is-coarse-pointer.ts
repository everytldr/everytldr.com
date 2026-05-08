"use client";

import { useMedia } from "react-use";

export function useIsCoarsePointer(): boolean {
  return useMedia("(pointer: coarse)", false);
}
