/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import org.dspace.content.Item;

/**
 * This class has been initially developed by Graham Triggs, we have moved to a
 * CILEA package to make more clear that it is not included in the org.dspace
 * sourcecode
 * 
 * 
 * This defines the interface that must be implemented by 'virtual field' ingesters.
 * 
 * IMPORTANT: Implementing classes must not retain state between calls to the methods defined here.
 * If state is retained, bad things will happen.
 * 
 * @author grahamt
 */
public interface VirtualFieldIngester {
    /**
     * This method is used to add values to the virtual field. The implementing
     * method may choose to process the value and add something to the items
     * metadata, or simply ignore it - all values that are passed into this method
     * are first added to the fieldCache, using fieldName as a key, hence you will
     * be able to retrieve these values later on in finalizeItem()
     * 
     * @param item
     * @param fieldName
     * @param value
     * @return
     */
    public boolean addMetadata(Item item, String fieldName, String value);

    /**
     * If the processor has been referenced in an ingest, this method will be called
     * at the end of the ingest process. This gives a chance to process across a
     * number of metadata values. All values discovered during the ingest will be in
     * the fieldCache map.
     * 
     * @param item
     * @return
     */
    public boolean finalizeItem(Item item);
}