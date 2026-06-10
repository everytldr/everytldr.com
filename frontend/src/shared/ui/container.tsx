import { cn } from "@/shared/lib";
import { type PropsWithChildren } from "react";

type ContainerProps = PropsWithChildren<{
  className?: string;
  size?: "sm" | "md";
}>;

export function Container({ className, size = "md", children }: ContainerProps) {
  return (
    <div
      className={cn(
        "mx-auto w-full px-md pc:px-xl",
        size === "md" && "max-w-5xl",
        size === "sm" && "max-w-3xl",
        className,
      )}
    >
      {children}
    </div>
  );
}
