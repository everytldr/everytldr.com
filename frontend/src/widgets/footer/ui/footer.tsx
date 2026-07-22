import { ROUTABLE_MAIN_CATEGORY_NODES, STATIC_PAGE_URLS } from "@/shared/config";
import { Link } from "@/shared/i18n";
import { buildCategoryUrl, cn } from "@/shared/lib";
import { ConditionalLink, Container, Logo, Translation } from "@/shared/ui";
import { getTranslations } from "next-intl/server";
import { cacheLife } from "next/cache";
import { type PropsWithChildren } from "react";

const CONTACT_EMAIL = process.env.NEXT_PUBLIC_CONTACT_EMAIL || "contact@everytldr.com";

type FooterProps = {
  className?: string;
};

export async function Footer({ className }: FooterProps) {
  const t = await getTranslations();
  const year = await getFooterYear();

  return (
    <footer className={cn("border-t border-hairline bg-canvas", className)}>
      <Container className="flex flex-col gap-lg py-xl text-meta">
        <nav
          className="flex flex-wrap items-center gap-x-sm gap-y-xs text-nav-sm"
          aria-label={t("footer.aria-label.categories")}
        >
          {ROUTABLE_MAIN_CATEGORY_NODES.map((node) => (
            <FooterLink key={node.slug} href={buildCategoryUrl(node)}>
              <Translation tKey={`header.category.${node.slug}`} />
            </FooterLink>
          ))}
        </nav>

        <div className="flex flex-col items-start gap-md pc:flex-row pc:items-center pc:justify-between">
          <div className="flex items-center gap-md text-body-sm">
            <Link href="/" aria-label="everytldr">
              <Logo className="h-5 w-auto" />
            </Link>
            <span>© {year} everytldr</span>
          </div>

          <nav
            className="flex flex-wrap items-center gap-x-sm gap-y-xs text-nav-sm"
            aria-label={t("footer.aria-label.site")}
          >
            <FooterLink href={STATIC_PAGE_URLS.about}>
              <Translation tKey="footer.about" />
            </FooterLink>
            <FooterLink href={STATIC_PAGE_URLS.privacy}>
              <Translation tKey="footer.privacy" />
            </FooterLink>
            <FooterLink href={STATIC_PAGE_URLS.terms}>
              <Translation tKey="footer.terms" />
            </FooterLink>
            <FooterLink href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</FooterLink>
          </nav>
        </div>
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
        "whitespace-nowrap transition-colors outline-none not-last:after:ml-sm not-last:after:text-meta-soft not-last:after:content-['·'] hover:text-ink focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-canvas",
        className,
      )}
      href={href}
      prefetch={false}
    >
      {children}
    </ConditionalLink>
  );
}
