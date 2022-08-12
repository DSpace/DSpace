/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class CombiningRequestItemStrategyTest {
    /**
     * Test of getRequestItemAuthor method, of class CombiningRequestItemStrategy.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetRequestItemAuthor()
            throws Exception {
        System.out.println("getRequestItemAuthor");
        Context context = null;

        Item item = Mockito.mock(Item.class);
        RequestItemAuthor author1 = new RequestItemAuthor("Pat Paulsen", "ppaulsen@example.com");
        RequestItemAuthor author2 = new RequestItemAuthor("Alfred E. Neuman", "aeneuman@example.com");
        RequestItemAuthor author3 = new RequestItemAuthor("Alias Undercover", "aundercover@example.com");

        RequestItemAuthorExtractor strategy1 = Mockito.mock(RequestItemHelpdeskStrategy.class);
        Mockito.when(strategy1.getRequestItemAuthor(context, item)).thenReturn(List.of(author1));

        RequestItemAuthorExtractor strategy2 = Mockito.mock(RequestItemMetadataStrategy.class);
        Mockito.when(strategy2.getRequestItemAuthor(context, item)).thenReturn(List.of(author2, author3));

        List<RequestItemAuthorExtractor> strategies = List.of(strategy1, strategy2);

        CombiningRequestItemStrategy instance = new CombiningRequestItemStrategy(strategies);
        List<RequestItemAuthor> result = instance.getRequestItemAuthor(context,
                item);
        assertThat(result, containsInAnyOrder(author1, author2, author3));
    }
}
