import { routing } from "@/shared/i18n";
import createMiddleware from "next-intl/middleware";

export const config = {
  matcher: "/((?!api|trpc|_next|_vercel|health|.*\\..*).*)",
};

export default createMiddleware(routing);
