package com.everytldr.common.domain.license;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LicenseCodeConverter implements AttributeConverter<LicenseCode, String> {
  @Override
  public String convertToDatabaseColumn(LicenseCode attribute) {
    return attribute == null ? LicenseCode.UNKNOWN.value() : attribute.value();
  }

  @Override
  public LicenseCode convertToEntityAttribute(String dbData) {
    return LicenseCode.fromValue(dbData);
  }
}
