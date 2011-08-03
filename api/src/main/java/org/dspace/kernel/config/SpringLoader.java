/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel.config;

import org.dspace.services.ConfigurationService;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 3-aug-2011
 * Time: 11:19:33
 *
 * Interface that is used so that modules can determine their own spring file locations
 */
public interface SpringLoader {

    public final String XML_SUFFIX = "*.xml";

    /**
     * Returns all the locations that contain spring files
     * @param configurationService the dspace configuration service
     * @return an array containing spring file locations
     */
    public String[] getResourcePaths(ConfigurationService configurationService);


}
