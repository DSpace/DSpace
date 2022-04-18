/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

<<<<<<< HEAD
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Interface to implement a SpiderDetectorService
=======
import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Interface to implement a SpiderDetectorService
 *
>>>>>>> dspace-7.2.1
 * @author frederic at atmire.com
 */
public interface SpiderDetectorService {

    public boolean isSpider(String clientIP, String proxyIPs, String hostname, String agent);

    public boolean isSpider(HttpServletRequest request);

    public boolean isSpider(String ip);

    public void loadSpiderIpAddresses();

    public Set<String> readPatterns(File patternFile)
<<<<<<< HEAD
            throws IOException;
=======
        throws IOException;
>>>>>>> dspace-7.2.1

    public IPTable getTable();

}
