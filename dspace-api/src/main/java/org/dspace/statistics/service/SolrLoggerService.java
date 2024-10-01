/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.service;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.ObjectCount;
import org.dspace.usage.UsageWorkflowEvent;

/**
 * Static holder for a HttpSolrClient connection pool to issue
 * usage logging events to Solr from DSpace libraries, and some static query
 * composers.
 *
 * @author ben at atmire.com
 * @author kevinvandevelde at atmire.com
 * @author mdiggory at atmire.com
 */
public interface SolrLoggerService {

    /**
     * Old post method, use the new {@link #postView} method instead !
     *
     * @param dspaceObject the object used.
     * @param request      the current request context.
     * @param currentUser  the current session's user.
     * @deprecated
     */
    @Deprecated
    public void post(DSpaceObject dspaceObject, HttpServletRequest request,
                     EPerson currentUser);

    /**
     * Store a usage event into Solr.
     *
     * @param dspaceObject the object used.
     * @param request      the current request context.
     * @param currentUser  the current session's user.
     */
    public void postView(DSpaceObject dspaceObject, HttpServletRequest request,
                         EPerson currentUser);

    /**
     * Store a usage event into Solr.
     *
     * @param dspaceObject the object used.
     * @param request      the current request context.
     * @param currentUser  the current session's user.
     * @param referrer     the optional referrer.
     */
    public void postView(DSpaceObject dspaceObject, HttpServletRequest request,
                         EPerson currentUser, String referrer);

    public void postView(DSpaceObject dspaceObject,
                         String ip, String userAgent, String xforwardedfor, EPerson currentUser);

    public void postView(DSpaceObject dspaceObject,
                         String ip, String userAgent, String xforwardedfor, EPerson currentUser, String referrer);

    public void postSearch(DSpaceObject resultObject, HttpServletRequest request, EPerson currentUser,
                           List<String> queries, int rpp, String sortBy, String order, int page, DSpaceObject scope);

    public void postWorkflow(UsageWorkflowEvent usageWorkflowEvent) throws SQLException;

    /**
     * Method just used to log the parents.
     * <ul>
     * <li>Community log: owning comms.</li>
     * <li>Collection log: owning comms and their comms.</li>
     * <li>Item log: owning colls/comms.</li>
     * <li>Bitstream log: owning item/colls/comms.</li>
     * </ul>
     *
     * @param doc1 the current SolrInputDocument
     * @param dso  the current dspace object we want to log
     * @throws SQLException if database error
     *                      ignore it
     */
    public void storeParents(SolrInputDocument doc1, DSpaceObject dso)
        throws SQLException;

    public boolean isUseProxies();

    /**
     * Delete data from the index, as described by a query.
     *
     * @param query description of the records to be deleted.
     * @throws IOException         A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SolrServerException Exception from the Solr server to the solrj Java client.
     */
    public void removeIndex(String query)
        throws IOException, SolrServerException;

    public Map<String, List<String>> queryField(String query,
                                                List oldFieldVals, String field)
        throws IOException;

    public void markRobotsByIP();

    public void markRobotByUserAgent(String agent);

    public void deleteRobotsByIsBotFlag();

    public void deleteIP(String ip);

    public void deleteRobotsByIP();

    /*
     * update(String query, boolean addField, String fieldName, Object
     * fieldValue, Object oldFieldValue) throws SolrServerException, IOException
     * { List<Object> vals = new ArrayList<Object>(); vals.add(fieldValue);
     * List<Object> oldvals = new ArrayList<Object>(); oldvals.add(fieldValue);
     * update(query, addField, fieldName, vals, oldvals); }
     */
    public void update(String query, String action,
                       List<String> fieldNames, List<List<Object>> fieldValuesList)
        throws SolrServerException, IOException;

    /**
     * Update the solr core.
     * @param query
     *      query indicating which documents to update
     * @param action
     *      the update action keyword
     * @param fieldNames
     *      the fields to update
     * @param fieldValuesList
     *      the values for the fields to update
     * @param commit
     *      whether to commit the changes
     */
    public void update(String query, String action,
                       List<String> fieldNames, List<List<Object>> fieldValuesList, boolean commit)
            throws SolrServerException, IOException;

    public void query(String query, int max, int facetMinCount)
        throws SolrServerException, IOException;

    /**
     * Query used to get values grouped by the given facet field.
     *
     * @param query        the query to be used
     * @param filterQuery  filter query
     * @param facetField   the facet field on which to group our values
     * @param max          the max number of values given back (in case of 10 the top 10
     *                     will be given)
     * @param showTotal    a boolean determining whether the total amount should be given
     *                     back as the last element of the array
     * @param facetQueries list of facet queries
     * @param facetMinCount Minimum count of results facet must have to return a result
     * @return an array containing our results
     * @throws SolrServerException Exception from the Solr server to the solrj Java client.
     * @throws java.io.IOException passed through.
     */
    public ObjectCount[] queryFacetField(String query,
                                         String filterQuery, String facetField, int max, boolean showTotal,
                                         List<String> facetQueries, int facetMinCount)
        throws SolrServerException, IOException;

    /**
     * Query used to get values grouped by the date.
     *
     * @param query       the query to be used
     * @param filterQuery filter query
     * @param max         the max number of values given back (in case of 10 the top 10
     *                    will be given)
     * @param dateType    the type to be used (example: DAY, MONTH, YEAR)
     * @param dateStart   the start date Format:(-3, -2, ..) the date is calculated
     *                    relatively on today
     * @param dateEnd     the end date stop Format (-2, +1, ..) the date is calculated
     *                    relatively on today
     * @param showTotal   a boolean determining whether the total amount should be given
     *                    back as the last element of the array
     * @param context     The relevant DSpace Context.
     * @param facetMinCount Minimum count of results facet must have to return a result
     * @return and array containing our results
     * @throws SolrServerException Exception from the Solr server to the solrj Java client.
     * @throws java.io.IOException passed through.
     */
    public ObjectCount[] queryFacetDate(String query,
                                        String filterQuery, int max, String dateType, String dateStart,
                                        String dateEnd, boolean showTotal, Context context, int facetMinCount)
        throws SolrServerException, IOException;

    public Map<String, Integer> queryFacetQuery(String query, String filterQuery, List<String> facetQueries,
                                                int facetMinCount)
        throws SolrServerException, IOException;

    public ObjectCount queryTotal(String query, String filterQuery, int facetMinCount)
        throws SolrServerException, IOException;

    /**
     * Perform a solr query.
     *
     * @param query         the query to be used
     * @param filterQuery   filter query
     * @param facetField    field to facet the results by
     * @param rows          the max number of results to return
     * @param max           the max number of facets to return
     * @param dateType      the type to be used (example: DAY, MONTH, YEAR)
     * @param dateStart     the start date Format:(-3, -2, ..) the date is calculated
     *                      relatively on today
     * @param dateEnd       the end date stop Format (-2, +1, ..) the date is calculated
     *                      relatively on today
     * @param facetQueries  list of facet queries
     * @param sort          the sort field
     * @param ascending     the sort direction (true: ascending)
     * @param facetMinCount Minimum count of results facet must have to return a result
     * @throws SolrServerException Exception from the Solr server to the solrj Java client.
     * @throws java.io.IOException passed through.
     */
    public QueryResponse query(String query, String filterQuery,
                               String facetField, int rows, int max, String dateType, String dateStart,
                               String dateEnd, List<String> facetQueries, String sort, boolean ascending,
                               int facetMinCount)
        throws SolrServerException, IOException;

    /**
     * Perform a solr query.
     *
     * @param query         the query to be used
     * @param filterQuery   filter query
     * @param facetField    field to facet the results by
     * @param rows          the max number of results to return
     * @param max           the max number of facets to return
     * @param dateType      the type to be used (example: DAY, MONTH, YEAR)
     * @param dateStart     the start date Format:(-3, -2, ..) the date is calculated
     *                      relatively on today
     * @param dateEnd       the end date stop Format (-2, +1, ..) the date is calculated
     *                      relatively on today
     * @param facetQueries  list of facet queries
     * @param sort          the sort field
     * @param ascending     the sort direction (true: ascending)
     * @param facetMinCount Minimum count of results facet must have to return a result
     * @param defaultFilterQueries
     *                      use the default filter queries
     * @throws SolrServerException Exception from the Solr server to the solrj Java client.
     * @throws java.io.IOException passed through.
     */
    public QueryResponse query(String query, String filterQuery,
                               String facetField, int rows, int max, String dateType, String dateStart,
                               String dateEnd, List<String> facetQueries, String sort, boolean ascending,
                               int facetMinCount, boolean defaultFilterQueries)
            throws SolrServerException, IOException;

    /**
     * Returns in a filterQuery string all the ip addresses that should be ignored
     *
     * @return a string query with ip addresses
     */
    public String getIgnoreSpiderIPs();

    public void shardSolrIndex() throws IOException, SolrServerException;

    public void reindexBitstreamHits(boolean removeDeletedBitstreams) throws Exception;

    /**
     * Export all SOLR usage statistics for viewing/downloading content to a flat text file.
     * The file goes to a series
     *
     * @throws Exception if error
     */
    public void exportHits() throws Exception;

    /**
     * Commit the solr core.
     */
    public void commit() throws IOException, SolrServerException;

    /**
     * Anonymize a given ip
     * @param ip
     *      The ip to anonymize.
     */
    public Object anonymizeIp(String ip) throws UnknownHostException;

}
