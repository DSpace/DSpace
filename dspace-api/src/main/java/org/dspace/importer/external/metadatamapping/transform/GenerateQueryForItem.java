/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.transform;

import java.util.*;
import org.dspace.content.*;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.importer.external.datamodel.*;
import org.dspace.importer.external.exception.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 28/09/15
 * Time: 17:31
 */
public class GenerateQueryForItem implements GenerateQueryService {

    private String metadateField;

    public GenerateQueryForItem(String metadateField) {
        this.metadateField = metadateField;
    }

    @Override
    public Query generateQueryForItem(Item item) throws MetadataSourceException {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        List<MetadataValue> value=itemService.getMetadataByMetadataString(item, metadateField);
        if (value.size()>0){
            Query query = new Query();
            query.addParameter("query","doi("+ value.get(0).getValue() +")");
            return query;
        } else {
            return null;
        }
    }
}
