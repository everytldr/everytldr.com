# syntax=docker/dockerfile:1
FROM node:22-alpine AS base
RUN corepack enable
WORKDIR /app/frontend

FROM base AS deps
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

FROM base AS builder
COPY --from=deps /app/frontend/node_modules ./node_modules
COPY docs/openapi.json /app/docs/openapi.json
COPY frontend/ ./

ARG NEXT_PUBLIC_API_MOCKING=false
ARG NEXT_PUBLIC_ADSENSE_CLIENT_ID
ARG NEXT_PUBLIC_ADSENSE_SLOT_ARTICLE_DETAIL
ARG NEXT_PUBLIC_GA_MEASUREMENT_ID
ARG NEXT_PUBLIC_FOOTER_ABOUT_URL
ARG NEXT_PUBLIC_FOOTER_PRIVACY_URL
ARG NEXT_PUBLIC_FOOTER_TERMS_URL
ARG NEXT_PUBLIC_CONTACT_EMAIL
ARG NEXT_PUBLIC_SITE_URL
ARG NEXT_PUBLIC_NAVER_SITE_VERIFICATION
ARG NEXT_PUBLIC_BING_SITE_VERIFICATION
ENV NEXT_TELEMETRY_DISABLED=1
RUN --mount=type=secret,id=football_key \
    FOOTBALL_DATA_API_KEY="$(cat /run/secrets/football_key 2>/dev/null)" pnpm build

FROM base AS runner
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0
RUN addgroup -g 1001 -S nodejs && adduser -S nextjs -u 1001
COPY --from=builder --chown=nextjs:nodejs /app/frontend/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/frontend/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nodejs /app/frontend/public ./public
USER nextjs
EXPOSE 3000
CMD ["node", "server.js"]
