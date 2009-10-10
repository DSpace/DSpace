/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.util;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import java.io.*;
import java.util.Vector;

/**
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 */
public class SpiderDetector {

    /**
     * Creates a file containing spiders based on an apache logfile
     * by analyzing users of the robots.txt file
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
// create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("l", "logfile", true, "type: Input log file");
        options.addOption("s", "spiderfile", true, "type: Spider ip file");

        CommandLine line = parser.parse(options, args);

        String logFileLoc;
        String spiderIpPath;
        if(line.hasOption("l"))
            logFileLoc = line.getOptionValue("l");
        else{
            System.out.println("We need our log file");
            return;
        }
        if(line.hasOption("s"))
            spiderIpPath = line.getOptionValue("s");
        else{
            System.out.println("We need a spider ip output file");
            return;
        }

        //First read in our log file line per line
        BufferedReader in = new BufferedReader(new FileReader(logFileLoc));
        Vector<String> spiders = new Vector<String>();
        String logLine;
        while ((logLine = in.readLine()) != null){
            //Currently only check if robot.txt is present in our line
            if(logLine.contains("robots.txt")){
                //We got a robots.txt so we got a bot
                String ip = logLine.substring(0, logLine.indexOf("-")).trim();
                //Only add single ip addresses once we got it in it is enough
                if(!spiders.contains(ip))
                    spiders.add(ip);
            }
        }
        in.close();

        //Get the output file
        File spiderIpFile = new File(spiderIpPath);
        //Get the ip's already added in our file
        Vector<String> oldSpiderIds = new Vector<String>();
        if(spiderIpFile.exists())
            oldSpiderIds = readIpAddresses(spiderIpFile);

        Vector<String> newSpiderIds = new Vector<String>();

        //Now run over all these naughty spiders & add em to our overview file
        //PS: only add them if not present
        for (int i = 0; i < spiders.size(); i++) {
            String spiderIp = spiders.elementAt(i);
            if(!oldSpiderIds.contains(spiderIp))
                newSpiderIds.add(spiderIp);
        }

        //Last but not least add the ips to our file
        BufferedWriter output = new BufferedWriter(new FileWriter(spiderIpFile));
        //First write the old ips back so we don't lose any
        for (int i = 0; i < oldSpiderIds.size(); i++) {
            String ip = oldSpiderIds.elementAt(i);
            output.write(ip + "\n");
        }

        //Second write the new ips
        for (int i = 0; i < newSpiderIds.size(); i++) {
            String ip = newSpiderIds.elementAt(i);
            System.out.println("Adding new ip: " + ip);
            //Write each new ip on a seperate line
            output.write(ip + "\n");
        }

        output.flush();
        output.close();
    }


    /**
     * Reads the ip addresses out a file & returns them in a vector
     * @param spiderIpFile the location of our spider file
     * @return a vector full of ip's
     * @throws IOException could not happen since we check the file be4 we use it
     */
    public static Vector<String> readIpAddresses(File spiderIpFile) throws IOException {
        Vector<String> ips = new Vector<String>();
        if(!spiderIpFile.exists())
            return ips;

        //Read our file & get all them ip's
        BufferedReader in = new BufferedReader(new FileReader(spiderIpFile));
        String ip;
        while((ip = in.readLine()) != null){
            ips.add(ip);
        }
        in.close();
        return ips;
    }
}
