import { Logo } from "@/shared/ui";

export function HomePage() {
  return (
    <main className="grid min-h-screen grid-cols-1 md:grid-cols-2">
      <section className="flex items-center justify-center bg-white p-16">
        <Logo />
      </section>
      <section className="dark flex items-center justify-center bg-[#1a1a1a] p-16">
        <Logo />
      </section>
    </main>
  );
}
