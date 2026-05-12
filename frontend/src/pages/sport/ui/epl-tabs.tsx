"use client";

import { EplPageTab } from "@/shared/config";
import { Link } from "@/shared/i18n";
import { buildEplTabUrl } from "@/shared/lib";
import { Tabs, TabsList, TabsTrigger, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";

type EplTabsProps = {
  className?: string;
  activeTab: EplPageTab;
};

export function EplTabs({ className, activeTab }: EplTabsProps) {
  const t = useTranslations("epl.aria-label");

  return (
    <Tabs className={className} value={activeTab}>
      <TabsList aria-label={t("tabs")}>
        <TabsTrigger value={EplPageTab.News} asChild>
          <Link href={buildEplTabUrl(EplPageTab.News)}>
            <Translation tKey="epl.tab.news" />
          </Link>
        </TabsTrigger>
        <TabsTrigger value={EplPageTab.Record} asChild>
          <Link href={buildEplTabUrl(EplPageTab.Record)}>
            <Translation tKey="epl.tab.record" />
          </Link>
        </TabsTrigger>
      </TabsList>
    </Tabs>
  );
}
