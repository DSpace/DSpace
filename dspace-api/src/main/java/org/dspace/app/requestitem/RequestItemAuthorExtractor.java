/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface to abstract the strategy for select the author to contact for
 * request copy
 *
 * @author Andrea Bollini
 */
public interface RequestItemAuthorExtractor {

    /**
     * Retrieve the auhtor to contact for a request copy of the give item.
     *
     * @param context DSpace context object
     * @param item item to request
     * @return An object containing name an email address to send the request to
     *         or null if no valid email address was found.
     * @throws SQLException if database error
     */
    public RequestItemAuthor getRequestItemAuthor(Context context, Item item) throws SQLException;
}
