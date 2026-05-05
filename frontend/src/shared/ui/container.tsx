import { cn } from "@/shared/lib";
import { type PropsWithChildren } from "react";

type Props = PropsWithChildren<{
  className?: string;
}>;

export function Container({ className, children }: Props) {
  return <div className={cn("mx-auto w-full max-w-7xl px-4 md:px-8", className)}>{children}</div>;
}
