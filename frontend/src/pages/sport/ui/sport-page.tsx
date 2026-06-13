import { type EplTabSlug, type EplTeam } from "@/shared/config";
import { notFound } from "next/navigation";
import { EplPage } from "./epl-page";

type SportPageProps = { className?: string } & (
  | { slug: "nba"; subSlug?: unknown } // TODO: MVP 이후에 구현 예정
  | { slug: "epl"; subSlug?: EplTabSlug | EplTeam }
);

export function SportPage(props: SportPageProps) {
  if (props.slug !== "epl") {
    notFound();
  }

  return <EplPage className={props.className} subSlug={props.subSlug} />;
}
