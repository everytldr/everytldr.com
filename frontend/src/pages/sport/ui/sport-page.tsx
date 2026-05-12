import { type EplPageTab, type EplTeam, SubCategorySlug } from "@/shared/config";
import { notFound } from "next/navigation";
import { EplPage } from "./epl-page";

type SportPageProps = { className?: string } & (
  | { slug: SubCategorySlug.NBA; subSlug?: unknown } // TODO: MVP 이후에 구현 예정
  | { slug: SubCategorySlug.EPL; subSlug?: EplPageTab | EplTeam }
);

export function SportPage(props: SportPageProps) {
  if (props.slug === SubCategorySlug.EPL) {
    return <EplPage className={props.className} subSlug={props.subSlug} />;
  }

  notFound();
}
