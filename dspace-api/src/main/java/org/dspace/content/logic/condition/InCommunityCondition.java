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

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * A condition that accepts a list of community handles and returns true
 * if the item belongs to any of them.
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class InCommunityCondition extends AbstractCondition {
    private static Logger log = Logger.getLogger(InCommunityCondition.class);

    /**
     * Return true if item is in one of the specified collections
     * Return false if not
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of evaluation
     * @throws LogicalStatementException
     */
    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {

        List<String> communityHandles = (List<String>)getParameters().get("communities");
        List<Collection> itemCollections = item.getCollections();

        for (Collection collection : itemCollections) {
            try {
                List<Community> communities = collection.getCommunities();
                for (Community community : communities) {
                    if (communityHandles.contains(community.getHandle())) {
                        return true;
                    }
                }
            } catch (SQLException e) {
                log.error(e.getMessage());
                throw new LogicalStatementException(e);
            }
        }

        return false;
    }
}
