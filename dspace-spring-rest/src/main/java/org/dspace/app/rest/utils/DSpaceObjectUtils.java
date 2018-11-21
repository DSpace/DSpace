/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DSpaceObjectUtils {

    @Autowired
    ContentServiceFactory contentServiceFactory;

    public DSpaceObject replaceMetadataValues(Context context,
                                              DSpaceObject dSpaceObject,
                                              List<MetadataEntryRest> metadataEntryRestList)
        throws SQLException, AuthorizeException {
        DSpaceObjectService dSpaceObjectService = contentServiceFactory.getDSpaceObjectService(dSpaceObject);
        dSpaceObjectService.clearMetadata(context, dSpaceObject, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (MetadataEntryRest mer : metadataEntryRestList) {
            String[] metadatakey = mer.getKey().split("\\.");
            dSpaceObjectService.addMetadata(context, dSpaceObject, metadatakey[0], metadatakey[1],
                           metadatakey.length == 3 ? metadatakey[2] : null, mer.getLanguage(), mer.getValue());
        }
        dSpaceObjectService.update(context, dSpaceObject);
        return dSpaceObject;
    }
}
