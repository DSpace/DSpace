/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.ObjectCount;
import org.dspace.usage.UsageWorkflowEvent;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
     * Old post method, use the new postview method instead !
     *
     * @deprecated
     * @param dspaceObject the object used.
     * @param request the current request context.
     * @param currentUser the current session's user.
     */
    public void post(DSpaceObject dspaceObject, HttpServletRequest request,
            EPerson currentUser);

    /**
     * Store a usage event into Solr.
     *
     * @param dspaceObject the object used.
     * @param request the current request context.
     * @param currentUser the current session's user.
     */
    public void postView(DSpaceObject dspaceObject, HttpServletRequest request,
                                EPerson currentUser);

    public void postView(DSpaceObject dspaceObject,
   			String ip, String userAgent, String xforwardedfor, EPerson currentUser);

    public void postSearch(DSpaceObject resultObject, HttpServletRequest request, EPerson currentUser,
                                 List<String> queries, int rpp, String sortBy, String order, int page, DSpaceObject scope);

    public void postWorkflow(UsageWorkflowEvent usageWorkflowEvent) throws SQLException;

    /**
     * Method just used to log the parents.
     * <ul>
     *  <li>Community log: owning comms.</li>
     *  <li>Collection log: owning comms & their comms.</li>
     *  <li>Item log: owning colls/comms.</li>
     *  <li>Bitstream log: owning item/colls/comms.</li>
     * </ul>
     *
     * @param doc1
     *            the current SolrInputDocument
     * @param dso
     *            the current dspace object we want to log
     * @throws SQLException if database error
     *             ignore it
     */
    public void storeParents(SolrInputDocument doc1, DSpaceObject dso)
            throws SQLException;

    public boolean isUseProxies();

    /**
     * Delete data from the index, as described by a query.
     *
     * @param query description of the records to be deleted.
     * @throws java.io.IOException
     * @throws org.apache.solr.client.solrj.SolrServerException
     */
    public void removeIndex(String query) throws IOException,
            SolrServerException;

    public Map<String, List<String>> queryField(String query,
            List oldFieldVals, String field);

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

    public void update(String query, String action,
                       List<String> fieldNames, List<List<Object>> fieldValuesList, boolean commit)
        throws SolrServerException, IOException;

    public void query(String query, int max) throws SolrServerException;

    /**
     * Query used to get values grouped by the given facet field.
     *
     * @param query
     *            the query to be used
     * @param facetField
     *            the facet field on which to group our values
     * @param max
     *            the max number of values given back (in case of 10 the top 10
     *            will be given)
     * @param showTotal
     *            a boolean determining whether the total amount should be given
     *            back as the last element of the array
     * @return an array containing our results
     * @throws SolrServerException
     *             ...
     */
    public ObjectCount[] queryFacetField(String query,
            String filterQuery, String facetField, int max, boolean showTotal,
            List<String> facetQueries) throws SolrServerException;

    /**
     * Query used to get values grouped by the date.
     *
     * @param query
     *            the query to be used
     * @param max
     *            the max number of values given back (in case of 10 the top 10
     *            will be given)
     * @param dateType
     *            the type to be used (example: DAY, MONTH, YEAR)
     * @param dateStart
     *            the start date Format:(-3, -2, ..) the date is calculated
     *            relatively on today
     * @param dateEnd
     *            the end date stop Format (-2, +1, ..) the date is calculated
     *            relatively on today
     * @param showTotal
     *            a boolean determining whether the total amount should be given
     *            back as the last element of the array
     * @return and array containing our results
     * @throws SolrServerException
     *             ...
     */
    public ObjectCount[] queryFacetDate(String query,
            String filterQuery, int max, String dateType, String dateStart,
            String dateEnd, boolean showTotal, Context context) throws SolrServerException;

    public Map<String, Integer> queryFacetQuery(String query,
            String filterQuery, List<String> facetQueries)
            throws SolrServerException;

    public ObjectCount queryTotal(String query, String filterQuery)
            throws SolrServerException;

    public QueryResponse query(String query, String filterQuery,
            String facetField, int rows, int max, String dateType, String dateStart,
            String dateEnd, List<String> facetQueries, String sort, boolean ascending)
            throws SolrServerException;

    public QueryResponse query(String query, String filterQuery,
                               String facetField, int rows, int max, String dateType, String dateStart,
                               String dateEnd, List<String> facetQueries, String sort, boolean ascending,
                               boolean defaultFilterQueries)
            throws SolrServerException;

    public QueryResponse query(String query, String filterQuery,
                               String facetField, int rows, int max, String dateType, String dateStart,
                               String dateEnd, List<String> facetQueries, String sort, boolean ascending,
                               boolean defaultFilterQueries, boolean includeShardField)
        throws SolrServerException;

    /**
     * Returns in a filterQuery string all the ip addresses that should be ignored
     *
     * @return a string query with ip addresses
     */
    public String getIgnoreSpiderIPs();

    /**
     * Maintenance to keep a SOLR index efficient.
     * Note: This might take a long time.
     */
    public void optimizeSOLR();

    public void shardSolrIndex() throws IOException, SolrServerException;

    public void reindexBitstreamHits(boolean removeDeletedBitstreams) throws Exception;

    /**
     * Export all SOLR usage statistics for viewing/downloading content to a flat text file.
     * The file goes to a series
     *
     * @throws Exception if error
     */
    public void exportHits() throws Exception;

    public void commit() throws Exception;

    public void commitShard(String shard) throws Exception;

    public Object anonymizeIp(String ip) throws UnknownHostException;

}
