import { CategoryNav } from "@/widgets/category-nav";
import { FloatingSubNav } from "@/widgets/floating-sub-nav";
import { type PropsWithChildren } from "react";

export default function Layout({ children }: PropsWithChildren) {
  return (
    <>
      <CategoryNav />
      <FloatingSubNav />
      {children}
    </>
  );
}
