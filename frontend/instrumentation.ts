export async function register() {
  const isMockingEnabled = process.env.NEXT_PUBLIC_API_MOCKING !== "false";
  const isNodeRuntime = process.env.NEXT_RUNTIME === "nodejs";

  if (!isNodeRuntime || !isMockingEnabled) {
    return;
  }

  const { initServerMocks } = await import("./src/shared/api/mocks/server-init");
  await initServerMocks();
}
