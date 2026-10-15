/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.unit.services.impl.xoai;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.lyncode.xoai.dataprovider.core.ListItemsResults;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.xoai.data.ResumptionCursor;
import org.dspace.xoai.services.api.solr.SolrQueryResolver;
import org.dspace.xoai.services.impl.xoai.DSpaceItemSolrRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class DSpaceItemSolrRepositoryTest {

    private static final String QUERY = "(item.collections:col_123456789_2)";
    private static final int TOTAL = 1000;
    private static final int LENGTH = 10;
    private static final int SERVED = 400;

    private final List<ScopedFilter> filters = new ArrayList<>();
    private final ResumptionCursor cursor = new ResumptionCursor();
    private final String cursorId = UUID.randomUUID().toString();
    private final List<String> respondedIds = new ArrayList<>();
    private SolrClient server;
    private DSpaceItemSolrRepository underTest;

    @Before
    public void wire() throws Exception {
        server = mock(SolrClient.class);
        SolrQueryResolver queryResolver = mock(SolrQueryResolver.class);
        when(queryResolver.buildQuery(any())).thenReturn(QUERY);
        underTest = new DSpaceItemSolrRepository(server, null, null, queryResolver, cursor);
    }

    @Test
    public void theFirstPageSkipsNothingAndReportsWhereItStopped() throws Exception {
        respondWith(TOTAL, LENGTH);

        ListItemsResults results = underTest.getItems(filters, 0, LENGTH);

        assertThat(capturedQuery().getQuery(), is(QUERY));
        assertThat(capturedQuery().getStart(), is(0));
        assertThat(results.getTotal(), is(TOTAL));
        assertThat(results.hasMore(), is(true));
        assertThat("the page just served is where the next one resumes from",
                   cursor.valueOf(), is(lastRespondedId()));
    }

    /**
     * The upper bound of the range has to stay open. Built from the term "*" instead of a null term,
     * TermRangeQuery escapes it to "\*", Solr reads the literal string and answers 400 "Invalid UUID String:
     * '*'" -- which getItems() turns into an empty page and a harvester reads as the end of the set. Hence an
     * assertion on the exact syntax handed over, not merely on the presence of a range.
     */
    @Test
    public void aPageReachedWithACursorIsBoundedByAnOpenEndedRange() throws Exception {
        cursor.moveTo(cursorId);
        respondWith(TOTAL - SERVED, LENGTH);

        underTest.getItems(filters, 0, LENGTH);

        assertThat(capturedQuery().getQuery(), is(QUERY + " AND item.id:{" + cursorId + " TO *]"));
        assertThat("nothing left to skip once the query starts where the cursor stands",
                   capturedQuery().getStart(), is(0));
    }

    /**
     * With the cursor bound in place, numFound is what was left to serve, this page included: one record
     * beyond a full page is enough to announce another one. How many records stand before the cursor is not
     * carried anymore, so no complete list size can be reported -- the attribute is optional and omitted.
     */
    @Test
    public void onePastAFullPageAnnouncesAnotherOneAndNoCompleteListSize() throws Exception {
        cursor.moveTo(cursorId);
        respondWith(LENGTH + 1, LENGTH);

        ListItemsResults results = underTest.getItems(filters, 0, LENGTH);

        assertThat("the record past this page still has to be announced",
                   results.hasMore(), is(true));
        assertThat(results.hasTotalResults(), is(false));
    }

    /**
     * Also the case of a set whose size is an exact multiple of the page size: the last full page already
     * reports no more, so no empty final page -- which the xoai library would turn into a noRecordsMatch
     * error mid-harvest -- is ever announced.
     */
    @Test
    public void aPageServingExactlyWhatWasLeftReportsNoMore() throws Exception {
        cursor.moveTo(cursorId);
        respondWith(LENGTH, LENGTH);

        ListItemsResults results = underTest.getItems(filters, 0, LENGTH);

        assertThat(results.hasMore(), is(false));
    }

    @Test
    public void aPageWithoutACursorFallsBackToSkippingTheOffset() throws Exception {
        // What a token whose cursor field is empty resolves to.
        respondWith(TOTAL, LENGTH);

        ListItemsResults results = underTest.getItems(filters, SERVED, LENGTH);

        assertThat(capturedQuery().getQuery(), is(not(containsString("item.id:"))));
        assertThat("the offset has to be skipped the old way", capturedQuery().getStart(), is(SERVED));
        assertThat("an unbounded count is already the total", results.getTotal(), is(TOTAL));
    }

    private void respondWith(long numFound, int returned) throws Exception {
        SolrDocumentList documents = new SolrDocumentList();
        documents.setNumFound(numFound);
        for (int i = 0; i < returned; i++) {
            String itemId = UUID.randomUUID().toString();
            respondedIds.add(itemId);
            SolrDocument document = new SolrDocument();
            document.addField("item.id", itemId);
            document.addField("item.handle", "123456789/" + i);
            document.addField("item.lastmodified", new Date());
            document.addField("item.deleted", Boolean.FALSE);
            documents.add(document);
        }
        QueryResponse response = mock(QueryResponse.class);
        when(response.getResults()).thenReturn(documents);
        when(server.query(any(SolrQuery.class))).thenReturn(response);
    }

    private String lastRespondedId() {
        return respondedIds.get(respondedIds.size() - 1);
    }

    private SolrQuery capturedQuery() throws Exception {
        ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
        verify(server).query(captor.capture());
        return captor.getValue();
    }
}
