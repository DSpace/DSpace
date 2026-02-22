/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.relationship;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link RelationshipItemAuthorizer} that check if the
 * current user is the owner of the given item.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RelationshipItemOwnerAuthorizer implements RelationshipItemAuthorizer {

    @Autowired
    private EPersonService ePersonService;

    @Override
    public boolean canHandleRelationshipOnItem(Context context, Item item) {
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        return ePersonService.isOwnerOfItem(currentUser, item);
    }

    public void setEPersonService(EPersonService ePersonService) {
        this.ePersonService = ePersonService;
    }

}
