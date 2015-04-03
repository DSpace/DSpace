/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.util.IPTable;

/* Created for LINDAT/CLARIN */
/**
 * SpiderDetector is used to find IP's that are spiders...
 * 
 * taken from dspace-stats SpiderDetector
 */
public class SimpleSpiderDetector {

    private static Logger log = Logger.getLogger(SimpleSpiderDetector.class);

    /**
     * Sparse HAshTable structure to hold IP Address Ranges.
     */
    private static IPTable table = null;

    /**
     * Utility method which Reads the ip addresses out a file & returns them in a Set
     *
     * @param spiderIpFile the location of our spider file
     * @return a vector full of ip's
     * @throws IOException could not happen since we check the file be4 we use it
     */
    public static Set<String> readIpAddresses(File spiderIpFile) throws IOException {
        Set<String> ips = new HashSet<String>();

        if (!spiderIpFile.exists() || !spiderIpFile.isFile())
        {
            return ips;
        }

        //Read our file & get all them ip's
        BufferedReader in = new BufferedReader(new FileReader(spiderIpFile));
        String line;
        while ((line = in.readLine()) != null) {
            if (!line.startsWith("#")) {
                line = line.trim();

                if (!line.equals("") && !Character.isDigit(line.charAt(0))) {
                    // is a hostname
                    // add this functionality later...
                } else if (!line.equals("")) {
                    ips.add(line);
                    // is full v4 ip (too tired to deal with v6)...
                }
            } else {
                //   ua.add(line.replaceFirst("#","").replaceFirst("UA","").trim());
                // ... add this functionality later
            }
        }
        in.close();
        return ips;
    }

    /**
     * Get an immutable Set representing all the Spider Addresses here
     *
     * @return
     */
    public static Set<String> getSpiderIpAddresses() {

        loadSpiderIpAddresses();
        return table.toSet();
    }

    /*
        private loader to populate the table from files.
     */

    private static void loadSpiderIpAddresses() {


        if (table == null) {
            table = new IPTable();

            String filePath = ConfigurationManager.getProperty("dspace.dir");

            try {
                File spidersDir = new File(filePath, "config/spiders");

                if (spidersDir.exists() && spidersDir.isDirectory()) {
                    for (File file : spidersDir.listFiles()) {
                        for (String ip : readIpAddresses(file)) {
                            table.add(ip);
                        }
                        log.info("Loaded Spider IP file: " + file);
                    }
                } else {
                    log.info("No spider file loaded");
                }


            }
            catch (Exception e) {
                log.error("Error Loading Spiders:" + e.getMessage(), e);
            }


        }

    }


    /**
     * Check individual IP is a spider.
     *
     * @param ip
     * @return if is spider IP
     */
    public static boolean isSpider(String ip) {

        if (table == null) {
            SimpleSpiderDetector.loadSpiderIpAddresses();
        }

        try {
            if (table.contains(ip)) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return false;


    }

}
