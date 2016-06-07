/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.datadryad.test;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.mockito.Mockito.*;

/**
 * @author Nathan Day
 */
public class ContextSolrUnitTest extends ContextUnitTest {
    protected SolrServer mockSolrStatsServer;
    public ContextSolrUnitTest() {
        super();
    }
    @Before
    public void setUp() {
        super.setUp();
        mockSolrStatsServer = mock(SolrServer.class);
    }
    @After
    public void tearDown() {
        super.tearDown();
    }

  /**
   * Given String path to solr XML response file, use it
   * as the return value for the solr server method:
   * query(SolrQuery, SolrRequest.METHOD)
   * @param path
   */
  protected void setQueryResponse(String path, String enc) throws SolrServerException, FileNotFoundException {
        QueryResponse response = makeResponse(path, enc);
        when(mockSolrStatsServer.query(
            any(SolrQuery.class), any(SolrRequest.METHOD.class)
        )).thenReturn(response);
        when(mockSolrStatsServer.query(
            any(SolrQuery.class)
        )).thenReturn(response);
    }

  /**
   * Return a solr QueryResponse object deserialized from the XML
   * response file at the provided path.
   * @param path path to XML response file
   * @param enc response file encoding; default: utf-8
   * @return
   */
    protected QueryResponse makeResponse(String path, String enc) throws FileNotFoundException {
        if (enc == null)
            enc = "utf-8";
        FileInputStream fis = new FileInputStream(new File(path));
        XMLResponseParser parser = new XMLResponseParser();
        NamedList<Object> nl = parser.processResponse(fis,enc);
        return new QueryResponse(nl, this.mockSolrStatsServer);
    }
}

