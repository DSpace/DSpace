/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.unit.services.impl.xoai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.dspace.xoai.services.api.CollectionsService;
import org.dspace.xoai.services.api.HandleResolver;
import org.dspace.xoai.services.api.solr.SolrQueryResolver;
import org.dspace.xoai.services.impl.xoai.DSpaceItemSolrRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the Solr lookup behind OAI GetRecord in {@link DSpaceItemSolrRepository#getItem(String)}.
 */
public class DSpaceItemSolrRepositoryTest {

    private final SolrClient solrClient = mock(SolrClient.class);
    private final QueryResponse response = mock(QueryResponse.class);
    private final SolrDocumentList results = new SolrDocumentList();

    private DSpaceItemSolrRepository repository;

    @Before
    public void setUp() throws Exception {
        when(solrClient.query(any(SolrParams.class))).thenReturn(response);
        when(response.getResults()).thenReturn(results);
        repository = new DSpaceItemSolrRepository(solrClient, mock(CollectionsService.class),
                mock(HandleResolver.class), mock(SolrQueryResolver.class));
    }

    @Test
    public void legitimateHandleIsQueriedLiterallyAndResolved() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField("item.handle", "123456789/1");
        doc.addField("item.deleted", false);
        results.add(doc);
        results.setNumFound(1);

        Item item = repository.getItem("oai:localhost:123456789/1");

        assertFalse(item.isDeleted());
        assertEquals("item.handle:123456789\\/1", sentQuery());
    }

    @Test
    public void querySyntaxInIdentifierIsEscaped() {
        // wildcard, range, boolean operator, grouping, quotes and backslash
        assertThrows(IdDoesNotExistException.class,
            () -> repository.getItem("oai:localhost:[* TO *] OR (x*) || \"a\\b\""));
        assertEquals("item.handle:\\[\\*\\ TO\\ \\*\\]\\ OR\\ \\(x\\*\\)\\ \\|\\|\\ \\\"a\\\\b\\\"", sentQuery());
    }

    private String sentQuery() {
        ArgumentCaptor<SolrParams> params = ArgumentCaptor.forClass(SolrParams.class);
        try {
            verify(solrClient).query(params.capture());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return params.getValue().get(CommonParams.Q);
    }
}
