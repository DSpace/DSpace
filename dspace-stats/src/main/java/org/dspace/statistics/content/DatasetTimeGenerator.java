/**
 * $Id: DatasetTimeGenerator.java 4440 2009-10-10 19:03:27Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/content/DatasetTimeGenerator.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;

import java.util.Calendar;
import java.util.Date;

/**
 * Represents a date facet for filtering.
 * 
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 9:44:57
 * 
 */
public class DatasetTimeGenerator extends DatasetGenerator {

    private String type = "time";
    private String dateType;
    private String startDate;
    private String endDate;
    private Date actualStartDate;
    private Date actualEndDate;

    //TODO: process includetotal
    public DatasetTimeGenerator() {
        
    }

    /**
     * Sets the date interval
     * For example if you wish to see the data from today to six months ago give the following params:
     * datatype = "month"
     * start = "-6"
     * end = "+1" the +1 indicates this month also 
     * @param dateType type can be days, months, years
     * @param start the start of the interval
     * @param end the end of the interval
     */
    public void setDateInterval(String dateType, String start, String end){
        this.startDate = start;
        this.endDate = end;
        this.dateType = dateType;

    }

    public void setDateInterval(String dateType, Date start, Date end) throws IllegalArgumentException{
        actualStartDate = (Date) start.clone();
        actualEndDate = (Date) end.clone();
        
        this.dateType = dateType;

        //Check if end comes before start
        Calendar startCal1 = Calendar.getInstance();
        startCal1.setTime(start);

        Calendar endCal1 = Calendar.getInstance();
        endCal1.setTime(end);
        if(endCal1.before(startCal1))
            throw new IllegalArgumentException();

        //TODO: ensure future dates are tested. Although we normally do not have visits from the future
        //Depending on our dateType check if we need to use days/months/years
        int type = -1;
        if("year".equalsIgnoreCase(dateType)){
            type = Calendar.YEAR;
        }else
        if("month".equalsIgnoreCase(dateType)){
            type = Calendar.MONTH;
        }else
        if("day".equalsIgnoreCase(dateType)){
            type = Calendar.DATE;
        }else
        if("hour".equalsIgnoreCase(dateType)){
            type = Calendar.HOUR;
        }

        int difStart = getTimeDifference(start, Calendar.getInstance().getTime(), type);
        int difEnd = getTimeDifference(end, Calendar.getInstance().getTime(), type);
//        System.out.println(difStart + " " + difEnd);

        boolean endPos = false;
        if(difEnd == 0){
            //Includes the current
            difEnd = 1;
            endPos = true;
        }else
        if(0 < difEnd)
            endPos = true;
        else{
            difEnd++;
        }

        startDate = "" + difStart;
        //We need +1 so we can count the current month/year/...
        endDate   = (endPos ? "+" : "") + difEnd;
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

    public Date getActualStartDate() {
        return actualStartDate;
    }

    public void setActualStartDate(Date actualStartDate) {
        this.actualStartDate = actualStartDate;
    }

    public Date getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(Date actualEndDate) {
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

    /**
     *
     * @param date1 the first date
     * @param date2
     * @param type
     */
    private int getTimeDifference(Date date1, Date date2, int type){
        int toAdd;
        int elapsed = 0;
        //We need calendar objects to compare
        Calendar cal1, cal2;
        cal1 = Calendar.getInstance();
        cal2 = Calendar.getInstance();

        cal1.setTime(date1);
        cal2.setTime(date2);

        cal1.clear(Calendar.MILLISECOND);
        cal2.clear(Calendar.MILLISECOND);
        cal1.clear(Calendar.SECOND);
        cal2.clear(Calendar.SECOND);
        cal1.clear(Calendar.MINUTE);
        cal2.clear(Calendar.MINUTE);
        if(type != Calendar.HOUR){
            cal1.clear(Calendar.HOUR);
            cal2.clear(Calendar.HOUR);
            cal1.clear(Calendar.HOUR_OF_DAY);
            cal2.clear(Calendar.HOUR_OF_DAY);
            //yet i know calendar just won't clear his hours
            cal1.set(Calendar.HOUR_OF_DAY, 0);
            cal2.set(Calendar.HOUR_OF_DAY, 0);
        }
        if(type != Calendar.DATE){
            cal1.set(Calendar.DATE, 1);
            cal2.set(Calendar.DATE, 1);
        }
        if(type != Calendar.MONTH){
            cal1.clear(Calendar.MONTH);
            cal2.clear(Calendar.MONTH);
        }

        //Switch em if needed
        if(cal1.after(cal2) || cal1.equals(cal2)){
            Calendar backup = cal1;
            cal1 = cal2;
            cal2 = backup;
            toAdd = 1;
        }else
            toAdd = -1;


        
        /*if(type != Calendar.YEAR){
            cal1.clear(Calendar.YEAR);
            cal2.clear(Calendar.YEAR);
        }
        */
        while(cal1.before(cal2)){
            cal1.add(type, 1);
            elapsed += toAdd;
        }
        return elapsed;
    }
}
