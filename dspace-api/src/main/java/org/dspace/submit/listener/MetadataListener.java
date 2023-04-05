/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.listener;

import java.util.Set;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;

/**
 * The interface to implement to support the ExtractMetadata enrichment step
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface MetadataListener {
    /**
     * Return the list of metadata that should be monitored as change to them could
     * allow the service to retrieve an ExternalDataObject to enrich the current
     * item
     *
     * @return the list of metadata to monitor
     */
    public Set<String> getMetadataToListen();

    /**
     * Retrieve an ExternalDataObject to enrich the current item using the current
     * metadata and the information about which listened metadata are changed
     * 
     * @param context         the DSpace Context Object
     * @param item            the item in its current status
     * @param changedMetadata the list of listened metadata that are changed
     * @return an ExternalDataObject that can be used to enrich the current item
     */
    public ExternalDataObject getExternalDataObject(Context context, Item item, Set<String> changedMetadata);

}
