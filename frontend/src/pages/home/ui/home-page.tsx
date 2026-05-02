import { Button, Translation } from "@/shared/ui";
import Image from "next/image";

export function HomePage() {
  return (
    <main>
      <Translation path="common.test" />
      <Image
        className="text-black"
        src="/next.svg"
        alt="Next.js logo"
        width={100}
        height={20}
        priority
      />
      <Image
        className="dark:invert"
        src="/vercel.svg"
        alt="Vercel logomark"
        width={16}
        height={16}
      />
      <section className="flex flex-wrap gap-3">
        <Button>Default</Button>
        <Button variant="secondary">Secondary</Button>
        <Button variant="outline">Outline</Button>
        <Button variant="destructive">Destructive</Button>
        <Button variant="ghost">Ghost</Button>
        <Button variant="link">Link</Button>
      </section>
    </main>
  );
}
