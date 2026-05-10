import { type EplTeam, SubCategorySlug } from "@/shared/config";
import { notFound } from "next/navigation";
import { EplPage } from "./epl-page";

type SportPageProps = { className?: string } & (
  | { categorySlug: SubCategorySlug.NBA; filter?: unknown } // TODO: MVP 이후에 구현 예정
  | { categorySlug: SubCategorySlug.EPL; filter?: EplTeam }
);

export function SportPage(props: SportPageProps) {
  if (props.categorySlug === SubCategorySlug.EPL) {
    return <EplPage className={props.className} filter={props.filter} />;
  }

  notFound();
}
