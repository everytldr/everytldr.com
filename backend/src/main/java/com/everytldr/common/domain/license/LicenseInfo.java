package com.everytldr.common.domain.license;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LicenseInfo {
  @Convert(converter = LicenseCodeConverter.class)
  @Column(nullable = false, length = 32)
  private LicenseCode licenseCode;

  @Column(length = 32)
  private String licenseVersion;

  public LicenseInfo(LicenseCode licenseCode, String licenseVersion) {
    this.licenseCode = licenseCode == null ? LicenseCode.UNKNOWN : licenseCode;
    this.licenseVersion = normalizeOptionalText(licenseVersion);
  }

  public static LicenseInfo createUnknown() {
    return new LicenseInfo(LicenseCode.UNKNOWN, null);
  }

  public static LicenseInfo createCcBy(String licenseVersion) {
    return new LicenseInfo(LicenseCode.CC_BY, licenseVersion);
  }

  private static String normalizeOptionalText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }
}
