import { http, HttpResponse } from "msw";

const getHealth = () =>
  HttpResponse.json({
    status: "ok",
    mocked: true,
    timestamp: new Date().toISOString(),
  });

export const handlers = [http.get("/api/health", getHealth), http.get("*/api/health", getHealth)];
