import { defaultLocale, isLocale, routing } from "@/shared/i18n";
import createMiddleware from "next-intl/middleware";
import { type NextRequest, NextResponse } from "next/server";

const intlMiddleware = createMiddleware(routing);

export const config = {
  matcher: ["/((?!api|trpc|_next|_vercel|healthz|.*\\..*).*)", "/feed.xml", "/:path*/feed.xml"],
};

export default function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const [, firstSegment] = pathname.split("/");

  if (pathname.endsWith("/feed.xml")) {
    if (isLocale(firstSegment)) {
      return NextResponse.next();
    }

    const url = request.nextUrl.clone();
    url.pathname = `/${defaultLocale}${pathname}`;
    return NextResponse.redirect(url, 308);
  }

  return intlMiddleware(request);
}
