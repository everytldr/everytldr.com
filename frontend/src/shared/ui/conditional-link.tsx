import { Link } from "@/shared/i18n";
import { type PropsWithChildren } from "react";

type ConditionalLinkProps = PropsWithChildren<{
  className?: string;
  href: string;
  external?: boolean;
}>;

export function ConditionalLink({ className, href, external, children }: ConditionalLinkProps) {
  if (external) {
    return (
      <a className={className} href={href} rel="noopener noreferrer" target="_blank">
        {children}
      </a>
    );
  }
  return (
    <Link className={className} href={href}>
      {children}
    </Link>
  );
}
