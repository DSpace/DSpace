/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.mail.MessagingException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.checker.factory.CheckerServiceFactory;
import org.dspace.checker.service.SimpleReporterService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * <p>
 * The email reporter creates and sends emails to an administrator. This only
 * reports information for today's date. It is expected this will be used just
 * after the checksum checker has been run.
 * </p>
 *
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 */
public class DailyReportEmailer {
    /**
     * log4j logger.
     */
    private static final Logger log = LogManager.getLogger(DailyReportEmailer.class);

    /**
     * Default constructor.
     */
    public DailyReportEmailer() {
    }

    /**
     * Send the report through email.
     *
     * @param attachment         the file containing the report
     * @param numberOfBitstreams the number of bitstreams reported
     * @throws IOException        if IO exception occurs
     * @throws MessagingException if message cannot be sent.
     */
    public void sendReport(File attachment, int numberOfBitstreams)
        throws IOException, jakarta.mail.MessagingException {
        if (numberOfBitstreams > 0) {
            ConfigurationService configurationService
                    = DSpaceServicesFactory.getInstance().getConfigurationService();
            String hostname = Utils.getHostName(configurationService.getProperty("dspace.ui.url"));
            Email email = new Email();
            email.setSubject(String.format(
                "Checksum checker Report - %d Bitstreams found with POSSIBLE issues on %s",
                    numberOfBitstreams, hostname));
            email.setContent("Checker Report", "report is attached ...");
            email.addAttachment(attachment, "checksum_checker_report.txt");
            email.addRecipient(configurationService.getProperty("mail.admin"));
            log.info("Sending checker report email to " + configurationService.getProperty("mail.admin"));
            email.send();
        }
    }

    /**
     * Allows users to have email sent to them. The default is to send all
     * reports in one email
     *
     * <dl>
     * <dt>-h</dt>
     * <dd>help</dd>
     * <dt>-d</dt>
     * <dd>Select deleted bitstreams</dd>
     * <dt>-m</dt>
     * <dd>Bitstreams missing from assetstore</dd>
     * <dt>-c</dt>
     * <dd>Bitstreams whose checksums were changed</dd>
     * <dt>-n</dt>
     * <dd>Bitstreams whose checksums were changed</dd>
     * <dt>-a</dt>
     * <dd>Send all reports in one email</dd>
     * </dl>
     *
     * @param args the command line arguments given
     */
    public static void main(String[] args) {
        // set up command line parser
        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;

        // create an options object and populate it
        Options options = new Options();

        options.addOption("h", "help", false, "Help");
        options.addOption("d", "deleted", false,
                          "Send email report for all bitstreams set as deleted for today");
        options.addOption("m", "missing", false,
                          "Send email report for all bitstreams not found in assetstore for today");
        options.addOption("c", "changed", false,
                          "Send email report for all bitstreams where checksum has been changed for today");
        options.addOption("a", "all", false,
                          "Send all email reports (used by default)");
        options.addOption("u", "unchecked", false,
                          "Send the unchecked (i.e. recently added) bitstream email report");
        options.addOption("n", "not-processed", false,
                          "Send email report for all bitstreams set to no longer be processed for today (includes"
                            + " bitstreams marked as deleted or not found)");

        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            log.fatal(e);
            System.exit(1);
        }

        // user asks for help
        if (line.hasOption('h')) {
            HelpFormatter myhelp = new HelpFormatter();

            myhelp.printHelp("checker-emailer\n", options);
            System.out.println("\nChecksum Checker Reporter usage examples:\n");
            System.out.println(" - Send all email reports: checker-emailer -a");
            System.out.println(" - Send deleted bitstream email report: checker-emailer -d");
            System.out.println(" - Send missing bitstreams email report: checker-emailer -m");
            System.out.println(" - Send checksum changed email report: checker-emailer -c");
            System.out.println(" - Send bitstream not to be processed email report: checker-emailer -n");
            System.out.println(" - Send unchecked bitstream email report: checker-emailer -u");
            System.out.println("\nDefault (no arguments) is equivalent to 'checker-emailer -a'\n");
            System.exit(0);
        }

        // create a new simple reporter
        SimpleReporterService reporter = CheckerServiceFactory.getInstance().getSimpleReporterService();

        DailyReportEmailer emailer = new DailyReportEmailer();

        // get dates for yesterday and tomorrow (start of day for both)
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

        File report = null;
        FileWriter writer = null;
        Context context = null;

        try {
            context = new Context(Context.Mode.READ_ONLY);

            // the number of bitstreams in report
            int numBitstreams = 0;

            // create a temporary file in the log directory
            ConfigurationService configurationService
                    = DSpaceServicesFactory.getInstance().getConfigurationService();
            String dirLocation = configurationService.getProperty("log.report.dir");
            File directory = new File(dirLocation);

            if (directory.exists() && directory.isDirectory()) {
                report = File.createTempFile("checker_report", ".txt",
                                             directory);
            } else {
                throw new IllegalStateException("directory :" + dirLocation
                                                    + " does not exist");
            }

            writer = new FileWriter(report);

            if ((line.hasOption("a")) || (line.getOptions().length == 0)) {
                writer.write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                numBitstreams += reporter.getDeletedBitstreamReport(context, yesterday, tomorrow, writer);
                writer.write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getChangedChecksumReport(context, yesterday, tomorrow, writer);
                writer.write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getBitstreamNotFoundReport(context, yesterday, tomorrow, writer);
                writer.write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                // not to be processed report includes deleted and not found bitstreams so it is not necessary to
                // include the sum in the counter
                reporter.getNotToBeProcessedReport(context, yesterday, tomorrow, writer);
                writer.write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getUncheckedBitstreamsReport(context, writer);
                writer.write("\n--------------------------------- End Report ---------------------------\n\n");
                writer.flush();
                writer.close();
                emailer.sendReport(report, numBitstreams);
            } else {
                if (line.hasOption("d")) {
                    writer.write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getDeletedBitstreamReport(context,
                                                                        yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("m")) {
                    writer.write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getBitstreamNotFoundReport(context,
                                                                         yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("c")) {
                    writer.write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getChangedChecksumReport(context,
                                                                       yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("n")) {
                    writer.write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getNotToBeProcessedReport(context,
                                                                        yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("u")) {
                    writer.write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter
                        .getUncheckedBitstreamsReport(context, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }
            }
        } catch (MessagingException | SQLException | IOException e) {
            log.fatal(e);
        } finally {
            if (context != null && context.isValid()) {
                context.abort();
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.fatal("Could not close writer", e);
                }
            }

            if (report != null && report.exists()) {
                if (!report.delete()) {
                    log.error("Unable to delete report file");
                }
            }
        }
    }
}
