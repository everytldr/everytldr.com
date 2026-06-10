import { type Optional } from "@/shared/lib";

let initBrowserMocksPromise: Optional<Promise<void>>;
const isMockingEnabled = process.env.NEXT_PUBLIC_API_MOCKING !== "false";

export async function initBrowserMocks(): Promise<void> {
  if (!isMockingEnabled) {
    return;
  }

  initBrowserMocksPromise ??= enableBrowserMocks();
  await initBrowserMocksPromise;
}

async function enableBrowserMocks(): Promise<void> {
  const { worker } = await import("./browser");
  await worker.start({
    onUnhandledRequest: "bypass",
  });
}
