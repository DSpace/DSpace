/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import org.apache.commons.lang.time.DateUtils;
import org.dspace.core.ConfigurationManager;

import java.text.DateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Helper class for loading the analysis / report files from the reports directory
 */
public class StatisticsLoader
{
    private static Map<String, StatsFile> monthlyAnalysis = null;
    private static Map<String, StatsFile> monthlyReports = null;

    private static StatsFile generalAnalysis = null;
    private static StatsFile generalReport = null;

    private static Date lastLoaded = null;
    private static int fileCount   = 0;

    private static Pattern analysisMonthlyPattern;
    private static Pattern analysisGeneralPattern;
    private static Pattern reportMonthlyPattern;
    private static Pattern reportGeneralPattern;

    private static ThreadLocal<DateFormat>  monthlySDF;
    private static ThreadLocal<DateFormat>  generalSDF;

    // one time initialisation of the regex patterns and formatters we will use
    static
    {
        analysisMonthlyPattern = Pattern.compile("dspace-log-monthly-([0-9][0-9][0-9][0-9]-[0-9]+)\\.dat");
        analysisGeneralPattern = Pattern.compile("dspace-log-general-([0-9]+-[0-9]+-[0-9]+)\\.dat");
        reportMonthlyPattern   = Pattern.compile("report-([0-9][0-9][0-9][0-9]-[0-9]+)\\.html");
        reportGeneralPattern   = Pattern.compile("report-general-([0-9]+-[0-9]+-[0-9]+)\\.html");

        monthlySDF = new ThreadLocal<DateFormat>(){
            @Override
            protected DateFormat initialValue() {
                return new SimpleDateFormat("yyyy'-'M");
            }
          };

        generalSDF = new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                return new SimpleDateFormat("yyyy'-'M'-'dd");
            }
        };
    }

    /**
     * Get an array of the dates of the report files.
     * @return array of dates
     */
    public static Date[] getMonthlyReportDates()
    {
        return sortDatesDescending(getDatesFromMap(monthlyReports));
    }

    /**
     * Get an array of the dates of the analysis files.
     * @return array of dates
     */
    public static Date[] getMonthlyAnalysisDates()
    {
        return sortDatesDescending(getDatesFromMap(monthlyAnalysis));
    }

    /**
     * Convert the formatted dates that are the keys of the map into a date array.
     * @param monthlyMap map 
     * @return array of dates
     */
    protected static Date[] getDatesFromMap(Map<String, StatsFile> monthlyMap)
    {
        Set<String> keys = monthlyMap.keySet();
        Date[] dates = new Date[keys.size()];
        int i = 0;
        for (String date : keys)
        {
            try
            {
                dates[i] = monthlySDF.get().parse(date);
            }
            catch (ParseException pe)
            {
            }

            i++;
        }

        return dates;
    }

    /**
     * Sort the date array in descending (reverse chronological) order.
     * @param dates array of dates
     * @return sorted dates.
     */
    protected static Date[] sortDatesDescending(Date[] dates)
    {
        Arrays.sort(dates, new Comparator<Date>() {
            @Override
            public int compare(Date d1, Date d2)
            {
                if (d1 == null && d2 == null)
                {
                    return 0;
                }
                else if (d1 == null)
                {
                    return -1;
                }
                else if (d2 == null)
                {
                    return 1;
                }
                else if (d1.before(d2))
                {
                    return 1;
                }
                else if (d2.before(d1))
                {
                    return -1;
                }

                return 0;
            }
        });
        return dates;
    }

    /**
     * Get the analysis file for a given date.
     * @param date date
     * @return File
     */
    public static File getAnalysisFor(String date)
    {
        StatisticsLoader.syncFileList();
        StatsFile sf = (monthlyAnalysis == null ? null : monthlyAnalysis.get(date));
        return sf == null ? null : sf.file;
    }

    /**
     * Get the report file for a given date.
     * @param date date
     * @return File
     */
    public static File getReportFor(String date)
    {
        StatisticsLoader.syncFileList();
        StatsFile sf = (monthlyReports == null ? null : monthlyReports.get(date));
        return sf == null ? null : sf.file;
    }

    /**
     * Get the current general analysis file.
     * @return File
     */
    public static File getGeneralAnalysis()
    {
        StatisticsLoader.syncFileList();
        return generalAnalysis == null ? null : generalAnalysis.file;
    }

    /**
     * Get the current general report file.
     * @return File
     */
    public static File getGeneralReport()
    {
        StatisticsLoader.syncFileList();
        return generalReport == null ? null : generalReport.file;
    }

    /**
     * Synchronize the cached list of analysis / report files with the reports directory
     *
     * We synchronize if:
     *
     * 1) The number of files is different (ie. files have been added or removed)
     * 2) We haven't cached anything yet
     * 3) The cache was last generate over an hour ago
     */
    private static void syncFileList()
    {
        // Get an array of all the analysis and report files present
        File[] fileList = StatisticsLoader.getAnalysisAndReportFileList();

        if (fileList != null && fileList.length != fileCount)
        {
            StatisticsLoader.loadFileList(fileList);
        }
        else if (lastLoaded == null)
        {
            StatisticsLoader.loadFileList(fileList);
        }
        else if (DateUtils.addHours(lastLoaded, 1).before(new Date()))
        {
            StatisticsLoader.loadFileList(fileList);
        }
    }

    /**
     * Generate the cached file list from the array of files
     * @param fileList array of files
     */
    private static synchronized void loadFileList(File[] fileList)
    {
        // If we haven't been passed an array of files, get one now
        if (fileList == null || fileList.length == 0)
        {
            fileList = StatisticsLoader.getAnalysisAndReportFileList();
        }

        // Create new maps for the monthly analysis / reports
        Map<String, StatsFile> newMonthlyAnalysis = new HashMap<String, StatsFile>();
        Map<String, StatsFile> newMonthlyReports  = new HashMap<String, StatsFile>();

        StatsFile newGeneralAnalysis = null;
        StatsFile newGeneralReport = null;

        if (fileList != null)
        {
            for (File thisFile : fileList)
            {
                StatsFile statsFile = null;

                // If we haven't identified this file yet
                if (statsFile == null)
                {
                    // See if it is a monthly analysis file
                    statsFile = makeStatsFile(thisFile, analysisMonthlyPattern, monthlySDF.get());
                    if (statsFile != null)
                    {
                        // If it is, add it to the map
                        newMonthlyAnalysis.put(statsFile.dateStr, statsFile);
                    }
                }

                // If we haven't identified this file yet
                if (statsFile == null)
                {
                    // See if it is a monthly report file
                    statsFile = makeStatsFile(thisFile, reportMonthlyPattern, monthlySDF.get());
                    if (statsFile != null)
                    {
                        // If it is, add it to the map
                        newMonthlyReports.put(statsFile.dateStr, statsFile);
                    }
                }

                // If we haven't identified this file yet
                if (statsFile == null)
                {
                    // See if it is a general analysis file
                    statsFile = makeStatsFile(thisFile, analysisGeneralPattern, generalSDF.get());
                    if (statsFile != null)
                    {
                        // If it is, ensure that we are pointing to the most recent file
                        if (newGeneralAnalysis == null || statsFile.date.after(newGeneralAnalysis.date))
                        {
                            newGeneralAnalysis = statsFile;
                        }
                    }
                }

                // If we haven't identified this file yet
                if (statsFile == null)
                {
                    // See if it is a general report file
                    statsFile = makeStatsFile(thisFile, reportGeneralPattern, generalSDF.get());
                    if (statsFile != null)
                    {
                        // If it is, ensure that we are pointing to the most recent file
                        if (newGeneralReport == null || statsFile.date.after(newGeneralReport.date))
                        {
                            newGeneralReport = statsFile;
                        }
                    }
                }
            }
        }

        // Store the newly discovered values in the member cache
        monthlyAnalysis = newMonthlyAnalysis;
        monthlyReports  = newMonthlyReports;
        generalAnalysis = newGeneralAnalysis;
        generalReport   = newGeneralReport;
        lastLoaded = new Date();
    }

    /**
     * Generate a StatsFile entry for this file. The pattern and date
     * formatters are used to identify the file as a particular type,
     * and extract the relevant information.  If the file is not identified
     * by the formatter provided, then we return null.
     * @param thisFile file
     * @param thisPattern patter
     * @param sdf date format
     * @return StatsFile
     */
    private static StatsFile makeStatsFile(File thisFile, Pattern thisPattern, DateFormat sdf)
    {
        Matcher matcher = thisPattern.matcher(thisFile.getName());
        if (matcher.matches())
        {
            StatsFile sf = new StatsFile();
            sf.file = thisFile;
            sf.path = thisFile.getPath();
            sf.dateStr = matcher.group(1).trim();

            try
            {
                sf.date = sdf.parse(sf.dateStr);
            }
            catch (ParseException e)
            {

            }

            return sf;
        }
        
        return null;
    }

    /**
     * Get an array of all the analysis and report files.
     * @return array of files
     */
    private static File[] getAnalysisAndReportFileList()
    {
        File reportDir = new File(ConfigurationManager.getProperty("log.report.dir"));
        if (reportDir != null)
        {
            return reportDir.listFiles(new AnalysisAndReportFilter());
        }

        return null;
    }

    /**
     * Simple class for holding information about an analysis/report file.
     */
    private static class StatsFile
    {
        File file;
        String path;
        Date date;
        String dateStr;
    }

    /**
     * Filter used to restrict files in the reports directory to just
     * analysis or report types.
     */
    private static class AnalysisAndReportFilter implements FilenameFilter
    {
        @Override
        public boolean accept(File dir, String name)
        {
            if (analysisMonthlyPattern.matcher(name).matches())
            {
                return true;
            }

            if (analysisGeneralPattern.matcher(name).matches())
            {
                return true;
            }

            if (reportMonthlyPattern.matcher(name).matches())
            {
                return true;
            }
            
            if (reportGeneralPattern.matcher(name).matches())
            {
                return true;
            }

            return false;
        }
    }
}
