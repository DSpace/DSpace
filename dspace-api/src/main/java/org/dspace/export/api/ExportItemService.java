/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.api;

import java.util.List;

/**
 * Service interface class for item exportation.
 * 
 * @author João Melo <jmelo@lyncode.com>
 * 
 */
public interface ExportItemService 
{
    /**
     * Get the list of all available export providers based on the config file.
     * 
     * @return list of export providers
     */
    List<ExportItemProvider> getProviders();

    /**
     * Get a specific export provider based on its string ID.
     * 
     * @param id of the export provider
     * @return the found export provider
     */
    ExportItemProvider getProvider(String id);
}
