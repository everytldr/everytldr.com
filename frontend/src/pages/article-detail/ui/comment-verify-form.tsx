"use client";

import { ApiError } from "@/shared/api";
import { cn, type Nullable } from "@/shared/lib";
import { Button, Input, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useState, type ComponentProps, type SubmitEvent } from "react";
import { MAX_PASSWORD_LENGTH, MIN_PASSWORD_LENGTH } from "./constants";

type TranslationTKey = ComponentProps<typeof Translation>["tKey"];

type CommentVerifyFormProps = {
  className?: string;
  confirmTKey: TranslationTKey;
  pendingTKey: TranslationTKey;
  errorTKey: TranslationTKey;
  destructive?: boolean;
  onCancel: () => void;
  onSubmit: (password: string) => Promise<void>;
};

export function CommentVerifyForm({
  className,
  confirmTKey,
  pendingTKey,
  errorTKey,
  destructive,
  onCancel,
  onSubmit,
}: CommentVerifyFormProps) {
  const t = useTranslations("article-detail");
  const [password, setPassword] = useState("");
  const [shownErrorTKey, setShownErrorTKey] = useState<Nullable<TranslationTKey>>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  return (
    <form className={cn("space-y-md", className)} onSubmit={handleSubmit}>
      <Input
        className="w-full"
        name="password"
        type="password"
        value={password}
        maxLength={MAX_PASSWORD_LENGTH}
        placeholder={t("comment-password")}
        autoFocus
        aria-invalid={Boolean(shownErrorTKey) || undefined}
        onChange={(event) => setPassword(event.target.value)}
      />
      {shownErrorTKey && (
        <Translation className="text-caption text-semantic-error" as="p" tKey={shownErrorTKey} />
      )}
      <div className="flex flex-col-reverse gap-sm pc:flex-row pc:justify-end">
        <Button variant="ghost" type="button" onClick={onCancel}>
          <Translation tKey="article-detail.comment-cancel" />
        </Button>
        <Button
          variant={destructive ? "destructive" : "primary"}
          type="submit"
          disabled={isSubmitting || password.length < MIN_PASSWORD_LENGTH}
        >
          <Translation tKey={isSubmitting ? pendingTKey : confirmTKey} />
        </Button>
      </div>
    </form>
  );

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (password.length < MIN_PASSWORD_LENGTH) {
      return;
    }
    setIsSubmitting(true);
    setShownErrorTKey(null);
    try {
      await onSubmit(password);
    } catch (error) {
      setShownErrorTKey(
        error instanceof ApiError && error.status === 403
          ? "article-detail.comment-password-invalid"
          : errorTKey,
      );
      setIsSubmitting(false);
    }
  }
}
