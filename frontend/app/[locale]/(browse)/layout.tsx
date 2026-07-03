import { CATEGORY_GRAPH } from "@/shared/config";
import { CategoryNav } from "@/widgets/category-nav";
import { FloatingSubNav } from "@/widgets/floating-sub-nav";
import { type PropsWithChildren } from "react";

export default function Layout({ children }: PropsWithChildren) {
  return (
    <>
      <FloatingSubNav categoryGraph={CATEGORY_GRAPH} />
      <div className="mt-xs">
        <CategoryNav categoryGraph={CATEGORY_GRAPH} />
        {children}
      </div>
    </>
  );
}
