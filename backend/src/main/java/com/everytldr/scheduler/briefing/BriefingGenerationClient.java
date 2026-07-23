package com.everytldr.scheduler.briefing;

import java.util.List;

public interface BriefingGenerationClient {
  List<BriefingGenerationResult> generate(BriefingGenerationRequest request);
}
