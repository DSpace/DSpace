/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.service;

import org.dspace.content.Item;
import org.dspace.importer.external.Query;
import org.dspace.importer.external.MetadataSourceException;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 14/12/12
 * Time: 11:44
 */
public interface GenerateQueryService {

    /* Create a Query object based on a given item.
     * Implementations need to make their own decisions as what to add in or leave out of the query
     */
    public Query generateQueryForItem(Item item) throws MetadataSourceException;
}
