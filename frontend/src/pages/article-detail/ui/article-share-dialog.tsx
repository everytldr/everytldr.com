"use client";

import { A_SECOND, useHydrated, useIsCoarsePointer } from "@/shared/lib";
import { Button, ResponsiveDialog, toast } from "@/shared/ui";
import { Check, Copy, Share2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useState, type ComponentProps, type FC, type FocusEvent } from "react";
import Facebook from "./brands/facebook.svg";
import LinkedIn from "./brands/linkedin.svg";
import Threads from "./brands/threads.svg";
import X from "./brands/x.svg";

type ShareChannel = {
  key: "x" | "facebook" | "linkedin" | "threads";
  Icon: FC<ComponentProps<"svg">>;
  buildHref: (params: { url: string; title: string }) => string;
  prefersSystemShare?: boolean;
};

const SHARE_CHANNELS: ShareChannel[] = [
  {
    key: "x",
    Icon: X,
    buildHref: ({ url, title }) =>
      `https://x.com/intent/post?text=${encodeURIComponent(title)}&url=${encodeURIComponent(url)}`,
    prefersSystemShare: true,
  },
  {
    key: "facebook",
    Icon: Facebook,
    buildHref: ({ url }) =>
      `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`,
    prefersSystemShare: true,
  },
  {
    key: "linkedin",
    Icon: LinkedIn,
    buildHref: ({ url }) =>
      `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(url)}`,
  },
  {
    key: "threads",
    Icon: Threads,
    buildHref: ({ url, title }) =>
      `https://www.threads.net/intent/post?text=${encodeURIComponent(`${title} ${url}`)}`,
  },
];

const COPIED_RESET_DELAY = 2 * A_SECOND;

type ArticleShareDialogProps = {
  className?: string;
  isOpen: boolean;
  url: string;
  title: string;
  onClose: () => void;
};

export function ArticleShareDialog({
  className,
  isOpen,
  url,
  title,
  onClose,
}: ArticleShareDialogProps) {
  const t = useTranslations("article-detail");
  const hydrated = useHydrated();
  const isCoarsePointer = useIsCoarsePointer();
  const [isCopied, setIsCopied] = useState(false);
  const canShareViaSystem = hydrated && typeof navigator.share === "function";
  const shouldDelegateToSystem = canShareViaSystem && isCoarsePointer;

  useEffect(() => {
    if (!isCopied) {
      return;
    }

    const timer = setTimeout(() => setIsCopied(false), COPIED_RESET_DELAY);
    return () => clearTimeout(timer);
  }, [isCopied]);

  return (
    <ResponsiveDialog
      className={className}
      isOpen={isOpen}
      header={{ title: t("share") }}
      onClose={onClose}
    >
      <div className="space-y-md pb-md">
        <ul className="grid grid-cols-4 gap-xs">
          {SHARE_CHANNELS.map(({ key, Icon, buildHref, prefersSystemShare }) => (
            <li key={key} className="min-w-0">
              {prefersSystemShare && shouldDelegateToSystem ? (
                <button
                  className="flex w-full cursor-pointer flex-col items-center gap-2xs rounded-sm bg-surface-soft px-2xs py-sm text-caption text-ink transition-colors outline-none hover:bg-surface-strong focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset active:bg-surface-pressed"
                  type="button"
                  onClick={handleSystemShare}
                >
                  <Icon className="size-5 shrink-0" aria-hidden="true" />
                  <span className="w-full truncate text-center">{t(`share-channel.${key}`)}</span>
                </button>
              ) : (
                <a
                  className="flex w-full flex-col items-center gap-2xs rounded-sm bg-surface-soft px-2xs py-sm text-caption text-ink transition-colors outline-none hover:bg-surface-strong focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset active:bg-surface-pressed"
                  href={buildHref({ url, title })}
                  target="_blank"
                  rel="noreferrer"
                >
                  <Icon className="size-5 shrink-0" aria-hidden="true" />
                  <span className="w-full truncate text-center">{t(`share-channel.${key}`)}</span>
                </a>
              )}
            </li>
          ))}
        </ul>

        {canShareViaSystem && (
          <button
            className="flex h-13 w-full cursor-pointer items-center justify-center gap-sm rounded-sm bg-surface-soft text-button-md text-ink transition-colors outline-none hover:bg-surface-strong focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset active:bg-surface-pressed"
            type="button"
            onClick={handleSystemShare}
          >
            <Share2 className="size-5 shrink-0" aria-hidden="true" />
            {t("share-system")}
          </button>
        )}

        <div className="flex items-center gap-xs">
          <input
            className="h-11 min-w-0 flex-1 rounded-sm bg-canvas px-md text-body-sm text-meta ring-1 ring-hairline-strong outline-none ring-inset focus-visible:ring-2 focus-visible:ring-primary"
            value={url}
            readOnly
            aria-label={t("share-url-label")}
            onFocus={handleFocus}
          />
          <Button className="shrink-0" variant="secondary" type="button" onClick={handleCopy}>
            {isCopied ? (
              <Check className="size-md text-semantic-success" aria-hidden="true" />
            ) : (
              <Copy className="size-md" aria-hidden="true" />
            )}
            {t("share-copy")}
          </Button>
        </div>
      </div>
    </ResponsiveDialog>
  );

  function handleFocus(event: FocusEvent<HTMLInputElement>) {
    event.currentTarget.select();
  }

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(url);
      setIsCopied(true);
      toast.success(t("share-copied"));
    } catch {
      toast.error(t("share-copy-error"));
    }
  }

  async function handleSystemShare() {
    try {
      await navigator.share({ title, url });
      onClose();
    } catch {
      return;
    }
  }
}
