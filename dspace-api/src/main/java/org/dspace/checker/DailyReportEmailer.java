/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.checker.factory.CheckerServiceFactory;
import org.dspace.checker.service.SimpleReporterService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;

import javax.mail.MessagingException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.GregorianCalendar;

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
 * 
 * 
 */
public class DailyReportEmailer
{
    /** log4j logger. */
    private static Logger log = Logger.getLogger(DailyReportEmailer.class);

    /**
     * Default constructor.
     */
    public DailyReportEmailer()
    {
    }

    /**
     * Send the report through email.
     * 
     * @param attachment
     *            the file containing the report
     * @param numberOfBitstreams
     *            the number of bitstreams reported
     * 
     * @throws IOException if IO exception occurs
     * @throws MessagingException if message cannot be sent.
     */
    public void sendReport(File attachment, int numberOfBitstreams) 
            throws IOException, javax.mail.MessagingException 
    { 
        if(numberOfBitstreams > 0)
        {
            String hostname = ConfigurationManager.getProperty("dspace.hostname");
            Email email = new Email();
            email.setSubject("Checksum checker Report - " + numberOfBitstreams + " Bitstreams found with POSSIBLE issues on " + hostname);
            email.setContent("report is attached ...");
            email.addAttachment(attachment, "checksum_checker_report.txt");
            email.addRecipient(ConfigurationManager.getProperty("mail.admin"));
            email.send();
        }
    } 

    /**
     * Allows users to have email sent to them. The default is to send all
     * reports in one email
     * 
     * @param args
     *            <dl>
     *            <dt>-h</dt>
     *            <dd>help</dd>
     *            <dt>-d</dt>
     *            <dd>Select deleted bitstreams</dd>
     *            <dt>-m</dt>
     *            <dd>Bitstreams missing from assetstore</dd>
     *            <dt>-c</dt>
     *            <dd>Bitstreams whose checksums were changed</dd>
     *            <dt>-n</dt>
     *            <dd>Bitstreams whose checksums were changed</dd>
     *            <dt>-a</dt>
     *            <dd>Send all reports in one email</dd>
     *            </dl>
     * 
     */
    public static void main(String[] args)
    {
        // set up command line parser
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;

        // create an options object and populate it
        Options options = new Options();

        options.addOption("h", "help", false, "Help");
        options
                .addOption("d", "Deleted", false,
                        "Send E-mail report for all bitstreams set as deleted for today");
        options
                .addOption("m", "Missing", false,
                        "Send E-mail report for all bitstreams not found in assetstore for today");
        options
                .addOption(
                        "c",
                        "Changed",
                        false,
                        "Send E-mail report for all bitstreams where checksum has been changed for today");
        options.addOption("a", "All", false, "Send all E-mail reports");

        options.addOption("u", "Unchecked", false,
                "Send the Unchecked bitstream report");

        options
                .addOption("n", "Not Processed", false,
                        "Send E-mail report for all bitstreams set to longer be processed for today");

        try
        {
            line = parser.parse(options, args);
        }
        catch (ParseException e)
        {
            log.fatal(e);
            System.exit(1);
        }

        // user asks for help
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();

            myhelp.printHelp("Checksum Reporter\n", options);
            System.out
                    .println("\nSend Deleted bitstream email report: DailyReportEmailer -d");
            System.out
                    .println("\nSend Missing bitstreams email report: DailyReportEmailer -m");
            System.out
                    .println("\nSend Checksum Changed email report: DailyReportEmailer -c");

            System.out
                    .println("\nSend bitstream not to be processed email report: DailyReportEmailer -n");

            System.out
                    .println("\nSend Un-checked bitstream report: DailyReportEmailer -u");

            System.out.println("\nSend All email reports: DailyReportEmailer");
            System.exit(0);
        }

        // create a new simple reporter
        SimpleReporterService reporter = CheckerServiceFactory.getInstance().getSimpleReporterService();

        DailyReportEmailer emailer = new DailyReportEmailer();

        // get dates for yesterday and tomorrow
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(GregorianCalendar.DAY_OF_YEAR, -1);

        Date yesterday = calendar.getTime();
        calendar.add(GregorianCalendar.DAY_OF_YEAR, 2);

        Date tomorrow = calendar.getTime();

        File report = null;
        FileWriter writer = null;
        Context context = null;

        try
        {
            context = new Context(Context.Mode.READ_ONLY);

            // the number of bitstreams in report
            int numBitstreams = 0;

            // create a temporary file in the log directory
            String dirLocation = ConfigurationManager.getProperty("log.report.dir");
            File directory = new File(dirLocation);

            if (directory.exists() && directory.isDirectory())
            {
                report = File.createTempFile("checker_report", ".txt",
                        directory);
            }
            else
            {
                throw new IllegalStateException("directory :" + dirLocation
                        + " does not exist");
            }

            writer = new FileWriter(report);

            if ((line.hasOption("a")) || (line.getOptions().length == 0))
            {
                writer
                        .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                numBitstreams += reporter.getDeletedBitstreamReport(context, yesterday,
                        tomorrow, writer);
                writer
                        .write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getChangedChecksumReport(context, yesterday,
                        tomorrow, writer);
                writer
                        .write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getBitstreamNotFoundReport(context, yesterday,
                        tomorrow, writer);
                writer
                        .write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getNotToBeProcessedReport(context, yesterday,
                        tomorrow, writer);
                writer
                        .write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getUncheckedBitstreamsReport(context, writer);
                writer
                        .write("\n--------------------------------- End Report ---------------------------\n\n");
                writer.flush();
                writer.close();
                emailer.sendReport(report, numBitstreams);
            }
            else
            {
                if (line.hasOption("d"))
                {
                    writer
                            .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getDeletedBitstreamReport(context,
                            yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("m"))
                {
                    writer
                            .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getBitstreamNotFoundReport(context,
                            yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("c"))
                {
                    writer
                            .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getChangedChecksumReport(context,
                            yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("n"))
                {
                    writer
                            .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getNotToBeProcessedReport(context,
                            yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("u"))
                {
                    writer
                            .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter
                            .getUncheckedBitstreamsReport(context, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }
            }
        }
        catch (MessagingException | SQLException | IOException e)
        {
            log.fatal(e);
        } finally
        {
            if(context != null && context.isValid())
            {
                context.abort();
            }
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (Exception e)
                {
                    log.fatal("Could not close writer", e);
                }
            }

            if (report != null && report.exists())
            {
                if (!report.delete())
                {
                    log.error("Unable to delete report file");
                }
            }
        }
    }
}
