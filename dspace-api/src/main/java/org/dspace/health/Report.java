/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import org.apache.commons.cli.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author LINDAT/CLARIN dev team
 */
public class Report {

    private static Logger log = Logger.getLogger(Report.class);
    public static final String EMAIL_PATH = "config/emails/healthcheck";
    // store the individual check reports
    private StringBuilder summary_;

    // ctor
    //
    public Report() {
        summary_ = new StringBuilder();
    }

    // run checks
    //
    public void run(List<Integer> to_perform, ReportInfo ri) {

        int pos = -1;
        for (Entry<String, Check> check_entry : checks().entrySet()) {
            ++pos;
            if ( null != to_perform && !to_perform.contains(pos) ) {
                continue;
            }
            String check_name = check_entry.getKey();
            Check check = check_entry.getValue();

            log.info(String.format("#%d. Processing [%s] at [%s]",
                pos, check_name, new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSS").format(new Date())));

            try {
                // do the stuff
                check.report(ri);
                store(check_name, check.took_, check.report_);

            }catch( Exception e ) {
                store(
                    check_name,
                    -1,
                    "Exception occurred when processing report - " + ExceptionUtils.getStackTrace(e)
                );
            }
        }
    }

    // create check list
    public static LinkedHashMap<String, Check> checks() {
        LinkedHashMap<String, Check> checks = new LinkedHashMap<>();
        String check_names[] = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("healthcheck.checks");
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        for ( String check_name : check_names ) {
            Check check = (Check) pluginService.getNamedPlugin(
                Check.class, check_name);
            if ( null != check ) {
                checks.put(check_name, check);
            }else {
                log.warn( String.format(
                    "Could not find implementation for [%s]", check_name) );
            }
        }
        return checks;
    }

    public String toString() {
        return summary_.toString();
    }

    //
    private void store(String name, long took, String report) {
        name += String.format(" [took: %ds] [# lines: %d]",
                took / 1000,
                new StringTokenizer(report, "\r\n").countTokens()
        );

        String one_summary = String.format(
            "\n#### %s\n%s\n\n###############################\n",
            name,
            report.replaceAll("\\s+$", "")
        );
        summary_.append(one_summary);

        // output it
        System.out.println(one_summary);

    }

    // main
    //

    public static void main(String[] args) {
        log.info("Starting healthcheck report...");

        final String option_help = "h";
        final String option_email = "e";
        final String option_check = "c";
        final String option_last_n = "f";
        final String option_verbose = "v";

        // command line options
        Options options = new Options();
        options.addOption(option_help, "help", false,
            "Show available checks and their index.");
        options.addOption(option_email, "email", true,
            "Send report to this email address.");
        options.addOption(option_check, "check", true,
            "Perform only specific check (use index starting from 0).");
        options.addOption(option_last_n, "for", true,
            "For last N days.");
        options.addOption(option_verbose, "verbose", false,
            "Verbose report.");

        CommandLine cmdline = null;
        try {
            cmdline = new PosixParser().parse(options, args);
        } catch (ParseException e) {
            log.fatal("Invalid command line " + e.toString(), e);
            System.exit(1);
        }

        if ( cmdline.hasOption(option_help) ) {
            String checks_summary = "";
            int pos = 0;
            for (String check_name: checks().keySet()) {
                checks_summary += String.format( "%d. %s\n", pos++, check_name );
            }
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("dspace healthcheck", options);
            System.out.println( "\nAvailable checks:\n" + checks_summary );
            return;
        }

        // what to perform
        List<Integer> to_perform = null;
        if ( null != cmdline.getOptionValues(option_check)) {
            to_perform = new ArrayList<>();
            for (String s : cmdline.getOptionValues('c')) {
                to_perform.add(Integer.valueOf(s));
            }
        }

        try {

            // last n days
            int for_last_n_days = ConfigurationManager.getIntProperty(
                "healthcheck", "last_n_days");
            if ( cmdline.hasOption(option_last_n) ) {
                for_last_n_days = Integer.getInteger(
                    cmdline.getOptionValue(option_last_n));
            }
            ReportInfo ri = new ReportInfo( for_last_n_days );
            if ( cmdline.hasOption(option_verbose) ) {
                ri.verbose( true );
            }

            // run report
            Report r = new Report();
            r.run(to_perform, ri);
            log.info("reports generated...");

            // send/output the report
            if (cmdline.hasOption(option_email)) {
                String to = cmdline.getOptionValue(option_email);
                if ( !to.contains("@") ) {
                    to = ConfigurationManager.getProperty(to);
                }
                try {
                    String dspace_dir = ConfigurationManager.getProperty("dspace.dir");
                    String email_path = dspace_dir.endsWith("/") ? dspace_dir
                            : dspace_dir + "/";
                    email_path += Report.EMAIL_PATH;
                    log.info(String.format(
                        "Looking for email template at [%s]", email_path));
                    Email email = Email.getEmail(email_path);
                    email.addRecipient(to);
                    email.addArgument(r.toString());
                    email.send();
                } catch (Exception e) {
                    log.fatal("Error sending email:", e);
                    System.err.println("Error sending email:\n" + e.getMessage());
                    System.exit(1);
                }
            }

        } catch (Exception e) {
            log.fatal(e);
            e.printStackTrace();
        }
    }

}
