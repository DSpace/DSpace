package com.atmire.statistics.export;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * Date: 25/04/12
 * Time: 09:32
 *
 * @author: Kevin Van Ransbeeck (kevin van ransbeeck @ atmire dot com)
 */
public class RetryOpenUrlTracker {
    private static final Logger log = Logger.getLogger(RetryOpenUrlTracker.class);

    /* Command Line execution */
    public static void main(String[] args) throws SQLException {
        Context context = new Context();
        context.setIgnoreAuthorization(true);

        String usage = "com.atmire.statistics.export.RetryOpenUrlTracker [-a <URL>]] or nothing to retry all failed attempts.";
        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        OptionBuilder.withArgName("Open URL Tracker");
        OptionBuilder.hasArg(true);
        OptionBuilder.withDescription("Add a new row to the table (test purposes only)");
        options.addOption(OptionBuilder.create("a"));
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("print this help message");
        options.addOption(OptionBuilder.create("h"));

        try {
            line = new PosixParser().parse(options, args);
        } catch (Exception e) {
            // automatically generate the help statement
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }
        if (line.hasOption("h")) {
            // automatically generate the help statement
            formatter.printHelp(usage, options);
            System.exit(1);
        }

        if (line.hasOption("a")) {
            ExportUsageEventListener.logfailed(context, line.getOptionValue("a"));
            log.info("Created dummy entry in OpenUrlTracker with URL: " + line.getOptionValue("a"));
        } else {
            ExportUsageEventListener.reprocessFailedQueue();
        }

        try {
            context.complete();
        } catch (Exception ignored) {
        }
    }
}
