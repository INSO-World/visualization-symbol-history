package com.mategka.dava.analyzer.extension.time;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

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

  public ZonedDateTime min(@NotNull ZonedDateTime zdt1, @NotNull ZonedDateTime zdt2) {
    return zdt1.isBefore(zdt2) ? zdt1 : zdt2;
  }

  public ZonedDateTime max(@NotNull ZonedDateTime zdt1, @NotNull ZonedDateTime zdt2) {
    return zdt1.isAfter(zdt2) ? zdt1 : zdt2;
  }

}
