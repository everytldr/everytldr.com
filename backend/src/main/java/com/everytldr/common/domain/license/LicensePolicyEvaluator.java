package com.everytldr.common.domain.license;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LicensePolicyEvaluator {
  private static final Set<LicenseCode> COMMERCIAL_ALLOWED_CODES =
      Set.of(
          LicenseCode.CC0,
          LicenseCode.PUBLIC_DOMAIN,
          LicenseCode.CC_BY,
          LicenseCode.CC_BY_SA,
          LicenseCode.CC_BY_ND);
  private static final Set<LicenseCode> TRANSFORMATION_ALLOWED_CODES =
      Set.of(
          LicenseCode.CC0,
          LicenseCode.PUBLIC_DOMAIN,
          LicenseCode.CC_BY,
          LicenseCode.CC_BY_SA,
          LicenseCode.CC_BY_NC,
          LicenseCode.CC_BY_NC_SA);
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

  public boolean canTransformText(LicenseInfo licenseInfo) {
    return licenseInfo != null
        && TRANSFORMATION_ALLOWED_CODES.contains(licenseInfo.getLicenseCode());
  }

  public boolean requiresAttribution(LicenseInfo licenseInfo) {
    return licenseInfo != null && ATTRIBUTION_REQUIRED_CODES.contains(licenseInfo.getLicenseCode());
  }
}
