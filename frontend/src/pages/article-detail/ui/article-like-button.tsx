"use client";

import {
  useGetMyArticleLike,
  useLikeArticle,
  useUnlikeArticle,
  type getMyArticleLikeResponse,
} from "@/shared/api";
import { cn } from "@/shared/lib";
import { Button, Skeleton, toast } from "@/shared/ui";
import { useQueryClient } from "@tanstack/react-query";
import { Heart } from "lucide-react";
import { useTranslations } from "next-intl";

type ArticleLikeButtonProps = {
  className?: string;
  articleId: string;
};

export function ArticleLikeButton({ className, articleId }: ArticleLikeButtonProps) {
  const t = useTranslations("article-detail");
  const queryClient = useQueryClient();
  const { data, isPending, queryKey } = useGetMyArticleLike(articleId);
  const like = useLikeArticle();
  const unlike = useUnlikeArticle();

  if (data?.status !== 200) {
    return null;
  }

  if (isPending) {
    return <ArticleLikeButtonSkeleton className={className} />;
  }

  const likeState = data.data;

  return (
    <Button
      className={cn(
        "min-w-30 gap-xs",
        likeState.likedByReader && "border-like-active text-like-active hover:text-like-active",
        className,
      )}
      variant="secondary"
      type="button"
      disabled={like.isPending || unlike.isPending}
      aria-pressed={likeState.likedByReader}
      onClick={handleToggle}
    >
      <Heart
        className={cn(
          "size-md",
          likeState.likedByReader ? "fill-like-active text-like-active" : "text-like-inactive",
        )}
        aria-hidden="true"
      />
      <span className="text-caption-mono">{likeState.likeCount ?? 0}</span>
      <span className="sr-only">{likeState.likedByReader ? t("unlike") : t("like")}</span>
    </Button>
  );

  async function handleToggle() {
    try {
      const response = likeState.likedByReader
        ? await unlike.mutateAsync({ articleId })
        : await like.mutateAsync({ articleId });

      if (response.status !== 200) {
        throw new Error("Failed to toggle article like");
      }

      queryClient.setQueryData<getMyArticleLikeResponse>(queryKey, response);
    } catch {
      toast.error(t("like-error"));
    }
  }
}

type ArticleLikeButtonSkeletonProps = {
  className?: string;
};

export function ArticleLikeButtonSkeleton({ className }: ArticleLikeButtonSkeletonProps) {
  return <Skeleton className={cn("h-11 w-30 rounded-sm", className)} />;
}
