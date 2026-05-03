import { Link, locales } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { Container, Logo } from "@/shared/ui";
import { Menu, Search } from "lucide-react";
import { getLocale } from "next-intl/server";

const SECTIONS = [
  { label: "Tech", href: "/tech" },
  { label: "Business", href: "/business" },
  { label: "Science", href: "/science" },
  { label: "Culture", href: "/culture" },
  { label: "Politics", href: "/politics" },
  { label: "Sports", href: "/sports" },
  { label: "Opinion", href: "/opinion" },
] as const;

export async function Header() {
  const locale = await getLocale();
  const today = new Intl.DateTimeFormat(locale, {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date());

  return (
    <header className="border-b border-ink bg-paper">
      <MobileHeader />
      <DesktopHeader currentLocale={locale} today={today} />
    </header>
  );
}

function MobileHeader() {
  return (
    <Container className="grid grid-cols-3 items-center py-3 md:hidden">
      <button
        type="button"
        aria-label="Open menu"
        className="justify-self-start text-ink transition-colors hover:text-link"
      >
        <Menu className="size-6" />
      </button>
      <Link href="/" aria-label="everytldr" className="justify-self-center">
        <Logo className="[&_svg]:h-7 [&_svg]:w-auto" />
      </Link>
      <button
        type="button"
        aria-label="Search"
        className="justify-self-end text-ink transition-colors hover:text-link"
      >
        <Search className="size-5" />
      </button>
    </Container>
  );
}

type DesktopHeaderProps = {
  currentLocale: string;
  today: string;
};

function DesktopHeader({ currentLocale, today }: DesktopHeaderProps) {
  return (
    <div className="hidden md:block">
      <div className="border-b border-hairline">
        <Container className="grid grid-cols-3 items-center py-2">
          <div className="justify-self-start">
            <button
              type="button"
              aria-label="Search"
              className="text-ink transition-colors hover:text-link"
            >
              <Search className="size-5" />
            </button>
          </div>
          <nav aria-label="Languages" className="justify-self-center">
            <ul className="flex items-center gap-6 font-mono text-xs tracking-[0.092em] text-ink uppercase">
              {locales.map((code) => (
                <li key={code}>
                  <Link
                    href="/"
                    locale={code}
                    className={cn(
                      "transition-colors hover:text-link",
                      code === currentLocale && "font-bold",
                    )}
                  >
                    {code}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
          <div />
        </Container>
      </div>

      <Container className="grid grid-cols-3 items-end pt-6 pb-4">
        <div className="justify-self-start font-display leading-tight text-page-ink">
          <p className="text-sm">{today}</p>
          <Link href="/" className="text-sm underline transition-colors hover:text-link">
            Today&apos;s Paper
          </Link>
        </div>
        <Link href="/" aria-label="everytldr" className="justify-self-center">
          <Logo className="[&_svg]:h-12 [&_svg]:w-auto" />
        </Link>
        <div />
      </Container>

      <div className="border-t border-hairline">
        <Container className="py-3">
          <nav aria-label="Sections">
            <ul className="flex justify-center gap-8 font-ui text-base text-page-ink">
              {SECTIONS.map(({ label, href }) => (
                <li key={href}>
                  <Link href={href} className="transition-colors hover:text-link hover:underline">
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        </Container>
      </div>
    </div>
  );
}
