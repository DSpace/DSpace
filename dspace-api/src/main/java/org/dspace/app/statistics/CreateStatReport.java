/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This class allows the running of the DSpace statistic tools
 *
 * Usage: {@code java CreateStatReport -r <statistic to run>}
 * Available: {@code <stat-initial> <stat-general> <stat-monthly> <stat-report-initial>
 * <stat-report-general> <stat-report-monthly>}
 *
 * @author Chris Yates
 */

public class CreateStatReport {

    /**
     * Current date
     */
    private static LocalDate calendar = null;

    /**
     * Reporting start date
     */
    private static LocalDate reportStartDate = null;

    /**
     * Path of log directory
     */
    private static String outputLogDirectory = null;

    /**
     * Path of reporting directory
     */
    private static String outputReportDirectory = null;

    /**
     * File suffix for log files
     */
    private static final String outputSuffix = ".dat";

    /**
     * User context
     */
    private static Context context;

    /**
     * Default constructor
     */
    private CreateStatReport() { }

    /*
     * Main method to be run from the command line executes individual statistic methods
     *
     * Usage: java CreateStatReport -r <statistic to run>
     */
    public static void main(String[] argv) throws Exception {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();

        // Open the statistics config file
        final String configFile = configurationService.getProperty("dspace.dir")
                + File.separator + "config" + File.separator + "dstat.cfg";
        FileInputStream fis = new java.io.FileInputStream(new File(configFile));
        Properties config = new Properties();
        config.load(fis);
        int startMonth = 1;
        int startYear = 2005;
        try {
            startYear = Integer.parseInt(config.getProperty("start.year", "2005").trim());
        } catch (NumberFormatException nfe) {
            System.err.println("start.year is incorrectly set in dstat.cfg. Must be a number (e.g. 2005).");
            System.exit(0);
        }
        try {
            startMonth = Integer.parseInt(config.getProperty("start.month", "1").trim());
        } catch (NumberFormatException nfe) {
            System.err.println("start.month is incorrectly set in dstat.cfg. Must be a number between 1 and 12.");
            System.exit(0);
        }
        reportStartDate = LocalDate.of(startYear, startMonth, 1);
        calendar = LocalDate.now();

        // create context as super user
        context = new Context();
        context.turnOffAuthorisationSystem();

        //get paths to directories
        outputLogDirectory = configurationService.getProperty("log.report.dir") + File.separator;
        outputReportDirectory = configurationService.getProperty("report.dir") + File.separator;

        //read in command line variable to determine which statistic to run
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("r", "report", true, "report");
        CommandLine line = parser.parse(options, argv);

        String statAction = null;

        if (line.hasOption('r')) {
            statAction = line.getOptionValue('r');
        }

        if (statAction == null) {
            usage();
            System.exit(0);
        }

        //call appropriate statistics method
        if (statAction.equals("stat-monthly")) {
            statMonthly();
        }

        if (statAction.equals("stat-general")) {
            statGeneral();
        }

        if (statAction.equals("stat-initial")) {
            statInitial();
        }

        if (statAction.equals("stat-report-general")) {
            statReportGeneral();
        }

        if (statAction.equals("stat-report-initial")) {
            statReportInitial();
        }

        if (statAction.equals("stat-report-monthly")) {
            statReportMonthly();
        }
    }

    /**
     * This method generates a report from the first of the current month to the end of the current month.
     *
     * @throws Exception if error
     */
    private static void statMonthly() throws Exception {

        //Output Prefix
        String outputPrefix = "dspace-log-monthly-";

        // set up our command line variables
        String myLogDir = null;
        String myFileTemplate = null;
        String myConfigFile = null;
        boolean myLookUp = false;

        LocalDate start = calendar.with(firstDayOfMonth());
        LocalDate end = calendar.with(lastDayOfMonth());

        StringBuilder myOutFile = new StringBuilder(outputLogDirectory);
        myOutFile.append(outputPrefix);
        myOutFile.append(calendar.getYear());
        myOutFile.append("-");
        myOutFile.append(calendar.getMonth());
        myOutFile.append(outputSuffix);

        LogAnalyser
            .processLogs(context, myLogDir, myFileTemplate, myConfigFile, myOutFile.toString(), start, end,
                         myLookUp);
    }

    /**
     * This method generates a full report based on the full log period
     *
     * @throws Exception if error
     */
    private static void statGeneral() throws Exception {

        //Output Prefix
        String outputPrefix = "dspace-log-general-";

        // set up our command line variables
        String myLogDir = null;
        String myFileTemplate = null;
        String myConfigFile = null;
        boolean myLookUp = false;

        StringBuilder myOutFile = new StringBuilder(outputLogDirectory);
        myOutFile.append(outputPrefix);
        myOutFile.append(calendar.getYear());
        myOutFile.append("-");
        myOutFile.append(calendar.getMonth());
        myOutFile.append("-");
        myOutFile.append(calendar.getDayOfMonth());
        myOutFile.append(outputSuffix);

        LogAnalyser
            .processLogs(context, myLogDir, myFileTemplate, myConfigFile, myOutFile.toString(), null, null,
                         myLookUp);
    }

    /**
     * This script starts from the year and month specified below and loops each month until the current month
     * generating a monthly aggregation files for the DStat system.
     *
     * @throws Exception if error
     */
    private static void statInitial() throws Exception {

        //Output Prefix
        String outputPrefix = "dspace-log-monthly-";

        // set up our command line variables
        String myLogDir = null;
        String myFileTemplate = null;
        String myConfigFile = null;
        boolean myLookUp = false;

        LocalDate reportEndDate = calendar.with(lastDayOfMonth());

        LocalDate currentMonth = reportStartDate;
        while (currentMonth.isBefore(reportEndDate)) {

            LocalDate start = currentMonth.with(firstDayOfMonth());
            LocalDate end = currentMonth.with(lastDayOfMonth());

            StringBuilder myOutFile = new StringBuilder(outputLogDirectory);
            myOutFile.append(outputPrefix);
            myOutFile.append(currentMonth.getYear());
            myOutFile.append("-");
            myOutFile.append(currentMonth.getMonth());
            myOutFile.append(outputSuffix);

            LogAnalyser.processLogs(context, myLogDir, myFileTemplate, myConfigFile, myOutFile.toString(), start,
                                    end, myLookUp);

            currentMonth = currentMonth.plus(1, ChronoUnit.MONTHS);
        }
    }

    /**
     * This method generates a full report based on the full log period
     *
     * @throws Exception if error
     */
    private static void statReportGeneral() throws Exception {

        //Prefix
        String inputPrefix = "dspace-log-general-";
        String outputPrefix = "report-general-";

        String myFormat = "html";
        String myMap = null;

        StringBuilder myInput = new StringBuilder(outputLogDirectory);
        myInput.append(inputPrefix);
        myInput.append(calendar.getYear());
        myInput.append("-");
        myInput.append(calendar.getMonth());
        myInput.append("-");
        myInput.append(calendar.getDayOfMonth());
        myInput.append(outputSuffix);

        StringBuilder myOutput = new StringBuilder(outputReportDirectory);
        myOutput.append(outputPrefix);
        myOutput.append(calendar.getYear());
        myOutput.append("-");
        myOutput.append(calendar.getMonth());
        myOutput.append("-");
        myOutput.append(calendar.getDayOfMonth());
        myOutput.append(".");
        myOutput.append(myFormat);

        ReportGenerator.processReport(context, myFormat, myInput.toString(), myOutput.toString(), myMap);
    }

    /**
     * This script starts from the year and month specified below and loops each month until the current month
     * generating monthly reports from the DStat aggregation files
     *
     * @throws Exception if error
     */
    private static void statReportInitial() throws Exception {

        //Prefix
        String inputPrefix = "dspace-log-monthly-";
        String outputPrefix = "report-";

        String myFormat = "html";
        String myMap = null;

        LocalDate reportEndDate = calendar.with(lastDayOfMonth());

        LocalDate currentMonth = reportStartDate;

        while (currentMonth.isBefore(reportEndDate)) {

            StringBuilder myInput = new StringBuilder(outputLogDirectory);
            myInput.append(inputPrefix);
            myInput.append(currentMonth.getYear());
            myInput.append("-");
            myInput.append(currentMonth.getMonth());
            myInput.append(outputSuffix);

            StringBuilder myOutput = new StringBuilder(outputReportDirectory);
            myOutput.append(outputPrefix);
            myOutput.append(currentMonth.getYear());
            myOutput.append("-");
            myOutput.append(currentMonth.getMonth());
            myOutput.append(".");
            myOutput.append(myFormat);

            ReportGenerator.processReport(context, myFormat, myInput.toString(), myOutput.toString(), myMap);

            currentMonth = currentMonth.plus(1, ChronoUnit.MONTHS);
        }
    }

    /**
     * This method generates a report from the aggregation files which have been run for the most recent month
     *
     * @throws Exception if error
     */
    private static void statReportMonthly() throws Exception {
        //Prefix
        String inputPrefix = "dspace-log-monthly-";
        String outputPrefix = "report-";

        String myFormat = "html";
        String myMap = null;

        StringBuilder myInput = new StringBuilder(outputLogDirectory);
        myInput.append(inputPrefix);
        myInput.append(calendar.getYear());
        myInput.append("-");
        myInput.append(calendar.getMonth());
        myInput.append(outputSuffix);

        StringBuilder myOutput = new StringBuilder(outputReportDirectory);
        myOutput.append(outputPrefix);
        myOutput.append(calendar.getYear());
        myOutput.append("-");
        myOutput.append(calendar.getMonth());
        myOutput.append(".");
        myOutput.append(myFormat);

        ReportGenerator.processReport(context, myFormat, myInput.toString(), myOutput.toString(), myMap);
    }

    /*
     * Output the usage information
     */
    private static void usage() throws Exception {

        System.out.println("Usage: java CreateStatReport -r <statistic to run>");
        System.out.println(
            "Available: <stat-initial> <stat-general> <stat-monthly> <stat-report-initial> <stat-report-general> " +
                "<stat-report-monthly>");
    }
}
