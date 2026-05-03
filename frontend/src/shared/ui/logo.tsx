import LogoDark from "./logo-dark.svg";
import LogoLight from "./logo-light.svg";

type Props = {
  className?: string;
};

export function Logo({ className }: Props) {
  return (
    <div className={className}>
      <LogoLight className="block dark:hidden" />
      <LogoDark className="hidden dark:block" />
    </div>
  );
}
