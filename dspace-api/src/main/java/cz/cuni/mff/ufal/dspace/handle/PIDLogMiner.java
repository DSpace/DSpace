/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.handle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import cz.cuni.mff.ufal.dspace.logging.SimpleLogEntry;
import cz.cuni.mff.ufal.dspace.logging.SimpleLogFile;

public class PIDLogMiner
{
    private static final Logger log = Logger.getLogger(PIDLogMiner.class);

    private static final Pattern REQUEST_PATTERN = Pattern
            .compile("Resolving \\[(.*)\\]");        

    private static final Pattern FAILURE_PATTERN = Pattern
            .compile("Unable to resolve \\[(.*)\\]");

    private static final Pattern SUCCESS_PATTERN = Pattern
            .compile("Handle \\[(.*)\\] resolved to \\[(.*)\\]");
    
    public static final String REQUEST_EVENT = "REQUEST";
    public static final String SUCCESS_EVENT = "SUCCESS";
    public static final String FAILURE_EVENT = "FAILURE";    
    public static final String UNKNOWN_EVENT = "UNKNOWN";

    private String logDir;

    public PIDLogMiner()
    {
        this.logDir = getConfigLogDir();
    }

    public PIDLogMiner(String logDir)
    {
        this.logDir = logDir;
    }

    private List<Date> getDatesInInterval(Date startDate, Date endDate)
    {
        List<Date> dates = new ArrayList<Date>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startDate);

        while (calendar.getTime().before(endDate) || calendar.getTime().equals(endDate))
        {
            dates.add(calendar.getTime());
            calendar.add(Calendar.DATE, 1);
        }
        return dates;
    }
    
    /**
     * Function for computing log statistics
     * 
     * Note: The current log is not processed, only from yesterday backwards.
     *  
     * @param startDate the first day of the statistics  
     * @param endDate the last day of the statistics
     * @return
     */

    public PIDLogStatistics computeStatistics(Date startDate, Date endDate)
    {
        PIDLogStatistics statistics = new PIDLogStatistics();
        List<Date> dates = getDatesInInterval(startDate, endDate);

        for (Date date : dates)
        {
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd")
                    .format(date);
            String logFilename = String.format("handle-plugin.log.%s",
                    formattedDate);
            log.info(String.format("Logfile [%s] processing started",
                    logFilename));

            SimpleLogFile logFile = new SimpleLogFile(logDir, logFilename);

            for (SimpleLogEntry logEntry : logFile)
            {
                updateStatistics(statistics, logEntry);
            }

            log.info(String.format("Logfile [%s] processing finished",
                    logFilename));

        }
        return statistics;

    }

    private void updateStatistics(PIDLogStatistics statistics,
            SimpleLogEntry logEntry)
    {
        String message = logEntry.getMessage();
        Date eventDate = logEntry.getDate();
        Matcher matcher;
        String pid;
        String event;

        matcher = REQUEST_PATTERN.matcher(message);
        if (matcher.find())
        {
            event = REQUEST_EVENT;
            pid = matcher.group(1);
            statistics.updateStatistics(event, pid, eventDate);
            return;
        }

        matcher = SUCCESS_PATTERN.matcher(message);
        if (matcher.find())
        {
            event = SUCCESS_EVENT;
            pid = matcher.group(1);
            statistics.updateStatistics(event, pid, eventDate);
            return;
        }

        matcher = FAILURE_PATTERN.matcher(message);
        if (matcher.find())
        {
            event = FAILURE_EVENT;
            pid = matcher.group(1);
            statistics.updateStatistics(event, pid, eventDate);
            return;
        }

        event = UNKNOWN_EVENT;
        pid = "";
        statistics.updateStatistics(event, pid, eventDate);
        return;

    }

    private String getConfigLogDir()
    {
        return ConfigurationManager.getProperty("log.dir");
    }

}
