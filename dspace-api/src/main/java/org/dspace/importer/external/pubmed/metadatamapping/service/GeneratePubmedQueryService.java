/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.pubmed.metadatamapping.service;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.Query;
import org.dspace.importer.external.metadatamapping.service.GenerateQueryService;

import java.util.List;

/**
 * @author jonas - jonas@atmire.com on 06/11/15.
 */
public class GeneratePubmedQueryService implements GenerateQueryService {


    /*
    * Create a Query object based on a given item.
    * If the item has at least 1 value for dc.identifier.doi, the first one will be used.
    * If no DOI is found, the title will be used.
    * When no DOI or title is found, an null object is returned instead.
    */
    @Override
    public Query generateQueryForItem(Item item) throws MetadataSourceException {
        Query query = new Query();

        // Retrieve an instance of the ItemService to access business calls on an item.
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        List<MetadataValue> doi = itemService.getMetadata(item, "dc", "identifier", "doi", Item.ANY);

        if(doi.size()>0){
            query.addParameter("term", doi.get(0).getValue());
            query.addParameter("field","ELocationID");
            return query;
        }

        List<MetadataValue> title = itemService.getMetadata(item, "dc", "title", null, Item.ANY);

        if(title.size()>0) {
            query.addParameter("term", title.get(0).getValue());
            query.addParameter("field","title");
            return query;
        }
        return null;
    }
}
