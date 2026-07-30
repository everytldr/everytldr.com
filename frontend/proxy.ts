import { defaultLocale, isLocale, routing } from "@/shared/i18n";
import createMiddleware from "next-intl/middleware";
import { type NextRequest, NextResponse } from "next/server";

const intlMiddleware = createMiddleware(routing);

// NOTE: 확장자가 붙은 경로를 전부 제외하면 /wp-login.php 같은 요청이 [locale] 세그먼트로 흘러들어가 500이 된다.
//       실제로 서빙하는 루트 경로(route handler, public 파일)만 제외하고, 나머지는 next-intl이 처리해 정상 404가 되게 한다.
//       public/에 파일을 추가하면 이 목록에도 추가해야 한다.
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
