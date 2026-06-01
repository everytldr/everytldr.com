package org.everytldr.enricher.enrichment;

public interface ArticleEnrichmentClient {
  ArticleEnrichmentResult enrich(ArticleEnrichmentRequest request);
}
