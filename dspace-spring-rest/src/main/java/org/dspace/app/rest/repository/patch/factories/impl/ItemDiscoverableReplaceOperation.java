/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import java.sql.SQLException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Item resource patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/item/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /discoverable", "value": "true|false"]'
 * </code>
 *
 *  @author Michael Spalti
 */
@Component
public class ItemDiscoverableReplaceOperation extends PatchOperation<Item, String>
        implements ResourcePatchOperation<Item> {

    @Autowired
    ItemService is;

    private static final Logger log = Logger.getLogger(ItemDiscoverableReplaceOperation.class);

    /**
     * Sets discoverable field on the item.
     *
     * @param item
     * @param context

     * @throws SQLException
     * @throws AuthorizeException
     */
    public void perform(Context context, Item item, Operation operation)
            throws SQLException, AuthorizeException, PatchBadRequestException {

        replace(context, item, operation.getValue());

    }

    private void replace(Context context, Item item, Object value)
            throws SQLException, AuthorizeException {

        checkOperationValue((String) value);
        Boolean discoverable = BooleanUtils.toBooleanObject((String) value);

        if (discoverable == null) {
            // make sure string was converted to boolean.
            throw new PatchBadRequestException(
                    "Boolean value not provided for discoverable operation.");
        }

        try {
            item.setDiscoverable(discoverable);
            is.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw e;
        }

    }

    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    protected Class<String> getClassForEvaluation() {
        return String.class;
    }

}
