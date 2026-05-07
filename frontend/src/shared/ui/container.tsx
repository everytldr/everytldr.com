import { cn } from "@/shared/lib";
import { type PropsWithChildren } from "react";

type ContainerProps = PropsWithChildren<{
  className?: string;
}>;

export function Container({ className, children }: ContainerProps) {
  return <div className={cn("mx-auto w-full max-w-6xl px-md md:px-xl", className)}>{children}</div>;
}
