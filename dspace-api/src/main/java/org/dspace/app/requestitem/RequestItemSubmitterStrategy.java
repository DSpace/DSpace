/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Basic strategy that looks to the original submitter.
 *
 * @author Andrea Bollini
 */
public class RequestItemSubmitterStrategy implements RequestItemAuthorExtractor {
    private static final Logger LOG = LogManager.getLogger();

    public RequestItemSubmitterStrategy() {
    }

    /**
     * Returns the submitter of an Item as RequestItemAuthor or null if the
     * Submitter is deleted.
     *
     * @return The submitter of the item or null if the submitter is deleted
     * @throws SQLException if database error
     */
    @Override
    public RequestItemAuthor getRequestItemAuthor(Context context, Item item)
            throws SQLException {
        EPerson submitter = item.getSubmitter();
        RequestItemAuthor author = null;
        if (null != submitter) {
            author = new RequestItemAuthor(
                    submitter.getFullName(), submitter.getEmail());
        }
        return author;
    }

    @Override
    public boolean isAuthorized(Context context, Item item) {
        RequestItemAuthor authorizer;
        try {
            authorizer = getRequestItemAuthor(context, item);
        } catch (SQLException ex) {
            LOG.warn("Failed to find an authorizer:  {}", ex::getMessage);
            return false;
        }
        EPerson user = context.getCurrentUser();
        if (null == user) {
            return false;
        }
        return authorizer.getEmail().equals(user.getEmail());
    }
}
