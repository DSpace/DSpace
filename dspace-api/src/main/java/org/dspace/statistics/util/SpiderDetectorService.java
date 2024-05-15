/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Interface to implement a SpiderDetectorService.
 *
 * @author frederic at atmire.com
 */
public interface SpiderDetectorService {

    /**
     * Service Method for testing spiders against existing spider files.
     *
     * @param clientIP address of the client.
     * @param proxyIPs comma-list of X-Forwarded-For addresses, or null.
     * @param hostname domain name of host, or null.
     * @param agent    User-Agent header value, or null.
     * @return true if the client matches any spider characteristics list.
     */
    public boolean isSpider(String clientIP, String proxyIPs, String hostname, String agent);

    /**
     * Service Method for testing spiders against existing spider files.
     *
     * @param request the current HTTP request.
     * @return true|false if the request was detected to be from a spider.
     */
    public boolean isSpider(HttpServletRequest request);

    /**
     * Check individual IP is a spider.
     *
     * @param ip the IP address to be checked.
     * @return if is spider IP
     */
    public boolean isSpider(String ip);

    /**
     *  Loader to populate the IP address table from files.
     */
    public void loadSpiderIpAddresses();

    /**
     * Utility method which reads lines from a file & returns them in a Set.
     *
     * @param patternFile the location of our spider file
     * @return a vector full of patterns
     * @throws IOException could not happen since we check the file be4 we use it
     */
    public Set<String> readPatterns(File patternFile)
        throws IOException;

    /**
     * @return the table of IP net blocks.
     */
    public IPTable getTable();
}
