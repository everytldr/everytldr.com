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
  void canPublishTransformedTextOnlyForCurrentlySupportedLicenses() {
    assertThat(evaluator.canPublishTransformedText(new LicenseInfo(LicenseCode.CC0, "1.0")))
        .isTrue();
    assertThat(
            evaluator.canPublishTransformedText(new LicenseInfo(LicenseCode.PUBLIC_DOMAIN, null)))
        .isTrue();
    assertThat(evaluator.canPublishTransformedText(licenseInfo("CC-BY"))).isTrue();
    assertThat(evaluator.canPublishTransformedText(licenseInfo("CC-BY-NC"))).isTrue();
    assertThat(evaluator.canPublishTransformedText(licenseInfo("CC-BY-SA"))).isTrue();
    assertThat(evaluator.canPublishTransformedText(licenseInfo("CC-BY-NC-SA"))).isTrue();

    assertThat(evaluator.canPublishTransformedText(LicenseInfo.createUnknown())).isFalse();
    assertThat(evaluator.canPublishTransformedText(licenseInfo("CC-BY-ND"))).isFalse();
    assertThat(evaluator.canPublishTransformedText(licenseInfo("CC-BY-NC-ND"))).isFalse();
  }

  @Test
  void requiresShareAlikeOnlyForShareAlikeCreativeCommonsLicenses() {
    assertThat(evaluator.requiresShareAlike(licenseInfo("CC-BY-SA"))).isTrue();
    assertThat(evaluator.requiresShareAlike(licenseInfo("CC-BY-NC-SA"))).isTrue();

    assertThat(evaluator.requiresShareAlike(licenseInfo("CC-BY"))).isFalse();
    assertThat(evaluator.requiresShareAlike(licenseInfo("CC-BY-NC"))).isFalse();
    assertThat(evaluator.requiresShareAlike(LicenseInfo.createUnknown())).isFalse();
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
