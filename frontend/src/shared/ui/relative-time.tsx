"use client";

import { A_MINUTE } from "@/shared/lib";
import { useFormatter, useNow } from "next-intl";

type RelativeTimeProps = {
  className?: string;
  date: string;
};

export function RelativeTime({ className, date }: RelativeTimeProps) {
  const format = useFormatter();
  const now = useNow({ updateInterval: A_MINUTE });

  return <span className={className}>{format.relativeTime(new Date(date), now)}</span>;
}
