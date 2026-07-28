import { type EplTabSlug, type EplTeam } from "@/shared/config";
import type { Locale } from "@/shared/i18n";
import { notFound } from "next/navigation";
import { EplPage } from "./epl-page";

type SportPageProps = { className?: string; locale: Locale } & (
  | { slug: "nba"; subSlug?: unknown } // TODO: MVP 이후에 구현 예정
  | { slug: "epl"; subSlug?: EplTabSlug | EplTeam }
);

export function SportPage(props: SportPageProps) {
  if (props.slug === "nba") {
    notFound();
  }

  return <EplPage className={props.className} locale={props.locale} subSlug={props.subSlug} />;
}
