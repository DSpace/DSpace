package uk.ac.edina.datashare.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Miscellaneous date helper methods.
 */
public class DateUtils
{
    private static final SimpleDateFormat UTC_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat YEAR_MONTH_DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat YEAR_MONTH_FORMAT = new SimpleDateFormat("yyyy-MM");
    private static final SimpleDateFormat YEAR_SMONTH_FORMAT = new SimpleDateFormat("yyyy-M");
    private static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");
    
    /**
     * Get standard simple date string.
     * @param date The date to convert.
     * @return Date string.
     */
    public static String getDateString(Date date)
    {
        return YEAR_MONTH_DAY_FORMAT.format(date);
    }
    
    /**
     * Convert UTC date to string.
     * @param date The date to convert.
     * @return string in the format yyyy-MM-ddTHH:mm:ssZ
     */
    public static String getUTCDateString(Date date)
    {
        return UTC_FORMAT.format(date);
    }
   
    /**
     * Get date for string.
     * @param dateStr Simple date string. options:
     * 1) YYYY
     * 2) YYYY-M
     * 3) YYYY-MM
     * 4) YYYY-MM-DD
     * @return date for string.
     */
    public static Date getDate(String dateStr) throws ParseException
    {
        Date date = null;
        
        switch(dateStr.length())
        {
            case 10:
            {
                date = YEAR_MONTH_DAY_FORMAT.parse(dateStr);
                break;
            }
            case 7:
            {
                date = YEAR_MONTH_FORMAT.parse(dateStr);
                break;
            }
            case 6:
            {
                date = YEAR_SMONTH_FORMAT.parse(dateStr);
                break;
            }
            case 4:
            {
                date = YEAR_FORMAT.parse(dateStr);
                break;
            }
        }
        
        return date;
    }
    
    /**
     * Given a date get the date of the start of the following month.
     * @param date The start date
     * @return The date of the next month
     */
    public static Date getNextMonthStart(Date date)
    {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        
        Calendar endCalendar = new GregorianCalendar(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
        
        
        return endCalendar.getTime();
    }
}
