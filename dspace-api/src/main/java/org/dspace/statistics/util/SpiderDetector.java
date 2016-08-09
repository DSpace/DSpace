/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SpiderDetector is used to find IP's that are spiders...
 * In future someone may add Host Domains
 * to the detection criteria here.
 *
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 * @author Mark Diggory (mdiggory at atmire.com)
 */
public class SpiderDetector {

    private static final Logger log = LoggerFactory.getLogger(SpiderDetector.class);

    private static Boolean useProxies;

    /**
     * Sparse HashTable structure to hold IP address ranges.
     */
    private static IPTable table = null;

    /** Collection of regular expressions to match known spiders' agents. */
    private static final List<Pattern> agents
            = Collections.synchronizedList(new ArrayList<Pattern>());

    /** Collection of regular expressions to match known spiders' domain names. */
    private static final List<Pattern> domains
            = Collections.synchronizedList(new ArrayList<Pattern>());

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
        Set<String> patterns = new HashSet<>();

        if (!patternFile.exists() || !patternFile.isFile())
        {
            return patterns;
        }

        //Read our file & get all them patterns.
        try (BufferedReader in = new BufferedReader(new FileReader(patternFile)))
        {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("#")) {
                    line = line.trim();

                    if (!line.equals("")) {
                        patterns.add(line);
                    }
                } else {
                    //   ua.add(line.replaceFirst("#","").replaceFirst("UA","").trim());
                    // ... add this functionality later
                }
            }
        }
        return patterns;
    }

    /**
     * Get an immutable Set representing all the Spider Addresses here
     *
     * @return a set of IP addresses as strings
     */
    public static Set<String> getSpiderIpAddresses() {

        loadSpiderIpAddresses();
        return table.toSet();
    }

    /*
     *  private loader to populate the table from files.
     */

    private synchronized static void loadSpiderIpAddresses() {

        if (table == null) {
            table = new IPTable();

            String filePath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir");

            try {
                File spidersDir = new File(filePath, "config/spiders");

                if (spidersDir.exists() && spidersDir.isDirectory()) {
                    for (File file : spidersDir.listFiles()) {
                        if (file.isFile())
                        {
                            for (String ip : readPatterns(file)) {
                                log.debug("Loading {}", ip);
                                if (!Character.isDigit(ip.charAt(0)))
                                {
                                    try {
                                        ip = DnsLookup.forward(ip);
                                        log.debug("Resolved to {}", ip);
                                    } catch (IOException e) {
                                        log.warn("Not loading {}:  {}", ip, e.getMessage());
                                        continue;
                                    }
                                }
                                table.add(ip);
                            }
                            log.info("Loaded Spider IP file: " + file);
                        }
                    }
                } else {
                    log.info("No spider file loaded");
                }
            }
            catch (IOException | IPTable.IPFormatException e) {
                log.error("Error Loading Spiders:" + e.getMessage(), e);
            }

        }

    }

    /**
     * Load agent name patterns from all files in a single subdirectory of config/spiders.
     *
     * @param directory simple directory name (e.g. "agents").
     *      "${dspace.dir}/config/spiders" will be prepended to yield the path to
     *      the directory of pattern files.
     * @param patternList patterns read from the files in {@code directory} will
     *      be added to this List.
     */
    private static void loadPatterns(String directory, List<Pattern> patternList)
    {
        String dspaceHome = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir");
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
                    log.error("Patterns not read from {}:  {}",
                            file.getPath(), ex.getMessage());
                    continue;
                }
                for (String pattern : patterns)
                {
                    patternList.add(Pattern.compile(pattern,Pattern.CASE_INSENSITIVE));
                }
                log.info("Loaded pattern file:  {}", file.getPath());
            }
        }
        else
        {
            log.info("No patterns loaded from {}", patternsDir.getPath());
        }
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
            synchronized(agents)
            {
                if (agents.isEmpty())
                    loadPatterns("agents", agents);
            }
            for (Pattern candidate : agents)
            {
		// prevent matcher() invocation from a null Pattern object
                if (null != candidate && candidate.matcher(agent).find())
                {
                    return true;
                }
            }
        }

        // No.  See if any IP addresses match
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

        // No.  See if any DNS names match
        if (null != hostname)
        {
            synchronized(domains)
            {
                if (domains.isEmpty())
                    loadPatterns("domains", domains);
            }
            for (Pattern candidate : domains)
            {
		// prevent matcher() invocation from a null Pattern object
		if (null != candidate && candidate.matcher(hostname).find())
                {
                    return true;
                }
            }
        }

        // Not a known spider.
        return false;
    }

    /**
     * Static Service Method for testing spiders against existing spider files.
     *
     * @param request
     * @return true|false if the request was detected to be from a spider.
     */
    public static boolean isSpider(HttpServletRequest request)
    {
        return isSpider(request.getRemoteAddr(),
                request.getHeader("X-Forwarded-For"),
                request.getRemoteHost(),
                request.getHeader("User-Agent"));
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

    private static boolean isUseProxies() {
        if(useProxies == null) {
            useProxies = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("useProxies");
        }

        return useProxies;
    }

}
