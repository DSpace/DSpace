/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.SolrLogger;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpiderDetector is used to find IP's that are spiders...
 * In future someone may add UserAgents and Host Domains
 * to the detection criteria here.
 *
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 * @author Mark Diggory (mdiggory at atmire.com)
 * @author Kevin Van Ransbeeck at atmire.com
 */
public class SpiderDetector {

    private static Logger log = Logger.getLogger(SpiderDetector.class);

    /**
     * Sparse HAshTable structure to hold IP Address Ranges.
     */
    private static IPTable table = null;
    private static Set<Pattern> spidersRegex = null;
    private static Set<String> spidersMatched = null;

    /**
     * Utility method which Reads the ip addresses out a file & returns them in a Set
     *
     * @param spiderIpFile the location of our spider file
     * @return a vector full of ip's
     * @throws java.io.IOException could not happen since we check the file be4 we use it
     */
    public static Set<String> readIpAddresses(File spiderIpFile) throws IOException {
        Set<String> ips = new HashSet<String>();

        if (!spiderIpFile.exists() || !spiderIpFile.isFile()) {
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
     * @return Set<String> setOfIpAddresses
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
            } catch (Exception e) {
                log.error("Error Loading Spiders:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Static Service Method for testing spiders against existing spider files.
     * <p/>
     * In the future this will be extended to support User Agent and
     * domain Name detection.
     * <p/>
     * In future spiders HashSet may be optimized as byte offset array to
     * improve performance and memory footprint further.
     *
     * @param request
     * @return true|false if the request was detected to be from a spider
     */
    public static boolean isSpider(HttpServletRequest request) {
        /*
        * 1) If the IP address matches the spider IP addresses (this is the current implementation)
        */
        boolean checkSpidersIP = ConfigurationManager.getBooleanProperty("stats","spider.ipmatch.enabled", true);
        if (checkSpidersIP) {
            if (SolrLogger.isUseProxies() && request.getHeader("X-Forwarded-For") != null) {
                /* This header is a comma delimited list */
                for (String xfip : request.getHeader("X-Forwarded-For").split(",")) {
                    if (isSpider(xfip)) {
                        log.debug("spider.ipmatch");
                        return true;
                    }
                }
            } else if (isSpider(request.getRemoteAddr())) {
                log.debug("spider.ipmatch");
                return true;
            }
        }
        /*
         * 2) if the user-agent header is empty - DISABLED BY DEFAULT -
         */
        boolean checkSpidersEmptyAgent = ConfigurationManager.getBooleanProperty("stats","spider.agentempty.enabled", false);
        if (checkSpidersEmptyAgent) {
            if (request.getHeader("user-agent") == null || request.getHeader("user-agent").length() == 0) {
                log.debug("spider.agentempty");
                return true;
            }
        }
        /*
         * 3) if the user-agent corresponds to one of the regexes at http://www.projectcounter.org/r4/COUNTER_robot_txt_list_Jan_2011.txt
         */
        boolean checkSpidersTxt = ConfigurationManager.getBooleanProperty("stats","spider.agentregex.enabled", true);
        if (checkSpidersTxt) {
            String userAgent = request.getHeader("user-agent");

            if (userAgent != null && !userAgent.equals("")) {
                return isSpiderRegex(userAgent);
            }
        }
        return false;
    }

    /**
     * Check individual IP is a spider.
     *
     * @param ip
     * @return if is spider IP
     */
    public static boolean isSpider(String ip) {
        if (table == null) {
            SpiderDetector.loadSpiderIpAddresses();
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

    /**
     * Checks the user-agent string vs a set of known regexes from spiders
     * A second Set is kept for fast-matching.
     * If a user-agent is matched once, it is added to this set with "known agents".
     * If this user-agent comes back later, we can do a quick lookup in this set,
     * instead of having to loop over the entire set with regexes again.
     *
     * @param userAgent String
     * @return true if the user-agent matches a regex
     */
    public static boolean isSpiderRegex(String userAgent) {
        if (spidersMatched != null && spidersMatched.contains(userAgent)) {
            log.debug("spider.agentregex");
            return true;
        } else {
            if (spidersRegex == null)
                loadSpiderRegexFromFile();

            if (spidersRegex != null) {
                for (Object regex : spidersRegex.toArray()) {
                    Matcher matcher = ((Pattern) regex).matcher(userAgent);
                    if (matcher.find()) {
                        if (spidersMatched == null) {
                            spidersMatched = new HashSet<String>();
                        }
                        if (spidersMatched.size() >= 100) {
                            spidersMatched.clear();
                        }
                        spidersMatched.add(userAgent);
                        log.debug("spider.agentregex");
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Populate static Set spidersRegex from local txt file.
     * Original file downloaded from http://www.projectcounter.org/r4/COUNTER_robot_txt_list_Jan_2011.txt during build
     */
    public static void loadSpiderRegexFromFile() {
        spidersRegex = new HashSet<Pattern>();
        String spidersTxt = ConfigurationManager.getProperty("stats","spider.agentregex.regexfile");
        DataInputStream in = null;
        try {
            FileInputStream fstream = new FileInputStream(spidersTxt);
            in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                spidersRegex.add(Pattern.compile(strLine, Pattern.CASE_INSENSITIVE));
            }
            log.info("Loaded Spider Regex file: " + spidersTxt);
        } catch (FileNotFoundException e) {
            log.error("File with spiders regex not found @ " + spidersTxt);
        } catch (IOException e) {
            log.error("Could not read from file " + spidersTxt);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.error("Could not close file " + spidersTxt);
            }
        }
    }
}
