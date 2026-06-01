package com.everytldr.ingestor.provider.guardian;

import java.util.List;

/**
 * Guardian Content API search response fields used by the ingestor.
 *
 * <pre>
 * {
 *   "response": {
 *     "status": "ok",
 *     "userTier": "developer",
 *     "total": 123,
 *     "startIndex": 1,
 *     "pageSize": 10,
 *     "currentPage": 1,
 *     "pages": 13,
 *     "orderBy": "newest",
 *     "results": [
 *       {
 *         "id": "football/2026/may/04/example",
 *         "type": "article",
 *         "sectionId": "football",
 *         "sectionName": "Football",
 *         "webPublicationDate": "2026-05-04T10:15:30Z",
 *         "webTitle": "Example title",
 *         "webUrl": "https://www.theguardian.com/football/2026/may/04/example",
 *         "apiUrl": "https://content.guardianapis.com/football/2026/may/04/example",
 *         "fields": {
 *           "thumbnail": "https://media.guim.co.uk/..."
 *         }
 *       }
 *     ]
 *   }
 * }
 * </pre>
 *
 * <p>The {@code fields} object is optional and is returned only when requested with parameters such
 * as {@code show-fields=thumbnail}. This DTO intentionally models only the fields currently mapped
 * to {@code CollectedArticle}.
 */
public record GuardianSearchResponse(Response response) {

  public record Response(List<Result> results) {}

  public record Result(String webPublicationDate, String webUrl, Fields fields) {}

  public record Fields(String thumbnail) {}
}
