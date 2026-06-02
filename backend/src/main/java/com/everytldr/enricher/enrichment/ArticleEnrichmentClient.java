package com.everytldr.enricher.enrichment;

public interface ArticleEnrichmentClient {
  ArticleEnrichmentResult enrich(ArticleEnrichmentRequest request);
}
