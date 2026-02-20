/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.relationship;

import java.sql.SQLException;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link RelationshipItemAuthorizer} that check if the
 * current user has WRITE policies on the given item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class RelationshipItemWritePolicyAuthorizer implements RelationshipItemAuthorizer {

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean canHandleRelationshipOnItem(Context context, Item item) {
        try {
            return authorizeService.authorizeActionBoolean(context, item, Constants.WRITE);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public void setAuthorizeService(AuthorizeService authorizeService) {
        this.authorizeService = authorizeService;
    }

}
