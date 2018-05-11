/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Commandline utility to create a file of spider addresses from an Apache
 * log file.
 * 
 * @author Mark Diggory (mdiggory at atmire.com)
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 */
public class ApacheLogRobotsProcessor {


    /**
     * Creates a file containing spiders based on an Apache logfile
     * by analyzing users of the robots.txt file
     *
     * @param args
     * @throws Exception if error
     */

    public static void main(String[] args) throws Exception {
        // create an Options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("l", "logfile", true, "type: Input log file");
        options.addOption("s", "spiderfile", true, "type: Spider IP file");

        CommandLine line = parser.parse(options, args);

        // Log source
        String logFileLoc;
        if (line.hasOption("l"))
        {
            logFileLoc = line.getOptionValue("l");
        }
        else {
            logFileLoc = "-";
        }

        // Spider IP list
        String spiderIpPath;
        if (line.hasOption("s"))
        {
            spiderIpPath = line.getOptionValue("s");
        }
        else {
            spiderIpPath = "-";
        }

        //Get the IPs already added in our file
        Set<String> logSpiders;
        Writer output;

        if ("-".equals(spiderIpPath))
        {
            logSpiders = new HashSet<String>();
            output = new BufferedWriter(new OutputStreamWriter(System.out));
        }
        else
        {
            File spiderIpFile = new File(spiderIpPath);

            if (spiderIpFile.exists())
            {
                logSpiders = SpiderDetector.readPatterns(spiderIpFile);
            }
            else
            {
                logSpiders = new HashSet<String>();
            }
            output = new BufferedWriter(new FileWriter(spiderIpFile));
        }

        //First read in our log file line per line
        BufferedReader in;
        if ("-".equals(logFileLoc))
            in = new BufferedReader(new InputStreamReader(System.in));
        else
            in = new BufferedReader(new FileReader(logFileLoc));

        String logLine;
        while ((logLine = in.readLine()) != null) {
            //Currently only check if robot.txt is present in our line
            if (logLine.contains("robots.txt")) {
                //We got a robots.txt so we got a bot
                String ip = logLine.substring(0, logLine.indexOf('-')).trim();
                //Only add single IP addresses once we got it in it is enough
                logSpiders.add(ip);
            }
        }
        in.close();

        //Last but not least add the IPs to our file
        for (String ip : logSpiders) {
            System.err.println("Adding new ip: " + ip);
            //Write each new IP on a separate line
            output.write(ip + "\n");
        }

        output.flush();
        output.close();
    }
}
