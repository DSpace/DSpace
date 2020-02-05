/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Utility class providing methods to deal with generic DSpace Object of unknown type
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
@Component
public class DSpaceObjectUtils {

    @Autowired
    private ContentServiceFactory contentServiceFactory;

    /**
     * Retrieve a DSpaceObject from its uuid. As this method need to iterate over all the different services that
     * support concrete class of DSpaceObject it has poor performance. Please consider the use of the direct service
     * (ItemService, CommunityService, etc.) if you know in advance the type of DSpaceObject that you are looking for
     * 
     * @param context
     *            DSpace context
     * @param uuid
     *            the uuid to lookup
     * @return the DSpaceObject if any with the supplied uuid
     * @throws SQLException
     */
    public DSpaceObject findDSpaceObject(Context context, UUID uuid) throws SQLException {
        for (DSpaceObjectService<? extends DSpaceObject> dSpaceObjectService :
                              contentServiceFactory.getDSpaceObjectServices()) {
            DSpaceObject dso = dSpaceObjectService.find(context, uuid);
            if (dso != null) {
                return dso;
            }
        }
        return null;
    }

}
