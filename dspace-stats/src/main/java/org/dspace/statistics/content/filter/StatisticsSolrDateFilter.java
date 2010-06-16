package org.dspace.statistics.content.filter;

import org.dspace.statistics.SolrLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

/**
 * Encapsulate a range of dates for Solr query filtering.
 * Created by IntelliJ IDEA.
 * User: kevinvandevelde
 * Date: 13-mrt-2009
 * Time: 13:14:14
 */
public class StatisticsSolrDateFilter implements StatisticsFilter {
    private Date startDate;
    private Date endDate;
    private String startStr;
    private String endStr;
    private String typeStr;


    public StatisticsSolrDateFilter() {
    }

    /** Set the start date as a string expression.
     * Must be paired with {@link #setEndStr(String)}.
     */
    public void setStartStr(String startStr) {
        this.startStr = startStr;
    }

    /** Set the end date as a string expression.
     * Must be paired with {@link #setStartStr(String)}.
     */
    public void setEndStr(String endStr) {
        this.endStr = endStr;
    }

    /** Set the range granularity:  DAY, MONTH, or YEAR. */
    public void setTypeStr(String typeStr) {
        this.typeStr = typeStr;
    }

    /** Set the start date as a Date object.
     * Must be paired with {@link #setEndDate(Date)}.
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /** Set the end date as a Date object.
     * Must be paired with {@link #setStartDate(Date)}.
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /** Convert the date range to a filter expression.
     * @return Solr date filter expression
     */
    public String toQuery() {
        if(startDate == null && endDate == null){
            // We have got strings instead of dates so calculate our dates out
            // of these strings
            Calendar startCal = Calendar.getInstance();

            startCal.clear(Calendar.MILLISECOND);
            startCal.clear(Calendar.SECOND);
            startCal.clear(Calendar.MINUTE);
            startCal.set(Calendar.HOUR_OF_DAY, 0);

            int dateType = -1;
            if(typeStr.equalsIgnoreCase("day")) {
                dateType = Calendar.DATE;
            } else if(typeStr.equalsIgnoreCase("month")) {
                dateType = Calendar.MONTH;
                startCal.set(Calendar.DATE, 1);
            } else if(typeStr.equalsIgnoreCase("year")) {
                startCal.clear(Calendar.MONTH);
                startCal.set(Calendar.DATE, 1);
                dateType = Calendar.YEAR;
            } else
                return "";

            Calendar endCal = (Calendar) startCal.clone();

            if(startStr.startsWith("+"))
                startStr = startStr.substring(startStr.indexOf("+") + 1);

            if(endStr.startsWith("+"))
                endStr = endStr.substring(endStr.indexOf("+") + 1);

            startCal.add(dateType, Integer.parseInt(startStr));
            endCal.add(dateType, Integer.parseInt(endStr));

            startDate = startCal.getTime();
            endDate = endCal.getTime();
        }

        //Parse the dates
        SimpleDateFormat formatter = new SimpleDateFormat(SolrLogger.DATE_FORMAT_8601);
        String startDateParsed = formatter.format(startDate);
        String endDateParsed = formatter.format(endDate);

        //Create our string
        return "time:[" + startDateParsed + " TO " + endDateParsed + "]";
    }
}
