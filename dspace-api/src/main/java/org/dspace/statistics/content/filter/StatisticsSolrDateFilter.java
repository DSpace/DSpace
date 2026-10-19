/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content.filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
/**
 * Encapsulate a range of dates for Solr query filtering.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class StatisticsSolrDateFilter implements StatisticsFilter {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String startStr;
    private String endStr;
    private String typeStr;


    public StatisticsSolrDateFilter() {
    }

    /**
     * Set the start date as a string expression.
     *
     * @param startStr statistics start date as a string
     *
     *                 Must be paired with {@link #setEndStr(String)}.
     */
    public void setStartStr(String startStr) {
        this.startStr = startStr;
    }

    /**
     * Set the end date as a string expression.
     *
     * @param endStr statistics end date as a string
     *
     *               Must be paired with {@link #setStartStr(String)}.
     */
    public void setEndStr(String endStr) {
        this.endStr = endStr;
    }

    /**
     * Set the range granularity:  DAY, MONTH, or YEAR.
     *
     * @param typeStr which granularity (case insensitive string: "day" / "month" / "year")
     */
    public void setTypeStr(String typeStr) {
        this.typeStr = typeStr;
    }

    /**
     * Set the start date as a Date object.
     *
     * @param startDate statistics start date object
     *
     *                  Must be paired with {@link #setEndDate(LocalDateTime)}.
     */
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    /**
     * Set the end date as a Date object.
     *
     * @param endDate statistics end date object
     *
     *                Must be paired with {@link #setStartDate(LocalDateTime)}.
     */
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    /**
     * Convert the date range to a filter expression.
     *
     * @return Solr date filter expression
     */
    @Override
    public String toQuery() {
        if (startDate == null || endDate == null) {
            // We have got strings instead of dates so calculate our dates out
            // of these strings
            LocalDate startCal = LocalDate.now();

            ChronoUnit dateType;
            if (typeStr.equalsIgnoreCase("day")) {
                dateType = ChronoUnit.DAYS;
            } else if (typeStr.equalsIgnoreCase("month")) {
                dateType = ChronoUnit.MONTHS;
                startCal = startCal.withDayOfMonth(1);
            } else if (typeStr.equalsIgnoreCase("year")) {
                startCal = startCal.withDayOfYear(1);
                dateType = ChronoUnit.YEARS;
            } else {
                return "";
            }

            LocalDate endCal = startCal;

            if (startDate == null) {
                if (startStr.startsWith("+")) {
                    startStr = startStr.substring(startStr.indexOf('+') + 1);
                }
                startDate = startCal.plus(Integer.parseInt(startStr), dateType).atStartOfDay();
            }

            if (endDate == null) {
                if (endStr.startsWith("+")) {
                    endStr = endStr.substring(endStr.indexOf('+') + 1);
                }
                endDate = endCal.plus(Integer.parseInt(endStr), dateType).atStartOfDay();
            }
        }

        //Parse the dates
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String startDateParsed = formatter.format(startDate.toInstant(ZoneOffset.UTC));
        String endDateParsed = formatter.format(endDate.toInstant(ZoneOffset.UTC));

        //Create our string
        return "time:[" + startDateParsed + " TO " + endDateParsed + "]";
    }
}
