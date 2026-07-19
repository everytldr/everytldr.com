"use client";

import { countArticleView } from "@/shared/api";
import type { Nullable } from "@/shared/lib";
import { useEffect, useRef } from "react";

type ArticleViewTrackerProps = {
  articleId: string;
};

export function ArticleViewTracker({ articleId }: ArticleViewTrackerProps) {
  const recordedArticleIdRef = useRef<Nullable<string>>(null);

  useEffect(() => {
    if (recordedArticleIdRef.current === articleId) {
      return;
    }
    recordedArticleIdRef.current = articleId;
    countArticleView(articleId).catch(() => undefined);
  }, [articleId]);

  return null;
}
