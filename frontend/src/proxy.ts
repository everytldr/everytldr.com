import { routing } from "@/i18n/routing";
import createMiddleware from "next-intl/middleware";

export const config = {
  matcher: "/((?!api|trpc|_next|_vercel|.*\\..*).*)",
};

export default createMiddleware(routing);
