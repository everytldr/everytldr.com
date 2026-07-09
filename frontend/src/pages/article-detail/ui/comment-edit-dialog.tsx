"use client";

import { cn, type Nullable } from "@/shared/lib";
import { Button, ResponsiveDialog, Textarea, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useState, type SubmitEvent } from "react";
import { CommentVerifyForm } from "./comment-verify-form";

const MAX_CONTENT_LENGTH = 5000;

type EditStep = "verify" | "edit";

type CommentEditDialogProps = {
  className?: string;
  isOpen: boolean;
  initialContent: string;
  onClose: () => void;
  onVerify: (password: string) => Promise<void>;
  onSave: (content: string, password: string) => Promise<void>;
};

export function CommentEditDialog({
  className,
  isOpen,
  initialContent,
  onClose,
  onVerify,
  onSave,
}: CommentEditDialogProps) {
  const t = useTranslations("article-detail");
  const [step, setStep] = useState<EditStep>("verify");
  const [password, setPassword] = useState("");
  const [content, setContent] = useState(initialContent);
  const [errorTKey, setErrorTKey] = useState<Nullable<"article-detail.comment-edit-error">>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  return (
    <ResponsiveDialog
      className={cn(className)}
      isOpen={isOpen}
      header={{
        title: t("comment-edit-title"),
        description:
          step === "verify" ? t("comment-edit-verify-description") : t("comment-edit-description"),
      }}
      onClose={onClose}
    >
      {step === "verify" ? (
        <CommentVerifyForm
          confirmTKey="article-detail.comment-password-confirm"
          pendingTKey="article-detail.comment-password-verifying"
          errorTKey="article-detail.comment-edit-error"
          onCancel={onClose}
          onSubmit={handleVerified}
        />
      ) : (
        <form className="space-y-md px-md py-md pc:p-0" onSubmit={handleSaveSubmit}>
          <Textarea
            className="block max-h-64 min-h-32 w-full"
            name="content"
            value={content}
            maxLength={MAX_CONTENT_LENGTH}
            autoFocus
            onChange={(event) => setContent(event.target.value)}
          />
          {errorTKey && (
            <Translation className="text-caption text-semantic-error" as="p" tKey={errorTKey} />
          )}
          <div className="flex flex-col-reverse gap-sm pc:flex-row pc:justify-end">
            <Button variant="ghost" type="button" onClick={onClose}>
              <Translation tKey="article-detail.comment-cancel" />
            </Button>
            <Button
              variant="primary"
              type="submit"
              disabled={isSubmitting || content.trim() === ""}
            >
              <Translation
                tKey={
                  isSubmitting
                    ? "article-detail.comment-edit-saving"
                    : "article-detail.comment-edit-save"
                }
              />
            </Button>
          </div>
        </form>
      )}
    </ResponsiveDialog>
  );

  async function handleVerified(nextPassword: string) {
    await onVerify(nextPassword);
    setPassword(nextPassword);
    setStep("edit");
  }

  async function handleSaveSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = content.trim();
    if (!trimmed) {
      return;
    }
    setIsSubmitting(true);
    setErrorTKey(null);
    try {
      await onSave(trimmed, password);
    } catch {
      setErrorTKey("article-detail.comment-edit-error");
      setIsSubmitting(false);
    }
  }
}
