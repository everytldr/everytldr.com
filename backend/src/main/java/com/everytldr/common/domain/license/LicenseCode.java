package com.everytldr.common.domain.license;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum LicenseCode {
  CC0("CC0"),
  PUBLIC_DOMAIN("PUBLIC_DOMAIN"),
  CC_BY("CC-BY"),
  CC_BY_SA("CC-BY-SA"),
  CC_BY_NC("CC-BY-NC"),
  CC_BY_NC_SA("CC-BY-NC-SA"),
  CC_BY_ND("CC-BY-ND"),
  CC_BY_NC_ND("CC-BY-NC-ND"),
  UNKNOWN("UNKNOWN");

  private static final Map<String, LicenseCode> BY_VALUE =
      Arrays.stream(values())
          .collect(Collectors.toUnmodifiableMap(LicenseCode::value, Function.identity()));

  private final String value;

  LicenseCode(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static LicenseCode fromValue(String value) {
    if (value == null || value.isBlank()) {
      return UNKNOWN;
    }

    LicenseCode licenseCode = BY_VALUE.get(value.trim().toUpperCase(Locale.ROOT));
    if (licenseCode != null) {
      return licenseCode;
    }

    try {
      return LicenseCode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return UNKNOWN;
    }
  }
}
