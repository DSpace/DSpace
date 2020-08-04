/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "move" PATCH operation.
 *
 * It is possible to rearrange the metadata values using the move operation. For
 * instance to put the 3rd author as 1st author you need to run:
 *
 * <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/<:id-workspaceitem> -H "
 * Content-Type: application/json" -d '[{ "op": "move", "from": "
 * /sections/traditionalpageone/dc.contributor.author/2", "path": "
 * /sections/traditionalpageone/dc.contributor.author/0"}]'
 * </code>
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class ItemMetadataValueMovePatchOperation extends MetadataValueMovePatchOperation<Item> {

    @Autowired
    ItemService itemService;

    @Override
    void move(Context context, Request currentRequest, InProgressSubmission source, String path, String from)
        throws Exception {
        String[] splitTo = getAbsolutePath(path).split("/");

        String evalFrom = getAbsolutePath(from);
        String[] splitFrom = evalFrom.split("/");
        String metadata = splitFrom[0];

        if (splitTo.length > 1) {
            String stringTo = splitTo[1];
            if (splitFrom.length > 1) {
                String stringFrom = splitFrom[1];

                int intTo = Integer.parseInt(stringTo);
                int intFrom = Integer.parseInt(stringFrom);
                moveValue(context, source.getItem(), metadata, intFrom, intTo);
            }
        }

    }

    @Override
    protected ItemService getDSpaceObjectService() {
        return itemService;
    }

}
