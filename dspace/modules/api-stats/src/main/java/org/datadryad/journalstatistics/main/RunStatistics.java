/*
 */
package org.datadryad.journalstatistics.main;

import java.util.Date;
import java.util.IllegalFormatException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.datadryad.journalstatistics.statistics.DefaultStatisticsPackage;
import org.dspace.content.DCDate;
import org.dspace.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class RunStatistics {

    public static void main(String args[]) throws Exception {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("j", true, "Journal Name (required)");
        options.addOption("f", "from", true, "Begin Date");
        options.addOption("t", "to", true, "End Date");
        options.addOption("h", "help", false, "help");
        CommandLine line = parser.parse(options, args);

        Date beginDate = null;
        Date endDate = null;
        if(line.hasOption("f")) {
            try {
                beginDate = parseDate(line.getOptionValue("f"));
            } catch (IllegalArgumentException ex) {
                System.err.println("Error parsing date: " + ex.getMessage());
                printHelp(options, 1);
            }
        }
        if(line.hasOption("t")) {
            try {
                endDate = parseDate(line.getOptionValue("t"));
            } catch (IllegalArgumentException ex) {
                System.err.println("Error parsing date: " + ex.getMessage());
                printHelp(options, 1);
            }
        }

        if(line.hasOption("j")) {
            String journalName = line.getOptionValue("j");
            Context c = new Context();
            c.turnOffAuthorisationSystem();
            DefaultStatisticsPackage statisticsPackage = new DefaultStatisticsPackage(c);
            if(beginDate != null) {
                statisticsPackage.setBeginDate(beginDate);
            }
            if(endDate != null) {
                statisticsPackage.setEndDate(endDate);
            }
            statisticsPackage.run(journalName);
        } else {
            printHelp(options, 0);
        }
    }

    private static Date parseDate(String dateString) throws IllegalArgumentException {
        final Date returnDate = new DCDate(dateString).toDate();
        if(returnDate == null) {
            throw new IllegalArgumentException("Unparseable date: " + dateString);
        }
        return new DCDate(dateString).toDate();
    }

    private static void printHelp(Options options, int exitCode) {
        HelpFormatter myhelp = new HelpFormatter();
        String footer = "\nDCDate is used for date parsing, accepts '2014', '2014-01-01', or full ISO8601 formatting";
        myhelp.printHelp("dspace journal-stats\n", null, options, footer);
        System.exit(exitCode);
    }

}
