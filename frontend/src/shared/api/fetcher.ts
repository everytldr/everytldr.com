import { isBrowser } from "@/shared/lib";

// NOTE: MSW can't intercept fetches inside `use cache`. Routing them to our own origin lets the next.config rewrite proxy re-issue the request where MSW does intercept.
const SERVER_BASE_URL =
  process.env.NEXT_PUBLIC_API_MOCKING !== "false"
    ? "http://localhost:3000"
    : (process.env.BACKEND_URL ?? "http://localhost:8080");

const BASE_URL = isBrowser() ? "" : SERVER_BASE_URL;

export class ApiError extends Error {
  status: number;
  data: unknown;

  constructor(status: number, data: unknown, message?: string) {
    super(message ?? `Request failed with status ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

// NOTE: orval-generated client mutator. Do not call directly.
export const _fetch = async <T>(url: string, init?: RequestInit): Promise<T> => {
  const response = await fetch(`${BASE_URL}${url}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
    credentials: "include",
  });

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const body = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    throw new ApiError(response.status, body);
  }

  return {
    status: response.status,
    data: body,
    headers: response.headers,
  } as T;
};
