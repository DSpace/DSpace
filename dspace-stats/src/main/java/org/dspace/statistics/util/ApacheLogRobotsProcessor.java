/**
 * $Id: $
 * $URL: $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import java.io.*;
import java.util.HashSet;

/**
 * @author Mark Diggory (mdiggory at atmire.com)
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 */
public class ApacheLogRobotsProcessor {


    /**
     * Creates a file containing spiders based on an apache logfile
     * by analyzing users of the robots.txt file
     *
     * @param args
     * @throws Exception
     */

    public static void main(String[] args) throws Exception {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("l", "logfile", true, "type: Input log file");
        options.addOption("s", "spiderfile", true, "type: Spider ip file");

        CommandLine line = parser.parse(options, args);

        String logFileLoc;
        String spiderIpPath;
        if (line.hasOption("l"))
            logFileLoc = line.getOptionValue("l");
        else {
            System.out.println("We need our log file");
            return;
        }
        if (line.hasOption("s"))
            spiderIpPath = line.getOptionValue("s");
        else {
            System.out.println("We need a spider ip output file");
            return;
        }

        File spiderIpFile = new File(spiderIpPath);

        //Get the ip's already added in our file
        HashSet<String> logSpiders = new HashSet<String>();
        if (spiderIpFile.exists())
            logSpiders = SpiderDetector.readIpAddresses(spiderIpFile);


        //First read in our log file line per line
        BufferedReader in = new BufferedReader(new FileReader(logFileLoc));
        String logLine;
        while ((logLine = in.readLine()) != null) {
            //Currently only check if robot.txt is present in our line
            if (logLine.contains("robots.txt")) {
                //We got a robots.txt so we got a bot
                String ip = logLine.substring(0, logLine.indexOf("-")).trim();
                //Only add single ip addresses once we got it in it is enough
                logSpiders.add(ip);
            }
        }
        in.close();

        //Last but not least add the ips to our file
        BufferedWriter output = new BufferedWriter(new FileWriter(spiderIpFile));

        //Second write the new ips
        for (String ip : logSpiders) {
            System.out.println("Adding new ip: " + ip);
            //Write each new ip on a seperate line
            output.write(ip + "\n");
        }

        output.flush();
        output.close();
    }
}
