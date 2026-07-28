"use client";

import { cn } from "@/shared/lib";
import { Button, IconButton } from "@/shared/ui";
import { Share2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { ArticleShareDialog } from "./article-share-dialog";

type ArticleShareButtonProps = {
  className?: string;
  variant: "icon" | "labeled";
  url: string;
  title: string;
};

export function ArticleShareButton({ className, variant, url, title }: ArticleShareButtonProps) {
  const t = useTranslations("article-detail");
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className={cn("shrink-0", className)}>
      {variant === "icon" ? (
        <IconButton
          className="size-11 pc:size-9"
          Icon={Share2}
          aria-label={t("share")}
          onClick={handleOpen}
        />
      ) : (
        <Button variant="secondary" type="button" onClick={handleOpen}>
          <Share2 className="size-md" aria-hidden="true" />
          {t("share")}
        </Button>
      )}
      <ArticleShareDialog isOpen={isOpen} url={url} title={title} onClose={handleClose} />
    </div>
  );

  function handleOpen() {
    setIsOpen(true);
  }

  function handleClose() {
    setIsOpen(false);
  }
}
