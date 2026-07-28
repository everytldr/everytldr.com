"use client";

import { A_MINUTE, useHydrated } from "@/shared/lib";
import { useFormatter, useNow } from "next-intl";

type RelativeTimeProps = {
  className?: string;
  date: string;
};

export function RelativeTime({ className, date }: RelativeTimeProps) {
  const format = useFormatter();
  const now = useNow({ updateInterval: A_MINUTE });
  const hydrated = useHydrated();

  return (
    <time key={String(hydrated)} className={className} dateTime={date} suppressHydrationWarning>
      {format.relativeTime(new Date(date), now)}
    </time>
  );
}
