package com.everytldr.enricher.processing;

public record ArticleEnrichmentProcessingResult(
    Long jobId, ArticleEnrichmentProcessingStatus status) {}
