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
import org.dspace.app.rest.exception.UnprocessableEntityException;
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
 * /withdrawn", "value": "true|false"]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class ItemWithdrawReplaceOperation extends PatchOperation<Item, String> {

    private static final Logger log = Logger.getLogger(ItemWithdrawReplaceOperation.class);

    @Autowired
    ItemService is;

    /**
     * Implementation of the PATCH replace operation.
     *
     * @param item
     * @param context
     * @param operation
     * @throws UnprocessableEntityException
     * @throws PatchBadRequestException
     * @throws SQLException
     * @throws AuthorizeException
     */
    @Override
    public void perform(Context context, Item item, Operation operation)
            throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {

        replace(context, item, (String) operation.getValue());

    }

    /**
     * Withdraws or reinstates the item based on boolean value provided in the patch request.
     *
     * @param item
     * @param context
     * @param value

     * @throws PatchBadRequestException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void replace(Context context, Item item,  Object value)
            throws PatchBadRequestException, SQLException, AuthorizeException {

        checkOperationValue((String) value);
        Boolean withdraw = BooleanUtils.toBooleanObject((String) value);

        if (withdraw == null) {
            // make sure string was converted to boolean.
            throw new PatchBadRequestException(
                    "Boolean value not provided for withdrawn operation..");
        }

        try {
            // This is a request to withdraw the item.
            if (withdraw) {
                // The DSO is currently not withdrawn and also not archived. Is this a possible situation?
                if (!item.isWithdrawn() && !item.isArchived()) {
                    throw new UnprocessableEntityException("Cannot withdraw item when it is not in archive.");
                }
                // Item is already withdrawn. No-op, 200 response.
                // (The operation is not idempotent since it results in a provenance note in the record.)
                if (item.isWithdrawn()) {
                    return;
                }
                is.withdraw(context, item);
            } else {
                // No need to reinstate item if it has not previously been not withdrawn.
                // No-op, 200 response. (The operation is not idempotent since it results
                // in a provenance note in the record.)
                if (!item.isWithdrawn()) {
                    return;
                }
                is.reinstate(context, item);
            }

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
