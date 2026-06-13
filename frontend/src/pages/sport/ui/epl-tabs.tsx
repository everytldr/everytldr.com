"use client";

import { EplTabSlug } from "@/shared/config";
import { Link } from "@/shared/i18n";
import { buildEplTabUrl } from "@/shared/lib";
import { Tabs, TabsList, TabsTrigger, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";

type EplTabsProps = {
  className?: string;
  activeTab: EplTabSlug;
};

export function EplTabs({ className, activeTab }: EplTabsProps) {
  const t = useTranslations("epl.aria-label");

  return (
    <Tabs className={className} value={activeTab}>
      <TabsList aria-label={t("tabs")}>
        <TabsTrigger value={EplTabSlug.News} asChild>
          <Link href={buildEplTabUrl(EplTabSlug.News)}>
            <Translation tKey="epl.tab.news" />
          </Link>
        </TabsTrigger>
        <TabsTrigger value={EplTabSlug.Record} asChild>
          <Link href={buildEplTabUrl(EplTabSlug.Record)}>
            <Translation tKey="epl.tab.record" />
          </Link>
        </TabsTrigger>
      </TabsList>
    </Tabs>
  );
}
