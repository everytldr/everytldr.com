"use client";

import {
  useDeleteArticleComment,
  useEditArticleComment,
  useVerifyArticleCommentPassword,
} from "@/shared/api";
import { useRouter } from "@/shared/i18n";
import { cn, formatDate } from "@/shared/lib";
import { Button, IconButton, ResponsiveActionMenu, toast, Translation } from "@/shared/ui";
import { Ellipsis, Pencil, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";
import type { CommentNode } from "../model/comment";
import { CommentDeleteDialog } from "./comment-delete-dialog";
import { CommentEditDialog } from "./comment-edit-dialog";

type CommentCardProps = {
  className?: string;
  articleId: string;
  comment: CommentNode;
  locale: string;
  isReplyOpen?: boolean;
  onToggleReply?: () => void;
};

export function CommentCard({
  className,
  articleId,
  comment,
  locale,
  isReplyOpen = false,
  onToggleReply,
}: CommentCardProps) {
  const t = useTranslations("article-detail");
  const router = useRouter();
  const verify = useVerifyArticleCommentPassword();
  const edit = useEditArticleComment();
  const remove = useDeleteArticleComment();

  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editSession, setEditSession] = useState(0);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [deleteSession, setDeleteSession] = useState(0);

  const createdAt = comment.createdAt ? formatDate(comment.createdAt, locale) : "";

  return (
    <article
      className={cn(
        "rounded-md border border-hairline bg-canvas px-md py-md dark:bg-surface-soft",
        className,
      )}
    >
      {comment.deletedAt ? (
        <Translation
          className="text-body-sm text-meta italic"
          as="p"
          tKey="article-detail.comment-deleted"
        />
      ) : (
        <>
          <header className="flex items-center gap-sm text-caption text-meta">
            <div className="flex flex-1 flex-wrap gap-xs self-stretch">
              <strong className="text-title-sm text-ink">{comment.nickname}</strong>
              {createdAt && (
                <>
                  <span aria-hidden="true">·</span>
                  <time dateTime={comment.createdAt}>{createdAt}</time>
                </>
              )}
              {comment.editedAt && <Translation as="span" tKey="article-detail.comment-edited" />}
            </div>
            <ResponsiveActionMenu
              title={t("comment-actions-title")}
              actions={[
                { key: "edit", label: t("comment-edit"), Icon: Pencil, onSelect: openEdit },
                {
                  key: "delete",
                  label: t("comment-delete"),
                  Icon: Trash2,
                  onSelect: openDelete,
                  variant: "destructive",
                },
              ]}
              renderTrigger={({ open }) => (
                <IconButton
                  className="shrink-0"
                  Icon={Ellipsis}
                  aria-label={t("comment-actions")}
                  onClick={open}
                />
              )}
            />
          </header>

          <p className="text-body-sm whitespace-pre-wrap text-body">{comment.content}</p>
          {onToggleReply && (
            <div className="mt-xs flex">
              <Button
                className="text-caption"
                variant="link"
                type="button"
                aria-expanded={isReplyOpen}
                onClick={onToggleReply}
              >
                <Translation tKey="article-detail.comment-reply" />
              </Button>
            </div>
          )}

          <CommentEditDialog
            key={editSession}
            isOpen={isEditOpen}
            initialContent={comment.content ?? ""}
            onClose={() => setIsEditOpen(false)}
            onVerify={handleVerify}
            onSave={handleSaveEdit}
          />
          <CommentDeleteDialog
            key={deleteSession}
            isOpen={isDeleteOpen}
            onClose={() => setIsDeleteOpen(false)}
            onConfirm={handleDelete}
          />
        </>
      )}
    </article>
  );

  function openEdit() {
    setEditSession((session) => session + 1);
    setIsEditOpen(true);
  }

  function openDelete() {
    setDeleteSession((session) => session + 1);
    setIsDeleteOpen(true);
  }

  async function handleVerify(password: string) {
    await verify.mutateAsync({ articleId, commentId: comment.id, data: { password } });
  }

  async function handleSaveEdit(content: string, password: string) {
    await edit.mutateAsync({ articleId, commentId: comment.id, data: { content, password } });
    setIsEditOpen(false);
    router.refresh();
    toast.success(t("comment-edit-success"));
  }

  async function handleDelete(password: string) {
    await remove.mutateAsync({ articleId, commentId: comment.id, data: { password } });
    setIsDeleteOpen(false);
    router.refresh();
    toast.success(t("comment-delete-success"));
  }
}
