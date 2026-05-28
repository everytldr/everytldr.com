"use client";

import { useFormatter, useNow } from "next-intl";

type RelativeTimeProps = {
  className?: string;
  date: string;
};

export function RelativeTime({ className, date }: RelativeTimeProps) {
  const format = useFormatter();
  const now = useNow({ updateInterval: 60_000 });

  return <span className={className}>{format.relativeTime(new Date(date), now)}</span>;
}
