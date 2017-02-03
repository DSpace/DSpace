/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.SolrLogger;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SpiderDetector is used to find IP's that are spiders...
 * In future someone may add UserAgents and Host Domains
 * to the detection criteria here.
 *
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 * @author Mark Diggory (mdiggory at atmire.com)
 */
public class SpiderDetector {

    private static Logger log = Logger.getLogger(SpiderDetector.class);

    /**
     * Sparse HAshTable structure to hold IP Address Ranges.
     */
    private static IPTable table = null;

    /** Collection of regular expressions to match known spiders' agents. */
    private static List<Pattern> agents = new ArrayList<Pattern>();
    /** Collection of regular expressions to match known spiders' domain names. */
    private static List<Pattern> domains = new ArrayList<Pattern>();

    private static Boolean useProxies;
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

        if (SolrLogger.isUseProxies() && request.getHeader("X-Forwarded-For") != null) {
            /* This header is a comma delimited list */
            for (String xfip : request.getHeader("X-Forwarded-For").split(",")) {
                if (isSpider(xfip))
                {
                    return true;
                }
            }
        }

        return isSpider(request.getRemoteAddr(),
                request.getHeader("X-Forwarded-For"),
                request.getRemoteHost(),
                request.getHeader("User-Agent"));

    }

    /**
     * Static Service Method for testing spiders against existing spider files.
     * <p>
     * In future spiders HashSet may be optimized as byte offset array to
     * improve performance and memory footprint further.
     *
     * @param clientIP address of the client.
     * @param proxyIPs comma-list of X-Forwarded-For addresses, or null.
     * @param hostname domain name of host, or null.
     * @param agent User-Agent header value, or null.
     * @return true if the client matches any spider characteristics list.
     */
    public static boolean isSpider(String clientIP, String proxyIPs,
                                   String hostname, String agent)
    {
        // See if any agent patterns match
        if (null != agent)
        {
            if (agents.isEmpty())
                loadPatterns("agents", agents);

            for (Pattern candidate : agents)
            {
                if (candidate != null && candidate.matcher(agent).find())
                {
                    return true;
                }
            }
        }

        // No. See if any IP addresses match
        if (isUseProxies() && proxyIPs != null) {
            /* This header is a comma delimited list */
            for (String xfip : proxyIPs.split(",")) {
                if (isSpider(xfip))
                {
                    return true;
                }
            }
        }

        if (isSpider(clientIP))
            return true;

        // No. See if any DNS names match
        if (null != hostname)
        {
            if (domains.isEmpty())
            {
                loadPatterns("domains", domains);
            }

            for (Pattern candidate : domains)
            {
                if (candidate.matcher(hostname).find())
                {
                    return true;
                }
            }
        }

        // Not a known spider.
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

    /*
    * private loader to populate the table from files.
    */

    private static void loadSpiderIpAddresses() {

        if (table == null) {
            table = new IPTable();

            String filePath = ConfigurationManager.getProperty("dspace.dir");

            try {
                File spidersDir = new File(filePath, "config/spiders");

                if (spidersDir.exists() && spidersDir.isDirectory()) {
                    for (File file : spidersDir.listFiles()) {
                        if (file.isFile())
                        {
                            for (String ip : readPatterns(file)) {
                                log.debug("Loading {}"+ ip);
                            
                                if (!Character.isDigit(ip.charAt(0)))
                                {
                                //   try {
                                //      ip = DnsLookup.forward(ip);
                                //      log.debug("Resolved to {}"+ip);
                                //  } catch (IOException e) {
                                //     log.warn("Not loading {}: {}"+ ip+ e.getMessage());
                                      continue;
                                //  }
                                } 
                                table.add(ip);
                            }

                            for (String pattern : readAgentCommentPatterns(file)) {
                                log.debug("Loading {}"+ pattern);
                                agents.add(Pattern.compile(pattern));
                            }

                            log.info("Loaded Spider IP file: " + file);
                        }
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
     * Load agent name patterns from all files in a single subdirectory of config/spiders.
     *
     * @param directory simple directory name (e.g. "agents").
     * "${dspace.dir}/config/spiders" will be prepended to yield the path to
     * the directory of pattern files.
     * @param patternList patterns read from the files in {@code directory} will
     * be added to this List.
     */
    private static void loadPatterns(String directory, List<Pattern> patternList)
    {
        String dspaceHome = ConfigurationManager.getProperty("dspace.dir");
        File spidersDir = new File(dspaceHome, "config/spiders");
        File patternsDir = new File(spidersDir, directory);
        if (patternsDir.exists() && patternsDir.isDirectory())
        {
            for (File file : patternsDir.listFiles())
            {
                Set<String> patterns;
                try
                {
                    patterns = readPatterns(file);
                } catch (IOException ex)
                {
                    log.error("Patterns not read from {}: {}"+
                            file.getPath()+ ex.getMessage());
                    continue;
                }
                for (String pattern : patterns)
                {
                    patternList.add(Pattern.compile(pattern));
                }
                log.info("Loaded pattern file: {}"+ file.getPath());
            }
        }
        else
        {
            log.info("No patterns loaded from {}"+ patternsDir.getPath());
        }
    }

    /**
     * Utility method which reads lines from a file & returns them in a Set.
     *
     * @param patternFile the location of our spider file
     * @return a vector full of patterns
     * @throws IOException could not happen since we check the file be4 we use it
     */
    public static Set<String> readPatterns(File patternFile)
            throws IOException
    {
        Set<String> patterns = new HashSet<String>();

        if (!patternFile.exists() || !patternFile.isFile())
        {
            return patterns;
        }

        //Read our file & get all them patterns.
        BufferedReader in = new BufferedReader(new FileReader(patternFile));
        String line;
        while ((line = in.readLine()) != null) {
            if (!line.startsWith("#")) {
                line = line.trim();

                if (!line.equals("")) {
                    patterns.add(line);
                }
            } else {
                // ... add this functionality later
            }
        }
        in.close();
        return patterns;
    }

    /**
     * Utility method which reads lines from a file & returns them in a Set.
     *
     * @param patternFile the location of our spider file
     * @return a vector full of patterns
     * @throws IOException could not happen since we check the file be4 we use it
     */
    public static Set<String> readAgentCommentPatterns(File patternFile)
            throws IOException
    {
        Set<String> patterns = new HashSet<String>();

        if (!patternFile.exists() || !patternFile.isFile())
        {
            return patterns;
        }

        //Read our file & get all them patterns.
        BufferedReader in = new BufferedReader(new FileReader(patternFile));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("# UA") || line.startsWith("#UA") ) {
                try{
                    String pattern = StringEscapeUtils.escapeJava(line.replaceFirst("#","").replaceFirst("UA","").replaceAll("\"","").trim().replaceAll(" ","\\ "));
                    pattern = Pattern.quote(pattern);
                    patterns.add(pattern);
                }catch (Exception e)
                {
                    log.error("error loading user agents" + line);
                }
                // ... add this functionality later
            }
        }
        in.close();
        return patterns;
    }

    private static boolean isUseProxies() {
        if(useProxies == null) {
            if ("true".equals(ConfigurationManager.getProperty("useProxies")))
            {
                useProxies = true;
            }
            else
            {
                useProxies = false;
            }
        }

        return useProxies;
    }
}
