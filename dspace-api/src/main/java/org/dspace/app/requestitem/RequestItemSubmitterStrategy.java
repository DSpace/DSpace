/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.lang.NonNull;

/**
 * Basic strategy that looks to the original submitter.
 *
 * @author Andrea Bollini
 */
public class RequestItemSubmitterStrategy implements RequestItemAuthorExtractor {
    public RequestItemSubmitterStrategy() {
    }

    /**
     * Returns the submitter of an Item as RequestItemAuthor or an empty List if
     * the Submitter is deleted.
     *
     * @return The submitter of the item or empty List if the submitter is deleted
     * @throws SQLException if database error
     */
    @Override
    @NonNull
    public List<RequestItemAuthor> getRequestItemAuthor(Context context, Item item)
        throws SQLException {
        EPerson submitter = item.getSubmitter();
        List<RequestItemAuthor> authors = new ArrayList<>(1);
        if (null != submitter) {
            RequestItemAuthor author = new RequestItemAuthor(
                submitter.getFullName(), submitter.getEmail());
            authors.add(author);
        }
        return authors;
    }
}
