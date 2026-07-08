"use client";

import { useCreateArticleComment } from "@/shared/api";
import { useRouter } from "@/shared/i18n";
import { cn, type Nullable } from "@/shared/lib";
import { Button, Input, Textarea, toast, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useState, type ComponentProps, type SubmitEvent } from "react";

const MAX_CONTENT_LENGTH = 5000;
const MAX_NICKNAME_LENGTH = 50;
const MAX_PASSWORD_LENGTH = 100;
const MIN_PASSWORD_LENGTH = 4;

enum ComposerError {
  Required = "required",
  PasswordMin = "password-min",
}

type CommentComposerProps = {
  className?: string;
  articleId: string;
  parentId?: string;
  autoFocus?: boolean;
  onSuccess?: () => void;
  onCancel?: () => void;
};

export function CommentComposer({
  className,
  articleId,
  parentId,
  autoFocus,
  onSuccess,
  onCancel,
}: CommentComposerProps) {
  const t = useTranslations("article-detail");
  const router = useRouter();
  const create = useCreateArticleComment();

  const [content, setContent] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<Nullable<ComposerError>>(null);

  const contentInvalid = error === ComposerError.Required && content.trim() === "";
  const nicknameInvalid = error === ComposerError.Required && nickname.trim() === "";
  const passwordInvalid =
    error === ComposerError.PasswordMin || (error === ComposerError.Required && password === "");

  return (
    <form
      className={cn(
        "overflow-hidden rounded-md border border-hairline bg-canvas shadow-raised dark:bg-surface-soft dark:shadow-none",
        className,
      )}
      onSubmit={handleSubmit}
    >
      <Textarea
        className={cn(
          "block max-h-48 rounded-b-none inset-ring-0",
          parentId ? "min-h-24" : "min-h-36",
        )}
        name="content"
        value={content}
        maxLength={MAX_CONTENT_LENGTH}
        placeholder={t(parentId ? "comment-reply-placeholder" : "comment-placeholder")}
        autoFocus={autoFocus}
        aria-invalid={contentInvalid}
        onChange={(event) => setContent(event.target.value)}
      />

      <div className="border-t border-hairline-soft bg-canvas p-sm dark:bg-surface-soft">
        <div className="grid gap-sm pc:grid-cols-[minmax(0,12rem)_minmax(0,18rem)_1fr]">
          <Input
            name="nickname"
            value={nickname}
            maxLength={MAX_NICKNAME_LENGTH}
            placeholder={t("comment-nickname")}
            aria-invalid={nicknameInvalid || undefined}
            onChange={(event) => setNickname(event.target.value)}
          />
          <Input
            name="password"
            type="password"
            value={password}
            maxLength={MAX_PASSWORD_LENGTH}
            placeholder={t("comment-password")}
            aria-invalid={passwordInvalid || undefined}
            onChange={(event) => setPassword(event.target.value)}
          />
          <div className="flex flex-wrap items-center justify-between gap-sm">
            {!!content.length && (
              <Translation
                className="h-full shrink-0 text-caption-mono text-meta"
                as="p"
                tKey="article-detail.comment-char-count"
                values={{ count: content.length }}
              />
            )}
            <div className="ml-auto flex items-center gap-sm">
              {onCancel && (
                <Button variant="ghost" type="button" onClick={onCancel}>
                  <Translation tKey="article-detail.comment-reply-cancel" />
                </Button>
              )}
              <Button
                variant="secondary"
                type="submit"
                disabled={
                  create.isPending ||
                  content.trim() === "" ||
                  nickname.trim() === "" ||
                  password.length < MIN_PASSWORD_LENGTH
                }
              >
                <Translation
                  tKey={getSubmitTKey({
                    isPending: create.isPending,
                    isReply: Boolean(parentId),
                  })}
                />
              </Button>
            </div>
          </div>
        </div>
        {error && (
          <Translation
            className="mt-xs text-caption text-semantic-error"
            as="p"
            tKey={getErrorTKey(error)}
          />
        )}
      </div>
    </form>
  );

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!content.trim() || !nickname.trim() || !password) {
      setError(ComposerError.Required);
      return;
    }

    if (password.length < MIN_PASSWORD_LENGTH) {
      setError(ComposerError.PasswordMin);
      return;
    }

    try {
      const response = await create.mutateAsync({
        articleId,
        data: {
          content: content.trim(),
          nickname: nickname.trim(),
          password,
          parentId: parentId ?? null,
        },
      });

      if (response.status !== 201) {
        throw new Error("Failed to post comment");
      }

      setContent("");
      setNickname("");
      setPassword("");
      setError(null);
      router.refresh();
      onSuccess?.();
    } catch {
      toast.error(t("comment-submit-error"));
    }
  }
}

function getErrorTKey(error: ComposerError): ComponentProps<typeof Translation>["tKey"] {
  switch (error) {
    case ComposerError.Required:
      return "article-detail.comment-error-required";
    case ComposerError.PasswordMin:
      return "article-detail.comment-error-password-min";
  }
}

function getSubmitTKey({
  isPending,
  isReply,
}: {
  isPending: boolean;
  isReply: boolean;
}): ComponentProps<typeof Translation>["tKey"] {
  if (isPending) {
    return isReply
      ? "article-detail.comment-reply-submitting"
      : "article-detail.comment-submitting";
  }

  return isReply ? "article-detail.comment-reply-submit" : "article-detail.comment-submit";
}
