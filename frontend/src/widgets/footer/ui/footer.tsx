import { Link } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { ConditionalLink, Container, Logo, Translation } from "@/shared/ui";
import { cacheLife } from "next/cache";
import { type PropsWithChildren } from "react";

const ABOUT_URL = process.env.NEXT_PUBLIC_FOOTER_ABOUT_URL || "/about";
const PRIVACY_URL = process.env.NEXT_PUBLIC_FOOTER_PRIVACY_URL || "/privacy";
const TERMS_URL = process.env.NEXT_PUBLIC_FOOTER_TERMS_URL || "/terms";
const GITHUB_URL =
  process.env.NEXT_PUBLIC_FOOTER_GITHUB_URL || "https://github.com/everytldr/everytldr.com";

type FooterProps = {
  className?: string;
};

export async function Footer({ className }: FooterProps) {
  const year = await getFooterYear();

  return (
    <footer className={cn("border-t border-hairline bg-canvas", className)}>
      <Container className="flex flex-col items-start gap-md py-xl text-meta pc:flex-row pc:items-center pc:justify-between">
        <div className="flex items-center gap-md text-body-sm">
          <Link href="/" aria-label="everytldr">
            <Logo className="h-5" />
          </Link>
          <span>© {year} everytldr</span>
        </div>

        <nav className="flex flex-wrap items-center gap-x-sm gap-y-xs text-nav-sm">
          <FooterLink href={ABOUT_URL}>
            <Translation tKey="footer.about" />
          </FooterLink>
          <FooterSeparator />
          <FooterLink href={PRIVACY_URL}>
            <Translation tKey="footer.privacy" />
          </FooterLink>
          <FooterSeparator />
          <FooterLink href={TERMS_URL}>
            <Translation tKey="footer.terms" />
          </FooterLink>
          <FooterSeparator />
          <FooterLink href={GITHUB_URL}>
            <Translation tKey="footer.github" />
          </FooterLink>
        </nav>
      </Container>
    </footer>
  );
}

async function getFooterYear(): Promise<number> {
  "use cache";

  cacheLife("days");

  return new Date().getFullYear();
}

type FooterLinkProps = PropsWithChildren<{
  className?: string;
  href: string;
}>;

function FooterLink({ className, href, children }: FooterLinkProps) {
  return (
    <ConditionalLink
      className={cn(
        "transition-colors outline-none hover:text-ink focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas",
        className,
      )}
      href={href}
    >
      {children}
    </ConditionalLink>
  );
}

type FooterSeparatorProps = {
  className?: string;
};

function FooterSeparator({ className }: FooterSeparatorProps) {
  return (
    <span className={cn("text-meta-soft select-none", className)} aria-hidden="true">
      ·
    </span>
  );
}
