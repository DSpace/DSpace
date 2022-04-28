/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * A condition that accepts a group and action parameter and returns true if the group
 * can perform the action on a given item
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class ReadableByGroupCondition extends AbstractCondition {
    private final static Logger log = LogManager.getLogger();

    // Authorize service
    AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    /**
     * Return true if this item allows a specified action (eg READ, WRITE, ADD) by a specified group
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of evaluation
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {

        String group = (String)getParameters().get("group");
        String action = (String)getParameters().get("action");

        try {
            List<ResourcePolicy> policies = authorizeService
                .getPoliciesActionFilter(context, item, Constants.getActionID(action));
            for (ResourcePolicy policy : policies) {
                if (policy.getGroup().getName().equals(group)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error("Error trying to read policies for " + item.getHandle() + ": " + e.getMessage());
            throw new LogicalStatementException(e);
        }
        log.debug("item " + item.getHandle() + " not readable by anonymous group");

        return false;
    }
}
