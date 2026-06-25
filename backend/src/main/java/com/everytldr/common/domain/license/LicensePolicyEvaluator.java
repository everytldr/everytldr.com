package com.everytldr.common.domain.license;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LicensePolicyEvaluator {
  private static final Set<LicenseCode> PUBLISHABLE_TRANSFORMED_TEXT_CODES =
      Set.of(LicenseCode.CC0, LicenseCode.PUBLIC_DOMAIN, LicenseCode.CC_BY, LicenseCode.CC_BY_NC);
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
    return licenseInfo != null && COMMERCIAL_ALLOWED_CODES.contains(licenseInfo.getLicenseCode());
  }

  public boolean canPublishTransformedText(LicenseInfo licenseInfo) {
    return licenseInfo != null
        && PUBLISHABLE_TRANSFORMED_TEXT_CODES.contains(licenseInfo.getLicenseCode());
  }

  public boolean requiresAttribution(LicenseInfo licenseInfo) {
    return licenseInfo != null && ATTRIBUTION_REQUIRED_CODES.contains(licenseInfo.getLicenseCode());
  }

  public Set<LicenseCode> getPublishableTransformedTextLicenseCodes() {
    return PUBLISHABLE_TRANSFORMED_TEXT_CODES;
  }
}
