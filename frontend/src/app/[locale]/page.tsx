import Translation from "@/components/Translation";
import Image from "next/image";

export default function Home() {
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
    </main>
  );
}
