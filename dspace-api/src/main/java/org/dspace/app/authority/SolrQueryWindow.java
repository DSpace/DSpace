/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.authority;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrap a Solr query in an Iterator, repeatedly executing the query while moving
 * a fixed-sized window across the results until all have been returned.
 *
 * @author mhwood
 */
class SolrQueryWindow
        implements Iterable<SolrDocument>, Iterator<SolrDocument> {
    private static final Logger LOG = LoggerFactory.getLogger(SolrQueryWindow.class);

    /** Fetch this many results at a time. */
    private static final int WINDOW_SIZE = 100;

    /** Where we send queries and whence we get results. */
    private final HttpSolrServer solr;

    /** Caller's Solr query parameters, augmented with {@code start} and {@code rows}. */
    private final ModifiableSolrParams params;

    /** Current offset of the window within the total result set. */
    private int windowStart;

    /** Current position within the results window. */
    private int windowPos;

    /** Content of the results window. */
    private SolrDocumentList results;

    /**
     * Holds a Solr connection, a query, and the current window of results.
     *
     * @param solr connection to a Solr core/collection.
     * @param params description of the query.  Any {@code start} and
     *          {@code rows} parameters will be ignored.
     */
    SolrQueryWindow(HttpSolrServer solr, SolrParams params)
            throws SolrServerException, IOException {
        this.solr = solr;
        this.params = new ModifiableSolrParams(params);

        // Initialize the results window.
        windowStart = -WINDOW_SIZE; // first refill() will advance it to zero.
        this.params.set("rows", WINDOW_SIZE);
        refill();
    }

    /** Position the window, perform partial query, advance the window. */
    private void refill()
            throws SolrServerException, IOException {
        windowStart += WINDOW_SIZE;
        params.set("start", windowStart);
        LOG.debug("refill:  windowStart = {}", windowStart);
        QueryResponse response = solr.query(params);
        results = response.getResults();
        windowPos = 0;
    }

    /*
     * Iterable
     */

    @Override
    public Iterator<SolrDocument> iterator() {
        return this;
    }

    /*
     * Iterator
     */

    @Override
    public boolean hasNext() {
        LOG.debug("hasNext:  results.getNumFound = {}; windowStart = {}; windowPos = {}",
                results.getNumFound(), windowStart, windowPos);
        return results.getNumFound() > windowStart + windowPos;
    }

    @Override
    public SolrDocument next() {
        if (!hasNext()) {
            throw new NoSuchElementException("All contents consumed.");
        }
        if (windowPos >= results.size()) {
            try {
                refill();
            } catch (SolrServerException | IOException ex) {
                throw new NoSuchElementException("Unable to refill window:  "
                        + ex.getMessage());
            }
        }
        return results.get(windowPos++);
    }
}
