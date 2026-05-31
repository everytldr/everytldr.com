import { type Optional } from "@/shared/lib";
import { server } from "./server";

let initServerMocksPromise: Optional<Promise<void>>;
const isMockingEnabled = process.env.NEXT_PUBLIC_API_MOCKING !== "false";

export async function initServerMocks(): Promise<void> {
  if (!isMockingEnabled) {
    return;
  }

  initServerMocksPromise ??= enableServerMocks();
  await initServerMocksPromise;
}

async function enableServerMocks(): Promise<void> {
  server.listen({
    onUnhandledRequest: "bypass",
  });
}
