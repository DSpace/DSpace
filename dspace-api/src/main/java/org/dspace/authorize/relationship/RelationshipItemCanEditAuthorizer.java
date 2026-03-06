/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.relationship;

import org.dspace.content.Item;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link RelationshipItemAuthorizer} that verify if the
 * current user can edit the given item based on the configured EditItemMode.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RelationshipItemCanEditAuthorizer implements RelationshipItemAuthorizer {

    @Autowired
    private EditItemModeService editItemModeService;

    @Override
    public boolean canHandleRelationshipOnItem(Context context, Item item) {
        return editItemModeService.canEdit(context, item);
    }

    public void setEditItemModeService(EditItemModeService editItemModeService) {
        this.editItemModeService = editItemModeService;
    }

}
