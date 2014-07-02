/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
     * Sets the date interval.
     * For example if you wish to see the data from today to six months ago give
     * the following parameters:
     * datatype = "month"
     * start = "-6"
     * end = "+1" // the +1 indicates this month also
     * 
     * @param dateType type can be days, months, years
     * @param start the start of the interval
     * @param end the end of the interval
     */
    public void setDateInterval(String dateType, String start, String end){
        this.startDate = start;
        this.endDate = end;
        this.dateType = dateType;

    }

    public void setDateInterval(String dateType, Date start, Date end)
            throws IllegalArgumentException
    {
        actualStartDate = (start == null ? null : new Date(start.getTime()));
        actualEndDate = (end == null ? null : new Date(end.getTime()));
        
        this.dateType = dateType;

        //Check if end comes before start
        Calendar startCal1 = Calendar.getInstance();
        Calendar endCal1 = Calendar.getInstance();

        if (startCal1 == null || endCal1 == null)
        {
            throw new IllegalStateException("Unable to create calendar instances");    
        }

        startCal1.setTime(start);
        endCal1.setTime(end);
        if(endCal1.before(startCal1))
        {
            throw new IllegalArgumentException();
        }

        // TODO: ensure future dates are tested. Although we normally do not
        // have visits from the future.
        //Depending on our dateType check if we need to use days/months/years.
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
        {
            endPos = true;
        }
        else{
            difEnd++;
        }

        startDate = "" + difStart;
        //We need +1 so we can count the current month/year/...
        endDate = (difEnd<0 ? "" : "+") + difEnd;
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
        return actualStartDate == null ? null : new Date(actualStartDate.getTime());
    }

    public void setActualStartDate(Date actualStartDate) {
        this.actualStartDate = (actualStartDate == null ? null : new Date(actualStartDate.getTime()));
    }

    public Date getActualEndDate() {
        return actualEndDate == null ? null : new Date(actualEndDate.getTime());
    }

    public void setActualEndDate(Date actualEndDate) {
        this.actualEndDate = (actualEndDate == null ? null : new Date(actualEndDate.getTime()));
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

    /** Get the difference between two Dates in terms of a given interval.
     * That is:  if you specify the difference in months, you get back the
     * number of months between the dates.
     *
     * @param date1 the first date
     * @param date2 the other date
     * @param type Calendar.HOUR or .DATE or .MONTH
     * @return number of {@code type} intervals between {@code date1} and
     *  {@code date2}
     */
    private int getTimeDifference(Date date1, Date date2, int type){
        //We need calendar objects to compare
        Calendar cal1, cal2;
        cal1 = Calendar.getInstance();
        cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        switch (type){
        case Calendar.HOUR:
      	  return (int) ((date2.getTime()-date1.getTime())/(1000*60*60)*-1);
        case Calendar.DATE:
      	  return (int) ((date2.getTime()-date1.getTime())/(1000*60*60*24)*-1);
      	  
        case Calendar.MONTH:
      	  int diffYear = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);
      	  int diffMonth = diffYear * 12 + cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH);
      	  return diffMonth*-1;
        case Calendar.YEAR:
      	  diffYear = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);
      	  return diffYear*-1;
        
        }
        return 0;
      }
    }
