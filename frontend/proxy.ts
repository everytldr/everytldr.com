import { defaultLocale, isLocale, routing } from "@/shared/i18n";
import createMiddleware from "next-intl/middleware";
import { type NextRequest, NextResponse } from "next/server";

const intlMiddleware = createMiddleware(routing);

export const config = {
  matcher: [
    "/((?!api|trpc|_next|_vercel|healthz|robots\\.txt|sitemap\\.xml|ads\\.txt|favicon\\.ico|icon\\.svg|core/sitemap\\.xml|news/sitemap\\.xml|briefings/sitemap/|articles/sitemap/|mockServiceWorker\\.js|logo-square\\.png|og-image\\.png).*)",
    "/api/:path*",
    "/feed.xml",
    "/:path*/feed.xml",
  ],
};

export default function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const [, firstSegment] = pathname.split("/");

  if (pathname.startsWith("/api/")) {
    // NOTE: 백엔드는 클라이언트 IP 헤더를 요구한다. 프로덕션에서는 Vercel이 붙여주지만 로컬에는 붙여줄 프록시가 없다.
    if (process.env.NODE_ENV === "development") {
      const headers = new Headers(request.headers);
      headers.set("x-forwarded-for", "127.0.0.1");

      return NextResponse.next({ request: { headers } });
    }

    return NextResponse.next();
  }

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
