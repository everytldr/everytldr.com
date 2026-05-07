import { cn } from "../lib";
import LogoDark from "./logo-dark.svg";
import LogoLight from "./logo-light.svg";

type LogoProps = {
  className?: string;
};

export function Logo({ className }: LogoProps) {
  return (
    <div className={cn("", className)}>
      <LogoLight className="block size-full dark:hidden" />
      <LogoDark className="hidden size-full dark:block" />
    </div>
  );
}
