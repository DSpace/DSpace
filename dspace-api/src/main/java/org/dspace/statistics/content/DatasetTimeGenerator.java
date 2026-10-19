/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents a date facet for filtering.
 *
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 9:44:57
 */
public class DatasetTimeGenerator extends DatasetGenerator {

    private String type = "time";
    private String dateType;
    private String startDate;
    private String endDate;
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;

    //TODO: process includetotal

    /**
     * Default constructor
     */
    public DatasetTimeGenerator() { }

    /**
     * Sets the date interval.
     * For example if you wish to see the data from today to six months ago give
     * the following parameters:
     * datatype = "month"
     * start = "-6"
     * end = "+1" // the +1 indicates this month also
     *
     * @param dateType type can be days, months, years
     * @param start    the start of the interval
     * @param end      the end of the interval
     */
    public void setDateInterval(String dateType, String start, String end) {
        this.startDate = start;
        this.endDate = end;
        this.dateType = dateType;

    }

    public void setDateInterval(String dateType, LocalDateTime start, LocalDateTime end)
        throws IllegalArgumentException {
        actualStartDate = start;
        actualEndDate = end;

        this.dateType = dateType;

        //Check if end comes before start
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date is before start date");
        }

        // TODO: ensure future dates are tested. Although we normally do not
        // have visits from the future.
        //Depending on our dateType check if we need to use days/months/years.
        ChronoUnit typeChronoUnit = ChronoUnit.DAYS;
        if ("year".equalsIgnoreCase(dateType)) {
            typeChronoUnit = ChronoUnit.YEARS;
        } else if ("month".equalsIgnoreCase(dateType)) {
            typeChronoUnit = ChronoUnit.MONTHS;
        } else if ("day".equalsIgnoreCase(dateType)) {
            typeChronoUnit = ChronoUnit.DAYS;
        } else if ("hour".equalsIgnoreCase(dateType)) {
            typeChronoUnit = ChronoUnit.HOURS;
        }

        long difStart = typeChronoUnit.between(start, LocalDateTime.now());
        long difEnd = typeChronoUnit.between(end, LocalDateTime.now());
//        System.out.println(difStart + " " + difEnd);

        boolean endPos = false;
        if (difEnd == 0) {
            //Includes the current
            difEnd = 1;
            endPos = true;
        } else if (0 < difEnd) {
            endPos = true;
        } else {
            difEnd++;
        }

        startDate = "" + difStart;
        //We need +1 so we can count the current month/year/...
        endDate = (endPos ? "+" : "") + difEnd;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getDateType() {
        return dateType.toUpperCase();
    }

    public LocalDateTime getActualStartDate() {
        return actualStartDate;
    }

    public void setActualStartDate(LocalDateTime actualStartDate) {
        this.actualStartDate = actualStartDate;
    }

    public LocalDateTime getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDateTime actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public void setDateType(String dateType) {
        this.dateType = dateType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
