package com.everytldr.common.domain.license;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LicensePolicyEvaluatorTest {
  private final LicensePolicyEvaluator evaluator = new LicensePolicyEvaluator();

  @Test
  void canDisplayAdvertisingForCommercialCreativeCommonsLicenses() {
    assertThat(evaluator.canDisplayAdvertising(licenseInfo("CC-BY"))).isTrue();
    assertThat(evaluator.canDisplayAdvertising(licenseInfo("CC-BY-NC"))).isFalse();
    assertThat(evaluator.canDisplayAdvertising(LicenseInfo.createUnknown())).isFalse();
  }

  @Test
  void canTransformTextForLicensesWithoutNoDerivativesRestriction() {
    assertThat(evaluator.canTransformText(licenseInfo("CC-BY-SA"))).isTrue();
    assertThat(evaluator.canTransformText(licenseInfo("CC-BY-ND"))).isFalse();
  }

  @Test
  void requiresAttributionForAttributionCreativeCommonsLicenses() {
    assertThat(evaluator.requiresAttribution(licenseInfo("CC-BY"))).isTrue();
    assertThat(evaluator.requiresAttribution(new LicenseInfo(LicenseCode.CC0, "1.0"))).isFalse();
  }

  private LicenseInfo licenseInfo(String licenseCode) {
    return new LicenseInfo(LicenseCode.fromValue(licenseCode), "4.0");
  }
}
