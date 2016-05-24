/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.transform;

import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;

/**
 * Represent a service to generate a query based on an item
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public interface GenerateQueryService {

    /* Create a Query object based on a given item.
     * Implementations need to make their own decisions as what to add in or leave out of the query
     */
    public Query generateQueryForItem(Item item) throws MetadataSourceException;
}
