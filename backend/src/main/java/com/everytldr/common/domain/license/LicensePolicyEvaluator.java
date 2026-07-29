package com.everytldr.common.domain.license;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LicensePolicyEvaluator {
  private static final Set<LicenseCode> PUBLISHABLE_TRANSFORMED_TEXT_CODES =
      Set.of(
          LicenseCode.CC0,
          LicenseCode.PUBLIC_DOMAIN,
          LicenseCode.CC_BY,
          LicenseCode.CC_BY_NC,
          LicenseCode.CC_BY_SA,
          LicenseCode.CC_BY_NC_SA);
  private static final Set<LicenseCode> SHARE_ALIKE_REQUIRED_CODES =
      Set.of(LicenseCode.CC_BY_SA, LicenseCode.CC_BY_NC_SA);
  // INFO: 브리핑은 여러 기사를 하나로 합친 2차 저작물이다. CC-BY-NC-SA는 CC-BY-SA와 상호 비호환이라 한 브리핑에 섞을 수 없으므로 소스에서 제외한다.
  private static final Set<LicenseCode> BRIEFING_SOURCE_CODES =
      Set.of(
          LicenseCode.CC0,
          LicenseCode.PUBLIC_DOMAIN,
          LicenseCode.CC_BY,
          LicenseCode.CC_BY_NC,
          LicenseCode.CC_BY_SA);
  private static final Set<LicenseCode> COMMERCIAL_ALLOWED_CODES =
      Set.of(
          LicenseCode.CC0,
          LicenseCode.PUBLIC_DOMAIN,
          LicenseCode.CC_BY,
          LicenseCode.CC_BY_SA,
          LicenseCode.CC_BY_ND);
  private static final Set<LicenseCode> ATTRIBUTION_REQUIRED_CODES =
      Set.of(
          LicenseCode.CC_BY,
          LicenseCode.CC_BY_SA,
          LicenseCode.CC_BY_ND,
          LicenseCode.CC_BY_NC,
          LicenseCode.CC_BY_NC_SA,
          LicenseCode.CC_BY_NC_ND);

  public boolean canDisplayAdvertising(LicenseInfo licenseInfo) {
    return hasLicenseCode(licenseInfo, COMMERCIAL_ALLOWED_CODES);
  }

  public boolean canPublishTransformedText(LicenseInfo licenseInfo) {
    return hasLicenseCode(licenseInfo, PUBLISHABLE_TRANSFORMED_TEXT_CODES);
  }

  public boolean requiresAttribution(LicenseInfo licenseInfo) {
    return hasLicenseCode(licenseInfo, ATTRIBUTION_REQUIRED_CODES);
  }

  public boolean requiresShareAlike(LicenseInfo licenseInfo) {
    return hasLicenseCode(licenseInfo, SHARE_ALIKE_REQUIRED_CODES);
  }

  public Set<LicenseCode> getPublishableTransformedTextLicenseCodes() {
    return PUBLISHABLE_TRANSFORMED_TEXT_CODES;
  }

  public Set<LicenseCode> getBriefingSourceLicenseCodes() {
    return BRIEFING_SOURCE_CODES;
  }

  private boolean hasLicenseCode(LicenseInfo licenseInfo, Set<LicenseCode> licenseCodes) {
    return licenseInfo != null && licenseCodes.contains(licenseInfo.getLicenseCode());
  }
}
