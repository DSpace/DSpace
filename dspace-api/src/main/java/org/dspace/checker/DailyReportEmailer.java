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
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.mail.MessagingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;

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
     *
     * @param attachment
     *            the file containing the report
     * @param numberOfBitstreams
     *            number of bitstreams with potential problems
     * @param toAdr
     *            where to send the email
     * @throws IOException
     *             if IO exception occurs
     * @throws javax.mail.MessagingException
     *             if message cannot be sent.
     */
    public void sendReport(File attachment, int numberOfBitstreams,  String toAdr)
            throws IOException, javax.mail.MessagingException
    {
        InternetAddress to = null;
        try {
            to = new InternetAddress(toAdr);
        } catch (javax.mail.internet.AddressException e) {
            log.error("Could not parse email address " + toAdr);
            return;
        }
        // Get the mail configuration properties
        String server = ConfigurationManager.getProperty("mail.server");

        // Set up properties for mail session
        Properties props = System.getProperties();
        props.put("mail.smtp.host", server);

        // Get session
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage msg = new MimeMessage(session);
        Multipart multipart = new MimeMultipart();

        // create the first part of the email
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart
                .setText("This is the checksum checker report see attachment for details \n"
                        + numberOfBitstreams
                        + " Bitstreams found with POSSIBLE issues");
        multipart.addBodyPart(messageBodyPart);

        // add the file
        messageBodyPart = new MimeBodyPart();

        DataSource source = new FileDataSource(attachment);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName("checksum_checker_report.txt");
        multipart.addBodyPart(messageBodyPart);
        msg.setContent(multipart);
        msg.setFrom(new InternetAddress(ConfigurationManager
                .getProperty("mail.from.address")));
        msg.addRecipient(Message.RecipientType.TO, to);

        msg.setSentDate(new Date());
        msg.setSubject("Checksum checker Report - " + numberOfBitstreams
                + " Bitstreams found with POSSIBLE issues");
        Transport.send(msg);

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
        options.addOption("d", "Deleted", false,
                        "Send E-mail report for all bitstreams set as deleted");
        options.addOption("m", "Missing", false,
                "Send E-mail report for all bitstreams not found in assetstore");
        options.addOption("c", "Changed", false,
                "Send E-mail report for all bitstreams where checksum has been changed");
        options.addOption("a", "All", false, "Send all E-mail reports");
        options.addOption("e", "Eternity", false, "Include all checksum info not just the ones from today");

        options.addOption("u", "Unchecked", false,
                "Send the Unchecked bitstream report");
        options.addOption("n", "Not Processed", false,
                "Send E-mail report for all bitstreams set to longer be processed for today");

        OptionBuilder emailadr = OptionBuilder
                .withArgName("emailadr")
                .hasArgs(1)
                .withDescription(
                        "Send report to given email instead of default mail.admin; if adr is '-' print report to stdout");
        options.addOption(OptionBuilder.create("t"));
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
                    .println("Send Deleted bitstream email report: DailyReportEmailer -d");
            System.out
                    .println("Send Missing bitstreams email report: DailyReportEmailer -m");
            System.out
                    .println("Send Checksum Changed email report: DailyReportEmailer -c");

            System.out
                    .println("Send bitstream not to be processed email report: DailyReportEmailer -n");

            System.out
                    .println("Send Un-checked bitstream report: DailyReportEmailer -u");

            System.out.println("Send All email reports: DailyReportEmailer");
            System.exit(0);
        }



        DailyReportEmailer emailer = new DailyReportEmailer();

        Date yesterday = null, tomorrow = null;

        if (!line.hasOption('e')) {
            // get dates for yesterday and tomorrow
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.add(GregorianCalendar.DAY_OF_YEAR, -1);
            yesterday = calendar.getTime();
            calendar.add(GregorianCalendar.DAY_OF_YEAR, 2);
            tomorrow = calendar.getTime();
        }

        File report = null;
        FileWriter fileWriter = null;
        OutputStreamWriter writer = null;
        try
        {
            String toAdr =  null;
            
            if (line.hasOption("t")) {
                toAdr = line.getOptionValue("t");
            } else {
                toAdr = ConfigurationManager.getProperty("mail.admin");
            }
            log.info(String.format("DailyReportEmailer toAdr = '%s'", toAdr));

            if (toAdr.equals("-")) {// use stdout
                writer = new OutputStreamWriter(System.out);
                toAdr = null;   // indicate that we do not want to send email
            }
            else
            {
                // create a temporary file in the log directory
                String dirLocation = ConfigurationManager.getProperty("log.dir");
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

                fileWriter = new FileWriter(report);
                writer = fileWriter;
            }

            // create a new simple reporter that uses writer for output
            SimpleReporter reporter = new SimpleReporter(new ReportWriter(writer));
            Boolean all = (line.hasOption("a")) || (line.getOptions().length == 0);
            // the number of bitstreams in report
            int numBitstreams = 0;

            if (all || line.hasOption('d'))
            {
                numBitstreams += reporter.deletedBitstreamReport(yesterday, tomorrow);
                writer.write("\n\n");
            }
            if (all || line.hasOption('m'))
            {
                numBitstreams += reporter.bitstreamNotFoundReport(yesterday, tomorrow);
                writer.write("\n\n");
            }
            if (all || line.hasOption('c'))
            {
                numBitstreams += reporter.changedChecksumReport(yesterday, tomorrow);
                writer.write("\n\n");
            }
            if (all || line.hasOption("n"))
            {
                numBitstreams += reporter.notToBeProcessedReport(yesterday, tomorrow);
                writer.write("\n\n");
            }
            if (all || line.hasOption("u"))
            {
                numBitstreams += reporter.uncheckedBitstreamsReport();
                writer.write("\n\n");
            }
            writer.flush();
            writer.close();
            if (toAdr != null)
            {
                emailer.sendReport(report, numBitstreams, toAdr);
            }
            else
            {
                writer.write("Summary:  - " + numBitstreams
                        + " Bitstreams found with POSSIBLE issues");
            }
        }
        catch (MessagingException e)
        {
            log.fatal(e);
        }
        catch (IOException e)
        {
            log.fatal(e);
        }
        finally
        {
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
