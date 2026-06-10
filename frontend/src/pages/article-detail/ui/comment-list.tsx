"use client";

import type { ArticleCommentListItem } from "@/shared/api";
import { cn, formatDate, type Nullable } from "@/shared/lib";
import { Button, Translation } from "@/shared/ui";
import { useTranslations } from "next-intl";
import { useMemo, useState, type PropsWithChildren } from "react";
import { buildCommentTree, type CommentNode } from "../model/comment";
import { CommentComposer } from "./comment-composer";

type CommentListProps = {
  className?: string;
  articleId: string;
  comments: ArticleCommentListItem[];
  locale: string;
};

export function CommentList({ className, articleId, comments, locale }: CommentListProps) {
  const [activeReplyId, setActiveReplyId] = useState<Nullable<string>>(null);
  const nodes = useMemo(() => buildCommentTree(comments), [comments]);

  return (
    <div className={cn("space-y-2xl", className)}>
      <CommentComposer articleId={articleId} />

      <div className="space-y-md">
        <Translation
          className="text-caption text-meta"
          tKey="article-detail.comment-count"
          values={{ count: comments.length }}
        />

        {nodes.length === 0 ? (
          <Translation
            className="rounded-md border border-hairline-soft bg-surface-soft p-2xl text-center text-body-md text-meta"
            as="p"
            tKey="article-detail.comments-empty"
          />
        ) : (
          <ol className="space-y-md">
            {nodes.map((comment) => (
              <CommentItem
                key={comment.id}
                articleId={articleId}
                comment={comment}
                locale={locale}
                isReplyOpen={activeReplyId === comment.id}
                onToggleReply={() => handleToggleReply(comment.id)}
                onCloseReply={() => setActiveReplyId(null)}
              />
            ))}
          </ol>
        )}
      </div>
    </div>
  );

  function handleToggleReply(id: string) {
    setActiveReplyId((current) => (current === id ? null : id));
  }
}

type CommentItemProps = {
  className?: string;
  articleId: string;
  comment: CommentNode;
  locale: string;
  isReplyOpen?: boolean;
  onToggleReply?: () => void;
  onCloseReply?: () => void;
};

function CommentItem({
  className,
  articleId,
  comment,
  locale,
  isReplyOpen = false,
  onToggleReply,
  onCloseReply,
}: CommentItemProps) {
  const t = useTranslations("article-detail");

  return (
    <li className={cn("space-y-sm", className)}>
      <CommentCard comment={comment} locale={locale}>
        {onToggleReply && (
          <Button
            className="text-caption"
            variant="link"
            type="button"
            aria-expanded={isReplyOpen}
            onClick={onToggleReply}
          >
            {t("comment-reply")}
          </Button>
        )}
      </CommentCard>

      {isReplyOpen && (
        <CommentComposer
          className="ml-md"
          articleId={articleId}
          parentId={comment.id}
          autoFocus
          onSuccess={onCloseReply}
          onCancel={onCloseReply}
        />
      )}

      {comment.children.length > 0 && (
        <ol className="space-y-md border-l border-hairline pl-md">
          {comment.children.map((child) => (
            <li key={child.id}>
              <CommentCard comment={child} locale={locale} />
            </li>
          ))}
        </ol>
      )}
    </li>
  );
}

type CommentCardProps = PropsWithChildren<{
  className?: string;
  comment: CommentNode;
  locale: string;
}>;

function CommentCard({ className, comment, locale, children }: CommentCardProps) {
  const createdAt = comment.createdAt ? formatDate(comment.createdAt, locale) : "";

  return (
    <article
      className={cn(
        "rounded-md border border-hairline bg-canvas px-md py-md dark:bg-surface-soft",
        className,
      )}
    >
      <header className="flex flex-wrap items-center gap-xs text-caption text-meta">
        <strong className="text-title-sm text-ink">{comment.nickname}</strong>
        {createdAt && (
          <>
            <span aria-hidden="true">·</span>
            <time dateTime={comment.createdAt}>{createdAt}</time>
          </>
        )}
      </header>
      <p className="mt-xs text-body-sm whitespace-pre-wrap text-body">{comment.content}</p>
      {children && <div className="mt-xs flex">{children}</div>}
    </article>
  );
}
