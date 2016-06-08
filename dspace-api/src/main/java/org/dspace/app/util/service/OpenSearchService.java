/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util.service;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.w3c.dom.Document;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Utility Class with static methods for producing OpenSearch-compliant search results,
 * and the OpenSearch description document.
 * <p>
 * OpenSearch is a specification for describing and advertising search-engines
 * and their result formats. Commonly, RSS and Atom formats are used, which
 * the current implementation supports, as is HTML (used directly in browsers).
 * NB: this is baseline OpenSearch, no extensions currently supported.
 * </p>
 * <p>
 * The value of the "scope" parameter should either be absent (which means no
 * scope restriction), or the handle of a community or collection.
 * </p>
 *
 * @author Richard Rodgers
 *
 */
public interface OpenSearchService {

    /**
     * Returns list of supported formats
     *
     * @return list of format names - 'rss', 'atom' or 'html'
     */
    public List<String> getFormats();

    /**
     * Returns a mime-type associated with passed format
     *
     * @param format the results document format (rss, atom, html)
     * @return content-type mime-type
     */
    public String getContentType(String format);

    /**
     * Returns the OpenSearch service document appropriate for given scope
     *
     * @param scope - null for entire repository, or handle or community or collection
     * @return document the service document
     * @throws IOException if IO error
     */
    public Document getDescriptionDoc(String scope) throws IOException;


    /**
     * Returns OpenSearch Servic Document as a string
     *
     * @param scope - null for entire repository, or handle or community or collection
     * @return service document as a string
     */
    public String getDescription(String scope);

    /**
     * Returns a formatted set of search results as a string
     *
     * @param context DSpace Context
     * @param format results format - html, rss or atom
     * @param query - the search query
     * @param totalResults - the hit count
     * @param start - start result index
     * @param pageSize - page size
     * @param scope - search scope, null or community/collection handle
     * @param results the retreived DSpace objects satisfying search
     * @param labels labels to apply - format specific
     * @return formatted search results
     * @throws IOException if IO error
     */
    public String getResultsString(Context context, String format, String query, int totalResults, int start, int pageSize,
                                          DSpaceObject scope, List<DSpaceObject> results,
                                          Map<String, String> labels) throws IOException;
    /**
     * Returns a formatted set of search results as a document
     *
     * @param context DSpace Context
     * @param format results format - html, rss or atom
     * @param query - the search query
     * @param totalResults - the hit count
     * @param start - start result index
     * @param pageSize - page size
     * @param scope - search scope, null or community/collection handle
     * @param results the retreived DSpace objects satisfying search
     * @param labels labels to apply - format specific
     * @return formatted search results
     * @throws IOException if IO error
     */
    public Document getResultsDoc(Context context, String format, String query, int totalResults, int start, int pageSize,
                                         DSpaceObject scope, List<DSpaceObject> results, Map<String, String> labels)
            throws IOException;

    public DSpaceObject resolveScope(Context context, String scope) throws SQLException;

}
