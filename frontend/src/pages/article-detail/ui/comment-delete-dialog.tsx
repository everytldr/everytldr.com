"use client";

import { cn } from "@/shared/lib";
import { ResponsiveDialog } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { CommentVerifyForm } from "./comment-verify-form";

type CommentDeleteDialogProps = {
  className?: string;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (password: string) => Promise<void>;
};

export function CommentDeleteDialog({
  className,
  isOpen,
  onClose,
  onConfirm,
}: CommentDeleteDialogProps) {
  const t = useTranslations("article-detail");

  return (
    <ResponsiveDialog
      className={cn(className)}
      isOpen={isOpen}
      header={{ title: t("comment-delete-title"), description: t("comment-delete-description") }}
      onClose={onClose}
    >
      <CommentVerifyForm
        confirmTKey="article-detail.comment-delete-confirm"
        pendingTKey="article-detail.comment-delete-deleting"
        errorTKey="article-detail.comment-delete-error"
        destructive
        onCancel={onClose}
        onSubmit={onConfirm}
      />
    </ResponsiveDialog>
  );
}
