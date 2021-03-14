/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.List;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Submission "replace" PATCH operation.
 *
 * The replace operation allows to replace existent information with new one.
 * Attempt to use the replace operation to set not yet initialized information
 * must return an error.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/<:id-workspaceitem> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /sections/traditionalpageone/dc.title/0", "value": {"value": "Add new
 * title", "language": "en"}}]'
 * </code>
 *
 * It is also possible to change only a single attribute of the {@link MetadataValueRest} (except the "place").
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/<:id-workspaceitem> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /sections/traditionalpageone/dc.title/0/language", "value": "it"}]'
 * </code>
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class ItemMetadataValueReplacePatchOperation extends MetadataValueReplacePatchOperation<Item> {

    @Autowired
    ItemService itemService;

    @Override
    void replace(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
        throws Exception {
        String[] split = getAbsolutePath(path).split("/");

        List<MetadataValue> metadataByMetadataString = itemService.getMetadataByMetadataString(source.getItem(),
                                                                                               split[0]);
        Assert.notEmpty(metadataByMetadataString);

        int index = Integer.parseInt(split[1]);
        // if split size is one so we have a call to initialize or replace
        if (split.length == 2) {
            MetadataValueRest obj = evaluateSingleObject((LateObjectEvaluator) value);
            replaceValue(context, source.getItem(), split[0], metadataByMetadataString, obj, index);
        } else {
            if (split.length == 3) {
                setDeclaredField(context, source.getItem(), value, split[0], split[2], metadataByMetadataString, index);
            }
        }
    }

    @Override
    protected ItemService getDSpaceObjectService() {
        return itemService;
    }
}
