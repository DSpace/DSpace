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

    public Query generateQueryForItem(Item item) throws MetadataSourceException;
}
