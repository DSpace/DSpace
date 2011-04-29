package org.dspace.statistics.content.filter;

import org.dspace.statistics.SolrLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: kevinvandevelde
 * Date: 13-mrt-2009
 * Time: 13:14:14
 * To change this template use File | Settings | File Templates.
 */
public class StatisticsSolrDateFilter implements StatisticsFilter {
    private Date startDate;
    private Date endDate;
    private String startStr;
    private String endStr;
    private String typeStr;


    public StatisticsSolrDateFilter() {
    }

    public void setStartStr(String startStr) {
        this.startStr = startStr;
    }

    public void setEndStr(String endStr) {
        this.endStr = endStr;
    }

    public void setTypeStr(String typeStr) {
        this.typeStr = typeStr;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String toQuery() {
        if(startDate == null && endDate == null){
            //We have got a strings instead of dates so calculate our dates out of these strings
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

        //Parse them dates
        SimpleDateFormat formatter = new SimpleDateFormat(SolrLogger.DATE_FORMAT_8601);
        String startDateParsed = formatter.format(startDate);
        String endDateParsed = formatter.format(endDate);

        //Create our string
        return "time:[" + startDateParsed + " TO " + endDateParsed + "]";
    }
}
