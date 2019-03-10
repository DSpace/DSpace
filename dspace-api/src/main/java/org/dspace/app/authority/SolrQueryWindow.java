/*
 * Copyright 2019 Mark H. Wood.
 */

package org.dspace.app.authority;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

/**
 * Wrap a Solr query in an Iterator, repeatedly executing the query while moving
 * a fixed-sized window across the results until all have been returned.
 *
 * @author mhwood
 */
class SolrQueryWindow implements Iterable<SolrDocument>, Iterator<SolrDocument> {
    /** Fetch this many results at a time. */
    private static final int WINDOW_SIZE = 100;

    /** Where we send queries and whence we get results. */
    private final HttpSolrClient solr;

    /** Caller's Solr query parameters, augmented with {@code start} and {@code rows}. */
    private final ModifiableSolrParams params;

    /** Current offset of the window within the total result set. */
    private int windowStart = 0;

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
    SolrQueryWindow(HttpSolrClient solr, SolrParams params)
            throws SolrServerException, IOException {
        this.solr = solr;
        this.params = new ModifiableSolrParams(params);

        // Initialize the results window.
        this.params.set("start", windowStart);
        this.params.set("rows", WINDOW_SIZE);
        refill();
    }

    /** Position the window, perform partial query, advance the window. */
    private void refill()
            throws SolrServerException, IOException {
        params.set("start", windowStart);
        QueryResponse response = solr.query(params);
        results = response.getResults();
        windowStart += WINDOW_SIZE;
        windowPos = 0;
    }

    @Override
    public Iterator<SolrDocument> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return results.getNumFound() < windowStart + windowPos;
    }

    @Override
    public SolrDocument next() {
        if (!hasNext()) {
            throw new NoSuchElementException("All contents consumed.");
        }
        if (windowPos > results.size()) {
            try {
                refill();
            } catch (SolrServerException | IOException ex) {
                throw new NoSuchElementException("Unable to refill window:  " + ex.getMessage());
            }
        }
        return results.get(windowPos++);
    }
}
