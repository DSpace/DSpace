/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.service;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 23/10/12
 * Time: 09:49
 */
public interface MetadataProcessorService {

    /* Process a given metadataValue to make them compliant to specific rules.
     * Implementations should regulate their own processing as to what is required for a specific cause
     */
    public String processMetadataValue(String value);
}
