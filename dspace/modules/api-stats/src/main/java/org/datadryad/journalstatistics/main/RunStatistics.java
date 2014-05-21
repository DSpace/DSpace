/*
 */
package org.datadryad.journalstatistics.main;

import java.sql.SQLException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.datadryad.journalstatistics.statistics.DefaultStatisticsPackage;
import org.dspace.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class RunStatistics {

    public static void main(String args[]) throws Exception {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("j", true, "Journal Name");
        options.addOption("f", "from", true, "Start Date");
        options.addOption("t", "to", true, "End Date");
        options.addOption("h", "help", false, "help");
        CommandLine line = parser.parse(options, args);

        // Did the user ask to see the help?
        if(line.hasOption("j")) {
            String journalName = line.getOptionValue("j");
            Context c = new Context();
            c.turnOffAuthorisationSystem();
            DefaultStatisticsPackage statisticsPackage = new DefaultStatisticsPackage(c);
            statisticsPackage.run(journalName);
        } else {
            printHelp(options, 0);
        }
    }

    private static void printHelp(Options options, int exitCode) {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("RunStatistics\n", options);
        System.exit(exitCode);
    }

}
