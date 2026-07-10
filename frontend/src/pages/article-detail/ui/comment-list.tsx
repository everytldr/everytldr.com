"use client";

import type { ArticleCommentListItem } from "@/shared/api";
import { cn, type Nullable } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { useMemo, useState } from "react";
import { buildCommentTree, type CommentNode } from "../model/comment";
import { CommentCard } from "./comment-card";
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

      <div className="space-y-xs">
        <Translation
          className="inline-block text-caption text-meta"
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
                onToggleReply={comment.deletedAt ? undefined : () => handleToggleReply(comment.id)}
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
  return (
    <li className={cn("space-y-sm", className)}>
      <CommentCard
        articleId={articleId}
        comment={comment}
        locale={locale}
        isReplyOpen={isReplyOpen}
        onToggleReply={onToggleReply}
      />

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
              <CommentCard articleId={articleId} comment={child} locale={locale} />
            </li>
          ))}
        </ol>
      )}
    </li>
  );
}
