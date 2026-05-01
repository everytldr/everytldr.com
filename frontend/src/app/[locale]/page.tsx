import { useTranslations } from "next-intl";
import Image from "next/image";

export default function Home() {
  const t = useTranslations("HomePage");

  return (
    <main>
      <h1>{t("title")}</h1>
      <p>{t("description")}</p>
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
