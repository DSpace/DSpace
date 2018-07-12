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
import javax.inject.Named;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Assemble a list of recipients from the results of other strategies.
 * The list of strategy classes is injected as the property {@code strategies}.
 * If the property is not configured, returns an empty List.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Named
public class CombiningRequestItemStrategy
        implements RequestItemAuthorExtractor {
    /** The strategies to combine. */
    private final List<RequestItemAuthorExtractor> strategies;

    public CombiningRequestItemStrategy(List<RequestItemAuthorExtractor> strategies) {
        this.strategies = strategies;
    }

    /**
     * Do not call.
     * @throws IllegalArgumentException always
     */
    private CombiningRequestItemStrategy() {
        throw new IllegalArgumentException();
    }

    @Override
    public List<RequestItemAuthor> getRequestItemAuthor(Context context, Item item)
            throws SQLException {
        List<RequestItemAuthor> recipients = new ArrayList<>();

        if (null != strategies) {
            for (RequestItemAuthorExtractor strategy : strategies) {
                recipients.addAll(strategy.getRequestItemAuthor(context, item));
            }
        }

        return recipients;
    }
}
