/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.apache.commons.codec.binary.StringUtils;

/**
 * This enum holds all the possible frequency types
 * that can be used in "subscription-send" script
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public enum FrequencyType {
    DAY("D"),
    WEEK("W"),
    MONTH("M");

    private String shortName;

    private FrequencyType(String shortName) {
        this.shortName = shortName;
    }

    public static String findLastFrequency(String frequency) {
        String startDate = "";
        String endDate = "";
        switch (frequency) {
            case "D":
                // Frequency is anything updated yesterday.
                // startDate is beginning of day yesterday
                Instant startOfYesterday = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)
                                                        .with(LocalTime.MIN)
                                                        .toInstant();
                startDate = startOfYesterday.toString();
                // endDate is end of day yesterday
                Instant endOfYesterday = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)
                                                      .with(LocalTime.MAX)
                                                      .toInstant();
                endDate = endOfYesterday.toString();
                break;
            case "M":
                // Frequency is anything updated last month.
                // startDate is beginning of last month (first day of month at start of day)
                Instant startOfLastMonth = YearMonth.now(ZoneOffset.UTC)
                                                    .minusMonths(1)
                                                    .atDay(1)
                                                    .atStartOfDay().toInstant(ZoneOffset.UTC);
                startDate = startOfLastMonth.toString();
                // endDate is end of last month (last day of month at end of day)
                Instant endOfLastMonth = YearMonth.now(ZoneOffset.UTC)
                                                  .minusMonths(1)
                                                  .atEndOfMonth()
                                                  .atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

                endDate = endOfLastMonth.toString();
                break;
            case "W":
                // Frequency is anything updated last week
                // startDate is beginning of last week (Sunday, beginning of the day)
                Instant startOfLastWeek = ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1)
                                                       .with(previousOrSame(DayOfWeek.SUNDAY))
                                                       .with(LocalTime.MIN)
                                                       .toInstant();
                startDate = startOfLastWeek.toString();
                // End date is end of last week (Saturday, end of day)
                Instant endOfLastWeek = ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1)
                                                     .with(nextOrSame(DayOfWeek.SATURDAY))
                                                     .with(LocalTime.MAX)
                                                     .toInstant();
                endDate = endOfLastWeek.toString();
                break;
            default:
                return null;
        }
        return "[" + startDate + " TO " + endDate + "]";
    }

    public static boolean isSupportedFrequencyType(String value) {
        for (FrequencyType ft : Arrays.asList(FrequencyType.values())) {
            if (StringUtils.equals(ft.getShortName(), value)) {
                return true;
            }
        }
        return false;
    }

    public String getShortName() {
        return shortName;
    }

}
