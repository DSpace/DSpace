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
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Assemble a list of recipients from the results of other strategies.
 * The list of strategy classes is injected as the constructor argument
 * {@code strategies}.
 * If the strategy list is not configured, returns an empty List.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class CombiningRequestItemStrategy
        implements RequestItemAuthorExtractor {
    /** The strategies to combine. */
    private final List<RequestItemAuthorExtractor> strategies;

    /**
     * Initialize a combination of strategies.
     * @param strategies the author extraction strategies to combine.
     */
    public CombiningRequestItemStrategy(@NonNull List<RequestItemAuthorExtractor> strategies) {
        Assert.notNull(strategies, "Strategy list may not be null");
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
    @NonNull
    public List<RequestItemAuthor> getRequestItemAuthor(Context context, Item item)
            throws SQLException {
        List<RequestItemAuthor> recipients = new ArrayList<>();

        for (RequestItemAuthorExtractor strategy : strategies) {
            recipients.addAll(strategy.getRequestItemAuthor(context, item));
        }

        return recipients;
    }
}
