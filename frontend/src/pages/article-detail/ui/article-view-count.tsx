"use client";

import { countArticleView } from "@/shared/api";
import { assert, safelyRunAsync, type Nullable } from "@/shared/lib";
import { Translation } from "@/shared/ui";
import { useEffect, useRef, useState } from "react";

type ArticleViewCountProps = {
  className?: string;
  articleId: string;
  initialViewCount: number;
};

type RecordedView = {
  articleId: string;
  viewCount: number;
};

export function ArticleViewCount({
  className,
  articleId,
  initialViewCount,
}: ArticleViewCountProps) {
  const [recordedView, setRecordedView] = useState<Nullable<RecordedView>>(null);
  const requestedArticleIdRef = useRef<Nullable<string>>(null);

  useEffect(() => {
    if (requestedArticleIdRef.current === articleId) {
      return;
    }
    requestedArticleIdRef.current = articleId;

    async function countAndSyncView() {
      const response = await countArticleView(articleId);
      assert(response.status === 200, "Failed to record article view");

      setRecordedView({ articleId, viewCount: response.data.viewCount });
    }

    safelyRunAsync(countAndSyncView);
  }, [articleId]);

  const viewCount =
    recordedView?.articleId === articleId ? recordedView.viewCount : initialViewCount;

  return (
    <Translation
      className={className}
      as="span"
      tKey="article-detail.view-count"
      values={{ count: viewCount }}
    />
  );
}
