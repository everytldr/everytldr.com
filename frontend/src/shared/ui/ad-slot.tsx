"use client";

import { ADSENSE_CLIENT_ID } from "@/shared/config";
import { cn } from "@/shared/lib";
import { useEffect, useRef } from "react";

declare global {
  interface Window {
    adsbygoogle?: Record<string, unknown>[];
  }
}

type AdSlotProps = {
  className?: string;
  slot: string;
};

export function AdSlot({ className, slot }: AdSlotProps) {
  const pushedRef = useRef(false);

  useEffect(() => {
    if (pushedRef.current) {
      return;
    }
    pushedRef.current = true;
    (window.adsbygoogle = window.adsbygoogle ?? []).push({});
  }, []);

  return (
    <div className={cn("min-h-24", className)}>
      <ins
        className="adsbygoogle block w-full"
        data-ad-client={ADSENSE_CLIENT_ID}
        data-ad-slot={slot}
        data-ad-format="auto"
        data-full-width-responsive="true"
      />
    </div>
  );
}
