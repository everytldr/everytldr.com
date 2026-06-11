package com.everytldr.enricher.enrichment;

import java.util.List;

public interface EnrichmentClient {
  List<EnrichmentResult> enrich(EnrichmentRequest request);
}
