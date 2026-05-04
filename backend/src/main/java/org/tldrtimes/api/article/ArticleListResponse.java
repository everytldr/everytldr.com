package org.tldrtimes.api.article;

import java.util.List;

public record ArticleListResponse(List<ArticleListItem> items, String nextCursor) {}
