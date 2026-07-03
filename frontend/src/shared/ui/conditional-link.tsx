import { Link } from "@/shared/i18n";
import { type PropsWithChildren } from "react";

type ConditionalLinkProps = PropsWithChildren<{
  className?: string;
  href: string;
  prefetch?: boolean;
}>;

export function ConditionalLink({ className, href, prefetch, children }: ConditionalLinkProps) {
  if (isExternalHref(href)) {
    return (
      <a className={className} href={href} rel="noopener noreferrer" target="_blank">
        {children}
      </a>
    );
  }
  return (
    <Link className={className} href={href} prefetch={prefetch}>
      {children}
    </Link>
  );
}

function isExternalHref(href: string): boolean {
  return /^[a-z][a-z0-9+.-]*:/i.test(href) || href.startsWith("//");
}
