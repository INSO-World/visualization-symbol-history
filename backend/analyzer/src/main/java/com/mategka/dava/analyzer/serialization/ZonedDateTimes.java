package com.mategka.dava.analyzer.serialization;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class ZonedDateTimes {

  public static final ZonedDateTime EPOCH = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);

  public ZonedDateTime nowWithSecondPrecision() {
    return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
  }

}
