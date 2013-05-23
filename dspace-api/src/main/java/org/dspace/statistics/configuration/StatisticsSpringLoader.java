/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.configuration;

import java.io.File;
import java.net.MalformedURLException;
import org.dspace.kernel.config.SpringLoader;
import org.dspace.services.ConfigurationService;

/**
 * Supply paths for configuring statistical code.
 *
 * @author mwood
 */
public class StatisticsSpringLoader
        implements SpringLoader
{
    /*
     * (non-Javadoc) @see
     * org.dspace.kernel.config.SpringLoader#getResourcePaths(org.dspace.services.ConfigurationService)
     */
    @Override
    public String[] getResourcePaths(ConfigurationService configurationService)
    {
        StringBuffer filePath = new StringBuffer();
        filePath.append(configurationService.getProperty("dspace.dir"));
        filePath.append(File.separator);
        filePath.append("config");
        filePath.append(File.separator);
        filePath.append("spring");
        filePath.append(File.separator);
        filePath.append("statistics");
        filePath.append(File.separator);

        try
        {
            return new String[]
                    {
                        new File(filePath.toString()).toURI().toURL().toString()
                        + File.separator + XML_SUFFIX
                    };
        } catch (MalformedURLException e)
        {
            return new String[0];
        }
    }
}
