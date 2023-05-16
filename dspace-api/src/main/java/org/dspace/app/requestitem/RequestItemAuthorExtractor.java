/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.lang.NonNull;

/**
 * Interface to abstract the strategy for selecting the author to contact for
 * request copy.
 *
 * @author Andrea Bollini
 */
public interface RequestItemAuthorExtractor {
    /**
     * Retrieve the author to contact for requesting a copy of the given item.
     *
     * @param context DSpace context object
     * @param item item to request
     * @return Names and email addresses to send the request to.
     * @throws SQLException if database error
     */
    @NonNull
    public List<RequestItemAuthor> getRequestItemAuthor(Context context, Item item)
            throws SQLException;
}
