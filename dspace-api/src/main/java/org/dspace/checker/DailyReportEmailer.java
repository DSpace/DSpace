/*
 * Copyright (c) 2004-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.checker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 * <p>
 * The email reporter creates and sends emails to an administrator. This only
 * reports information for todays date. It is expected this will be used just
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
     *            the file conntaing the report
     * @param numberOfBitstreams
     *            the number of bitstreams reported
     * 
     * @throws IOException
     *             if IO exception occurs
     * @throws javax.mail.MessagingException
     *             if message cannot be sent.
     */
    public void sendReport(File attachment, int numberOfBitstreams)
            throws IOException, javax.mail.MessagingException
    {
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
                .setText("This is the checksum checker report see attachement for details \n"
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
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
                ConfigurationManager.getProperty("mail.admin")));

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
     *            <dd>Bitstreams whoses checksums were changed</dd>
     *            <dt>-n</dt>
     *            <dd>Bitstreams whoses checksums were changed</dd>
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
                        "Send E-mail report for all bitstrems where checksum has been changed for today");
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
        SimpleReporter reporter = new SimpleReporterImpl();

        DailyReportEmailer emailer = new DailyReportEmailer();

        // get dates for yesterday and tomorrow
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(GregorianCalendar.DAY_OF_YEAR, -1);

        Date yesterday = calendar.getTime();
        calendar.add(GregorianCalendar.DAY_OF_YEAR, 2);

        Date tomorrow = calendar.getTime();

        File report = null;
        FileWriter writer = null;

        try
        {
            // the number of bitstreams in report
            int numBitstreams = 0;

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

            writer = new FileWriter(report);

            if ((line.hasOption("a")) || (line.getOptions().length == 0))
            {
                writer
                        .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                numBitstreams += reporter.getDeletedBitstreamReport(yesterday,
                        tomorrow, writer);
                writer
                        .write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getChangedChecksumReport(yesterday,
                        tomorrow, writer);
                writer
                        .write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getBitstreamNotFoundReport(yesterday,
                        tomorrow, writer);
                writer
                        .write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getNotToBeProcessedReport(yesterday,
                        tomorrow, writer);
                writer
                        .write("\n--------------------------------- Report Spacer ---------------------------\n\n");
                numBitstreams += reporter.getUncheckedBitstreamsReport(writer);
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
                    numBitstreams += reporter.getDeletedBitstreamReport(
                            yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("m"))
                {
                    writer
                            .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getBitstreamNotFoundReport(
                            yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("c"))
                {
                    writer
                            .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getChangedChecksumReport(
                            yesterday, tomorrow, writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }

                if (line.hasOption("n"))
                {
                    writer
                            .write("\n--------------------------------- Begin Reporting ------------------------\n\n");
                    numBitstreams += reporter.getNotToBeProcessedReport(
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
                            .getUncheckedBitstreamsReport(writer);
                    writer.flush();
                    writer.close();
                    emailer.sendReport(report, numBitstreams);
                }
            }
        }
        catch (Exception e)
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

            if (report != null)
            {
                if (report.exists())
                {
                    report.delete();
                }
            }
        }
    }
}
