package com.freepoint.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Period;

/**
 * ISO-8601 Period 문자열(P1D, P5Y ...) <-> java.time.Period
 */
@Converter
public class PeriodConverter implements AttributeConverter<Period, String> {

    @Override
    public String convertToDatabaseColumn(Period attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public Period convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Period.parse(dbData);
    }
}
