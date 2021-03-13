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
 * Submission "remove" PATCH operation.
 *
 * Path used to remove a <b>specific metadata value</b> using index:
 * "/sections/<:name-of-the-form>/<:metadata>/<:idx-zero-based>"
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/<:id-workspaceitem> -H "
 * Content-Type: application/json" -d '[{ "op": "remove", "path": "
 * /sections/traditionalpageone/dc.title/1"}]'
 * </code>
 *
 * Path used to remove <b>all the metadata values</b> for a specific metadata key:
 * "/sections/<:name-of-the-form>/<:metadata>"
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/<:id-workspaceitem> -H "
 * Content-Type: application/json" -d '[{ "op": "remove", "path": "
 * /sections/traditionalpageone/dc.title"}]'
 * </code>
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class ItemMetadataValueRemovePatchOperation extends MetadataValueRemovePatchOperation<Item> {

    @Autowired
    ItemService itemService;

    @Override
    void remove(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
        throws Exception {
        String[] split = getAbsolutePath(path).split("/");
        if (split.length == 1) {
            deleteValue(context, source.getItem(), split[0], -1);
        } else {
            Integer toDelete = Integer.parseInt(split[1]);
            deleteValue(context, source.getItem(), split[0], toDelete);
        }
    }

    @Override
    protected ItemService getDSpaceObjectService() {
        return itemService;
    }

}
