import { ADSENSE_CLIENT_ID } from "@/shared/config";

const GOOGLE_CERTIFICATION_AUTHORITY_ID = "f08c47fec0942fa0";

export function GET() {
  const publisherId = ADSENSE_CLIENT_ID.replace(/^ca-/, "");
  const body = `google.com, ${publisherId}, DIRECT, ${GOOGLE_CERTIFICATION_AUTHORITY_ID}\n`;

  return new Response(body, {
    headers: { "content-type": "text/plain" },
  });
}
