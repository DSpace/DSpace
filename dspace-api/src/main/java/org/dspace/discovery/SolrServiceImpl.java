/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.apache.solr.common.params.SpellingParams;
import org.apache.solr.common.util.NamedList;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryMoreLikeThisConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SolrIndexer contains the methods that index Items and their metadata,
 * collections, communities, etc. It is meant to either be invoked from the
 * command line (see dspace/bin/index-all) or via the indexContent() methods
 * within DSpace.
 * <p>
 * The Administrator can choose to run SolrIndexer in a cron that repeats
 * regularly, a failed attempt to index from the UI will be "caught" up on in
 * that cron.
 *
 * The SolrServiceImpl is registered as a Service in the ServiceManager via
 * a Spring configuration file located under
 * classpath://spring/spring-dspace-applicationContext.xml
 *
 * Its configuration is Autowired by the ApplicationContext
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
@Service
public class SolrServiceImpl implements SearchService, IndexingService {

    /**
     * The name of the discover configuration used to search for workflow tasks in the mydspace
     */
    public static final String DISCOVER_WORKFLOW_CONFIGURATION_NAME = "workflow";

    /**
     * The name of the discover configuration used to search for inprogress submission in the mydspace
     */
    public static final String DISCOVER_WORKSPACE_CONFIGURATION_NAME = "workspace";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SolrServiceImpl.class);

    @Autowired
    protected ContentServiceFactory contentServiceFactory;
    @Autowired
    protected GroupService groupService;
    @Autowired
    protected IndexObjectFactoryFactory indexObjectServiceFactory;
    @Autowired
    protected SolrSearchCore solrSearchCore;

    protected SolrServiceImpl() {

    }



    /**
     * If the handle for the "dso" already exists in the index, and the "dso"
     * has a lastModified timestamp that is newer than the document in the index
     * then it is updated, otherwise a new document is added.
     *
     * @param context Users Context
     * @param dso     DSpace Object (Item, Collection or Community
     * @throws SQLException if error
     */
    @Override
    public void indexContent(Context context, IndexableObject dso)
        throws SQLException {
        indexContent(context, dso, false);
    }

    /**
     * If the handle for the "dso" already exists in the index, and the "dso"
     * has a lastModified timestamp that is newer than the document in the index
     * then it is updated, otherwise a new document is added.
     *
     * @param context Users Context
     * @param indexableObject     The object we want to index
     * @param force   Force update even if not stale.
     * @throws SQLException if error
     */
    @Override
    public void indexContent(Context context, IndexableObject indexableObject,
                             boolean force) {

        try {
            final IndexFactory indexableObjectFactory = indexObjectServiceFactory.
                    getIndexableObjectFactory(indexableObject);
            if (force || requiresIndexing(indexableObject.getUniqueIndexID(), indexableObject.getLastModified())) {
                update(context, indexableObjectFactory, indexableObject);
                log.info(LogManager.getHeader(context, "indexed_object", indexableObject.getUniqueIndexID()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected void update(Context context, IndexFactory indexableObjectService,
                          IndexableObject indexableObject) throws IOException, SQLException, SolrServerException {
        final SolrInputDocument solrInputDocument = indexableObjectService.buildDocument(context, indexableObject);
        indexableObjectService.writeDocument(context, indexableObject, solrInputDocument);
    }

    /**
     * unIndex removes an Item, Collection, or Community
     *
     * @param context The relevant DSpace Context.
     * @param dso     DSpace Object, can be Community, Item, or Collection
     * @throws SQLException if database error
     * @throws IOException  if IO error
     */
    @Override
    public void unIndexContent(Context context, IndexableObject dso)
        throws SQLException, IOException {
        unIndexContent(context, dso, false);
    }

    /**
     * unIndex removes an Item, Collection, or Community
     *
     * @param context The relevant DSpace Context.
     * @param indexableObject The object to be indexed
     * @param commit  if <code>true</code> force an immediate commit on SOLR
     * @throws SQLException if database error
     * @throws IOException  if IO error
     */
    @Override
    public void unIndexContent(Context context, IndexableObject indexableObject, boolean commit)
        throws SQLException, IOException {
        try {
            if (indexableObject == null) {
                return;
            }
            String uniqueID = indexableObject.getUniqueIndexID();
            log.info("Try to delete uniqueID:" + uniqueID);
            indexObjectServiceFactory.getIndexableObjectFactory(indexableObject).delete(indexableObject);
            if (commit) {
                solrSearchCore.getSolr().commit();
            }
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            emailException(exception);
        }
    }

    /**
     * Unindex a Document in the Lucene index.
     *
     * @param context the dspace context
     * @param searchUniqueID the search uniqueID of the document to be deleted
     * @throws IOException  if IO error
     */
    @Override
    public void unIndexContent(Context context, String searchUniqueID) throws IOException {
        unIndexContent(context, searchUniqueID, false);
    }

    /**
     * Unindex a Document in the Lucene Index.
     *
     * @param context the dspace context
     * @param searchUniqueID the search uniqueID of the document to be deleted
     * @throws IOException  if IO error
     */
    @Override
    public void unIndexContent(Context context, String searchUniqueID, boolean commit)
        throws IOException {

        try {
            if (solrSearchCore.getSolr() != null) {
                indexObjectServiceFactory.getIndexableObjectFactory(searchUniqueID).delete(searchUniqueID);
                if (commit) {
                    solrSearchCore.getSolr().commit();
                }
            }
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * reIndexContent removes something from the index, then re-indexes it
     *
     * @param context context object
     * @param dso     object to re-index
     */
    @Override
    public void reIndexContent(Context context, IndexableObject dso)
        throws SQLException, IOException {
        try {
            indexContent(context, dso);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            emailException(exception);
        }
    }

    /**
     * create full index - wiping old index
     *
     * @param c context to use
     */
    @Override
    public void createIndex(Context c) throws SQLException, IOException {

        /* Reindex all content preemptively. */
        updateIndex(c, true);

    }


    /**
     * Iterates over all Items, Collections and Communities. And updates them in
     * the index. Uses decaching to control memory footprint. Uses indexContent
     * and isStale to check state of item in index.
     *
     * @param context the dspace context
     */
    @Override
    public void updateIndex(Context context) {
        updateIndex(context, false);
    }

    /**
     * Iterates over all Items, Collections and Communities. And updates them in
     * the index. Uses decaching to control memory footprint. Uses indexContent
     * and isStale to check state of item in index.
     * <p>
     * At first it may appear counterintuitive to have an IndexWriter/Reader
     * opened and closed on each DSO. But this allows the UI processes to step
     * in and attain a lock and write to the index even if other processes/jvms
     * are running a reindex.
     *
     * @param context the dspace context
     * @param force   whether or not to force the reindexing
     */
    @Override
    public void updateIndex(Context context, boolean force) {
        updateIndex(context, force, null);
    }

    @Override
    public void updateIndex(Context context, boolean force, String type) {
        try {
            final List<IndexFactory> indexableObjectServices = indexObjectServiceFactory.
                getIndexFactories();
            for (IndexFactory indexableObjectService : indexableObjectServices) {
                if (type == null || StringUtils.equals(indexableObjectService.getType(), type)) {
                    final Iterator<IndexableObject> indexableObjects = indexableObjectService.findAll(context);
                    while (indexableObjects.hasNext()) {
                        final IndexableObject indexableObject = indexableObjects.next();
                        indexContent(context, indexableObject, force);
                        context.uncacheEntity(indexableObject.getIndexedObject());
                    }
                }
            }
            if (solrSearchCore.getSolr() != null) {
                solrSearchCore.getSolr().commit();
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Iterates over all documents in the Lucene index and verifies they are in
     * database, if not, they are removed.
     *
     * @param force whether or not to force a clean index
     * @throws IOException            IO exception
     * @throws SQLException           sql exception
     * @throws SearchServiceException occurs when something went wrong with querying the solr server
     */
    @Override
    public void cleanIndex(boolean force) throws IOException, SQLException, SearchServiceException {
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try {
            if (solrSearchCore.getSolr() == null) {
                return;
            }
            if (force) {
                final List<IndexFactory> indexableObjectServices = indexObjectServiceFactory.
                        getIndexFactories();
                for (IndexFactory indexableObjectService : indexableObjectServices) {
                    indexableObjectService.deleteAll();
                }
            } else {
                SolrQuery query = new SolrQuery();
                // Query for all indexed Items, Collections and Communities,
                // returning just their handle
                query.setFields(SearchUtils.RESOURCE_UNIQUE_ID);
                query.addSort(SearchUtils.RESOURCE_UNIQUE_ID, SolrQuery.ORDER.asc);
                query.setQuery("*:*");

                // Get the total amount of results
                QueryResponse totalResponse = solrSearchCore.getSolr().query(query, SolrRequest.METHOD.POST);
                long total = totalResponse.getResults().getNumFound();

                int start = 0;
                int batch = 100;

                query.setRows(batch);
                while (start < total) {
                    query.setStart(start);
                    QueryResponse rsp = solrSearchCore.getSolr().query(query, SolrRequest.METHOD.POST);
                    SolrDocumentList docs = rsp.getResults();

                    for (SolrDocument doc : docs) {
                        String uniqueID = (String) doc.getFieldValue(SearchUtils.RESOURCE_UNIQUE_ID);

                        IndexableObject o = findIndexableObject(context, doc);

                        if (o == null) {
                            log.info("Deleting: " + uniqueID);
                            /*
                             * Use IndexWriter to delete, its easier to manage
                             * write.lock
                             */
                            unIndexContent(context, uniqueID);
                        } else {
                            log.debug("Keeping: " + o.getUniqueIndexID());
                        }
                    }

                    start += batch;
                }
            }
        } catch (Exception e) {
            log.error("Error cleaning discovery index: " + e.getMessage(), e);
        } finally {
            context.abort();
        }
    }

    /**
     * Maintenance to keep a SOLR index efficient.
     * Note: This might take a long time.
     */
    @Override
    public void optimize() {
        try {
            if (solrSearchCore.getSolr() == null) {
                return;
            }
            long start = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Started:" + start);
            solrSearchCore.getSolr().optimize();
            long finish = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Finished:" + finish);
            System.out.println("SOLR Search Optimize -- Total time taken:" + (finish - start) + " (ms).");
        } catch (SolrServerException sse) {
            System.err.println(sse.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    @Override
    public void buildSpellCheck()
            throws SearchServiceException, IOException {
        try {
            if (solrSearchCore.getSolr() == null) {
                return;
            }
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.set("spellcheck", true);
            solrQuery.set(SpellingParams.SPELLCHECK_BUILD, true);
            solrSearchCore.getSolr().query(solrQuery, SolrRequest.METHOD.POST);
        } catch (SolrServerException e) {
            //Make sure to also log the exception since this command is usually run from a crontab.
            log.error(e, e);
            throw new SearchServiceException(e);
        }
    }

    // //////////////////////////////////
    // Private
    // //////////////////////////////////

    protected void emailException(Exception exception) {
        // Also email an alert, system admin may need to check for stale lock
        try {
            String recipient = ConfigurationManager
                .getProperty("alert.recipient");

            if (StringUtils.isNotBlank(recipient)) {
                Email email = Email
                    .getEmail(I18nUtil.getEmailFilename(
                        Locale.getDefault(), "internal_error"));
                email.addRecipient(recipient);
                email.addArgument(ConfigurationManager
                                      .getProperty("dspace.ui.url"));
                email.addArgument(new Date());

                String stackTrace;

                if (exception != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                } else {
                    stackTrace = "No exception";
                }

                email.addArgument(stackTrace);
                email.send();
            }
        } catch (Exception e) {
            // Not much we can do here!
            log.warn("Unable to send email alert", e);
        }

    }


    /**
     * Is stale checks the lastModified time stamp in the database and the index
     * to determine if the index is stale.
     *
     * @param uniqueId       the unique identifier of the object that we want to index
     * @param lastModified the last modified date of the DSpace object
     * @return a boolean indicating if the dso should be re indexed again
     * @throws SQLException           sql exception
     * @throws IOException            io exception
     * @throws SearchServiceException if something went wrong with querying the solr server
     */
    protected boolean requiresIndexing(String uniqueId, Date lastModified)
        throws SQLException, IOException, SearchServiceException {

        // Check if we even have a last modified date
        if (lastModified == null) {
            return true;
        }

        boolean reindexItem = false;
        boolean inIndex = false;

        SolrQuery query = new SolrQuery();
        query.setQuery(SearchUtils.RESOURCE_UNIQUE_ID + ":" + uniqueId);
        // Specify that we ONLY want the LAST_INDEXED_FIELD returned in the field list (fl)
        query.setFields(SearchUtils.LAST_INDEXED_FIELD);
        QueryResponse rsp;

        try {
            if (solrSearchCore.getSolr() == null) {
                return false;
            }
            rsp = solrSearchCore.getSolr().query(query, SolrRequest.METHOD.POST);
        } catch (SolrServerException e) {
            throw new SearchServiceException(e.getMessage(), e);
        }

        for (SolrDocument doc : rsp.getResults()) {

            inIndex = true;

            Object value = doc.getFieldValue(SearchUtils.LAST_INDEXED_FIELD);

            if (value instanceof Date) {
                Date lastIndexed = (Date) value;

                if (lastIndexed.before(lastModified)) {

                    reindexItem = true;
                }
            }
        }

        return reindexItem || !inIndex;
    }

    @Override
    public String createLocationQueryForAdministrableItems(Context context)
        throws SQLException {
        StringBuilder locationQuery = new StringBuilder();

        if (context.getCurrentUser() != null) {
            List<Group> groupList = EPersonServiceFactory.getInstance().getGroupService()
                                                         .allMemberGroups(context, context.getCurrentUser());

            List<ResourcePolicy> communitiesPolicies = AuthorizeServiceFactory.getInstance().getResourcePolicyService()
                                                                              .find(context, context.getCurrentUser(),
                                                                                    groupList, Constants.ADMIN,
                                                                                    Constants.COMMUNITY);

            List<ResourcePolicy> collectionsPolicies = AuthorizeServiceFactory.getInstance().getResourcePolicyService()
                                                                              .find(context, context.getCurrentUser(),
                                                                                    groupList, Constants.ADMIN,
                                                                                    Constants.COLLECTION);

            List<Collection> allCollections = new ArrayList<>();

            for (ResourcePolicy rp : collectionsPolicies) {
                Collection collection = ContentServiceFactory.getInstance().getCollectionService()
                                                             .find(context, rp.getdSpaceObject().getID());
                allCollections.add(collection);
            }

            if (CollectionUtils.isNotEmpty(communitiesPolicies) || CollectionUtils.isNotEmpty(allCollections)) {
                locationQuery.append("location:( ");

                for (int i = 0; i < communitiesPolicies.size(); i++) {
                    ResourcePolicy rp = communitiesPolicies.get(i);
                    Community community = ContentServiceFactory.getInstance().getCommunityService()
                                                               .find(context, rp.getdSpaceObject().getID());

                    locationQuery.append("m").append(community.getID());

                    if (i != (communitiesPolicies.size() - 1)) {
                        locationQuery.append(" OR ");
                    }
                    allCollections.addAll(ContentServiceFactory.getInstance().getCommunityService()
                                                               .getAllCollections(context, community));
                }

                Iterator<Collection> collIter = allCollections.iterator();

                if (communitiesPolicies.size() > 0 && allCollections.size() > 0) {
                    locationQuery.append(" OR ");
                }

                while (collIter.hasNext()) {
                    locationQuery.append("l").append(collIter.next().getID());

                    if (collIter.hasNext()) {
                        locationQuery.append(" OR ");
                    }
                }
                locationQuery.append(")");
            } else {
                log.warn("We have a collection or community admin with ID: " + context.getCurrentUser().getID()
                             + " without any administrable collection or community!");
            }
        }
        return locationQuery.toString();
    }

    /**
     * Helper function to retrieve a date using a best guess of the potential
     * date encodings on a field
     *
     * @param t the string to be transformed to a date
     * @return a date if the formatting was successful, null if not able to transform to a date
     */
    public Date toDate(String t) {
        SimpleDateFormat[] dfArr;

        // Choose the likely date formats based on string length
        switch (t.length()) {
            // case from 1 to 3 go through adding anyone a single 0. Case 4 define
            // for all the SimpleDateFormat
            case 1:
                t = "0" + t;
                // fall through
            case 2:
                t = "0" + t;
                // fall through
            case 3:
                t = "0" + t;
                // fall through
            case 4:
                dfArr = new SimpleDateFormat[] {new SimpleDateFormat("yyyy")};
                break;
            case 6:
                dfArr = new SimpleDateFormat[] {new SimpleDateFormat("yyyyMM")};
                break;
            case 7:
                dfArr = new SimpleDateFormat[] {new SimpleDateFormat("yyyy-MM")};
                break;
            case 8:
                dfArr = new SimpleDateFormat[] {new SimpleDateFormat("yyyyMMdd"),
                    new SimpleDateFormat("yyyy MMM")};
                break;
            case 10:
                dfArr = new SimpleDateFormat[] {new SimpleDateFormat("yyyy-MM-dd")};
                break;
            case 11:
                dfArr = new SimpleDateFormat[] {new SimpleDateFormat("yyyy MMM dd")};
                break;
            case 20:
                dfArr = new SimpleDateFormat[] {new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'")};
                break;
            default:
                dfArr = new SimpleDateFormat[] {new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")};
                break;
        }

        for (SimpleDateFormat df : dfArr) {
            try {
                // Parse the date
                df.setCalendar(Calendar
                                   .getInstance(TimeZone.getTimeZone("UTC")));
                df.setLenient(false);
                return df.parse(t);
            } catch (ParseException pe) {
                log.error("Unable to parse date format", pe);
            }
        }

        return null;
    }

    public String locationToName(Context context, String field, String value) throws SQLException {
        if ("location.comm".equals(field) || "location.coll".equals(field)) {
            int type = ("location.comm").equals(field) ? Constants.COMMUNITY : Constants.COLLECTION;
            DSpaceObject commColl = null;
            if (StringUtils.isNotBlank(value)) {
                commColl = contentServiceFactory.getDSpaceObjectService(type).find(context, UUID.fromString(value));
            }
            if (commColl != null) {
                return commColl.getName();
            }

        }
        return value;
    }

    //========== SearchService implementation

    @Override
    public DiscoverResult search(Context context, IndexableObject dso, DiscoverQuery discoveryQuery)
        throws SearchServiceException {
        if (dso != null) {
            if (dso instanceof IndexableCommunity) {
                discoveryQuery.addFilterQueries("location:m" + dso.getID());
            } else if (dso instanceof IndexableCollection) {
                discoveryQuery.addFilterQueries("location:l" + dso.getID());
            } else if (dso instanceof IndexableItem) {
                discoveryQuery.addFilterQueries(SearchUtils.RESOURCE_UNIQUE_ID + ":" + dso.
                        getUniqueIndexID());
            }
        }
        return search(context, discoveryQuery);

    }


    @Override
    public DiscoverResult search(Context context, DiscoverQuery discoveryQuery )
        throws SearchServiceException {
        try {
            if (solrSearchCore.getSolr() == null) {
                return new DiscoverResult();
            }
            SolrQuery solrQuery = resolveToSolrQuery(context, discoveryQuery);


            QueryResponse queryResponse = solrSearchCore.getSolr().query(solrQuery, SolrRequest.METHOD.POST);
            return retrieveResult(context, discoveryQuery, queryResponse);

        } catch (Exception e) {
            throw new org.dspace.discovery.SearchServiceException(e.getMessage(), e);
        }
    }

    protected SolrQuery resolveToSolrQuery(Context context, DiscoverQuery discoveryQuery)
        throws SearchServiceException {
        SolrQuery solrQuery = new SolrQuery();

        String query = "*:*";
        if (discoveryQuery.getQuery() != null) {
            query = discoveryQuery.getQuery();
        }

        solrQuery.setQuery(query);

        // Add any search fields to our query. This is the limited list
        // of fields that will be returned in the solr result
        for (String fieldName : discoveryQuery.getSearchFields()) {
            solrQuery.addField(fieldName);
        }
        // Also ensure a few key obj identifier fields are returned with every query
        solrQuery.addField(SearchUtils.RESOURCE_TYPE_FIELD);
        solrQuery.addField(SearchUtils.RESOURCE_ID_FIELD);

        if (discoveryQuery.isSpellCheck()) {
            solrQuery.setParam(SpellingParams.SPELLCHECK_Q, query);
            solrQuery.setParam(SpellingParams.SPELLCHECK_COLLATE, Boolean.TRUE);
            solrQuery.setParam("spellcheck", Boolean.TRUE);
        }

        for (int i = 0; i < discoveryQuery.getFilterQueries().size(); i++) {
            String filterQuery = discoveryQuery.getFilterQueries().get(i);
            solrQuery.addFilterQuery(filterQuery);
        }
        if (discoveryQuery.getDSpaceObjectFilter() != null) {
            solrQuery.addFilterQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":" + discoveryQuery.getDSpaceObjectFilter());
        }

        for (int i = 0; i < discoveryQuery.getFieldPresentQueries().size(); i++) {
            String filterQuery = discoveryQuery.getFieldPresentQueries().get(i);
            solrQuery.addFilterQuery(filterQuery + ":[* TO *]");
        }

        if (discoveryQuery.getStart() != -1) {
            solrQuery.setStart(discoveryQuery.getStart());
        }

        if (discoveryQuery.getMaxResults() != -1) {
            solrQuery.setRows(discoveryQuery.getMaxResults());
        }

        if (discoveryQuery.getSortField() != null) {
            SolrQuery.ORDER order = SolrQuery.ORDER.asc;
            if (discoveryQuery.getSortOrder().equals(DiscoverQuery.SORT_ORDER.desc)) {
                order = SolrQuery.ORDER.desc;
            }

            solrQuery.addSort(discoveryQuery.getSortField(), order);
        }

        for (String property : discoveryQuery.getProperties().keySet()) {
            List<String> values = discoveryQuery.getProperties().get(property);
            solrQuery.add(property, values.toArray(new String[values.size()]));
        }

        List<DiscoverFacetField> facetFields = discoveryQuery.getFacetFields();
        if (0 < facetFields.size()) {
            //Only add facet information if there are any facets
            for (DiscoverFacetField facetFieldConfig : facetFields) {
                String field = transformFacetField(facetFieldConfig, facetFieldConfig.getField(), false);
                solrQuery.addFacetField(field);

                // Setting the facet limit in this fashion ensures that each facet can have its own max
                solrQuery
                    .add("f." + field + "." + FacetParams.FACET_LIMIT, String.valueOf(facetFieldConfig.getLimit()));
                String facetSort;
                if (DiscoveryConfigurationParameters.SORT.COUNT.equals(facetFieldConfig.getSortOrder())) {
                    facetSort = FacetParams.FACET_SORT_COUNT;
                } else {
                    facetSort = FacetParams.FACET_SORT_INDEX;
                }
                solrQuery.add("f." + field + "." + FacetParams.FACET_SORT, facetSort);
                if (facetFieldConfig.getOffset() != -1) {
                    solrQuery.setParam("f." + field + "."
                                           + FacetParams.FACET_OFFSET,
                                       String.valueOf(facetFieldConfig.getOffset()));
                }
                if (facetFieldConfig.getPrefix() != null) {
                    solrQuery.setFacetPrefix(field, facetFieldConfig.getPrefix());
                }
            }
        }

        List<String> facetQueries = discoveryQuery.getFacetQueries();
        for (String facetQuery : facetQueries) {
            solrQuery.addFacetQuery(facetQuery);
        }

        if (discoveryQuery.getFacetMinCount() != -1) {
            solrQuery.setFacetMinCount(discoveryQuery.getFacetMinCount());
        }

        if (CollectionUtils.isNotEmpty(facetFields) || CollectionUtils.isNotEmpty(facetQueries)) {
            solrQuery.setParam(FacetParams.FACET_OFFSET, String.valueOf(discoveryQuery.getFacetOffset()));
        }

        if (0 < discoveryQuery.getHitHighlightingFields().size()) {
            solrQuery.setHighlight(true);
            solrQuery.add(HighlightParams.USE_PHRASE_HIGHLIGHTER, Boolean.TRUE.toString());
            for (DiscoverHitHighlightingField highlightingField : discoveryQuery.getHitHighlightingFields()) {
                solrQuery.addHighlightField(highlightingField.getField() + "_hl");
                solrQuery.add("f." + highlightingField.getField() + "_hl." + HighlightParams.FRAGSIZE,
                              String.valueOf(highlightingField.getMaxChars()));
                solrQuery.add("f." + highlightingField.getField() + "_hl." + HighlightParams.SNIPPETS,
                              String.valueOf(highlightingField.getMaxSnippets()));
            }

        }

        boolean isWorkspace = StringUtils.startsWith(discoveryQuery.getDiscoveryConfigurationName(),
                DISCOVER_WORKSPACE_CONFIGURATION_NAME);
        boolean isWorkflow = StringUtils.startsWith(discoveryQuery.getDiscoveryConfigurationName(),
                DISCOVER_WORKFLOW_CONFIGURATION_NAME);
        EPerson currentUser = context.getCurrentUser();

        // extra security check to avoid the possibility that an anonymous user
        // get access to workspace or workflow
        if (currentUser == null && (isWorkflow || isWorkspace)) {
            throw new IllegalStateException("An anonymous user cannot perform a workspace or workflow search");
        }
        if (isWorkspace) {
            // insert filter by submitter
            solrQuery
                .addFilterQuery("submitter_authority:(" + currentUser.getID() + ")");
        } else if (isWorkflow) {
            // Retrieve all the groups the current user is a member of !
            Set<Group> groups;
            try {
                groups = groupService.allMemberGroupsSet(context, currentUser);
            } catch (SQLException e) {
                throw new org.dspace.discovery.SearchServiceException(e.getMessage(), e);
            }

            // insert filter by controllers
            StringBuilder controllerQuery = new StringBuilder();
            controllerQuery.append("taskfor:(e" + currentUser.getID());
            for (Group group : groups) {
                controllerQuery.append(" OR g").append(group.getID());
            }
            controllerQuery.append(")");
            solrQuery.addFilterQuery(controllerQuery.toString());
        }


        //Add any configured search plugins !
        List<SolrServiceSearchPlugin> solrServiceSearchPlugins = DSpaceServicesFactory.getInstance().getServiceManager()
                                                                                      .getServicesByType(
                                                                                          SolrServiceSearchPlugin
                                                                                              .class);
        for (SolrServiceSearchPlugin searchPlugin : solrServiceSearchPlugins) {
            searchPlugin.additionalSearchParameters(context, discoveryQuery, solrQuery);
        }

        return solrQuery;
    }

    protected DiscoverResult retrieveResult(Context context, DiscoverQuery query, QueryResponse solrQueryResponse)
        throws SQLException {
        DiscoverResult result = new DiscoverResult();

        if (solrQueryResponse != null) {
            result.setSearchTime(solrQueryResponse.getQTime());
            result.setStart(query.getStart());
            result.setMaxResults(query.getMaxResults());
            result.setTotalSearchResults(solrQueryResponse.getResults().getNumFound());

            List<String> searchFields = query.getSearchFields();
            for (SolrDocument doc : solrQueryResponse.getResults()) {
                IndexableObject indexableObject = findIndexableObject(context, doc);

                if (indexableObject != null) {
                    result.addIndexableObject(indexableObject);
                } else {
                    log.error(LogManager.getHeader(context,
                            "Error while retrieving DSpace object from discovery index",
                           "Unique identifier: " + doc.getFirstValue(SearchUtils.RESOURCE_UNIQUE_ID)));
                    continue;
                }

                DiscoverResult.SearchDocument resultDoc = new DiscoverResult.SearchDocument();
                //Add information about our search fields
                for (String field : searchFields) {
                    List<String> valuesAsString = new ArrayList<String>();
                    for (Object o : doc.getFieldValues(field)) {
                        valuesAsString.add(String.valueOf(o));
                    }
                    resultDoc.addSearchField(field, valuesAsString.toArray(new String[valuesAsString.size()]));
                }
                result.addSearchDocument(indexableObject, resultDoc);

                if (solrQueryResponse.getHighlighting() != null) {
                    Map<String, List<String>> highlightedFields = solrQueryResponse.getHighlighting().get(
                        indexableObject.getUniqueIndexID());
                    if (MapUtils.isNotEmpty(highlightedFields)) {
                        //We need to remove all the "_hl" appendix strings from our keys
                        Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
                        for (String key : highlightedFields.keySet()) {
                            List<String> highlightOriginalValue = highlightedFields.get(key);
                            List<String[]> resultHighlightOriginalValue = new ArrayList<String[]>();
                            for (String highlightValue : highlightOriginalValue) {
                                String[] splitted = highlightValue.split("###");
                                resultHighlightOriginalValue.add(splitted);

                            }
                            resultMap.put(key.substring(0, key.lastIndexOf("_hl")), highlightedFields.get(key));
                        }

                        result.addHighlightedResult(indexableObject,
                            new DiscoverResult.IndexableObjectHighlightResult(indexableObject, resultMap));
                    }
                }
            }

            //Resolve our facet field values
            List<FacetField> facetFields = solrQueryResponse.getFacetFields();
            if (facetFields != null) {
                for (int i = 0; i < facetFields.size(); i++) {
                    FacetField facetField = facetFields.get(i);
                    DiscoverFacetField facetFieldConfig = query.getFacetFields().get(i);
                    List<FacetField.Count> facetValues = facetField.getValues();
                    if (facetValues != null) {
                        if (facetFieldConfig.getType()
                                            .equals(DiscoveryConfigurationParameters.TYPE_DATE) && facetFieldConfig
                            .getSortOrder().equals(DiscoveryConfigurationParameters.SORT.VALUE)) {
                            //If we have a date & are sorting by value, ensure that the results are flipped for a
                            // proper result
                            Collections.reverse(facetValues);
                        }

                        for (FacetField.Count facetValue : facetValues) {
                            String displayedValue = transformDisplayedValue(context, facetField.getName(),
                                                                            facetValue.getName());
                            String field = transformFacetField(facetFieldConfig, facetField.getName(), true);
                            String authorityValue = transformAuthorityValue(context, facetField.getName(),
                                                                            facetValue.getName());
                            String sortValue = transformSortValue(context, facetField.getName(), facetValue.getName());
                            String filterValue = displayedValue;
                            if (StringUtils.isNotBlank(authorityValue)) {
                                filterValue = authorityValue;
                            }
                            result.addFacetResult(
                                field,
                                new DiscoverResult.FacetResult(filterValue,
                                                               displayedValue, authorityValue,
                                                               sortValue, facetValue.getCount(),
                                                               facetFieldConfig.getType()));
                        }
                    }
                }
            }

            if (solrQueryResponse.getFacetQuery() != null) {
                // just retrieve the facets in the order they where requested!
                // also for the date we ask it in proper (reverse) order
                // At the moment facet queries are only used for dates
                LinkedHashMap<String, Integer> sortedFacetQueries = new LinkedHashMap<String, Integer>(
                    solrQueryResponse.getFacetQuery());
                for (String facetQuery : sortedFacetQueries.keySet()) {
                    //TODO: do not assume this, people may want to use it for other ends, use a regex to make sure
                    //We have a facet query, the values looks something like: dateissued.year:[1990 TO 2000] AND -2000
                    //Prepare the string from {facet.field.name}:[startyear TO endyear] to startyear - endyear
                    String facetField = facetQuery.substring(0, facetQuery.indexOf(":"));
                    String name = "";
                    String filter = "";
                    if (facetQuery.indexOf('[') > -1 && facetQuery.lastIndexOf(']') > -1) {
                        name = facetQuery.substring(facetQuery.indexOf('[') + 1);
                        name = name.substring(0, name.lastIndexOf(']')).replaceAll("TO", "-");
                        filter = facetQuery.substring(facetQuery.indexOf('['));
                        filter = filter.substring(0, filter.lastIndexOf(']') + 1);
                    }

                    Integer count = sortedFacetQueries.get(facetQuery);

                    //No need to show empty years
                    if (0 < count) {
                        result.addFacetResult(facetField,
                                              new DiscoverResult.FacetResult(filter, name, null, name, count,
                                                                             DiscoveryConfigurationParameters
                                                                                 .TYPE_DATE));
                    }
                }
            }

            if (solrQueryResponse.getSpellCheckResponse() != null) {
                String recommendedQuery = solrQueryResponse.getSpellCheckResponse().getCollatedResult();
                if (StringUtils.isNotBlank(recommendedQuery)) {
                    result.setSpellCheckQuery(recommendedQuery);
                }
            }
        }

        return result;
    }

    /**
     * Find the indexable object by type and UUID
     *
     * @param context
     *            The relevant DSpace Context.
     * @param doc
     *            the solr document, the following fields MUST be present RESOURCE_TYPE_FIELD, RESOURCE_ID_FIELD and
     *            HANDLE_FIELD
     * @return an IndexableObject
     * @throws SQLException
     *             An exception that provides information on a database access error or other errors.
     */
    protected IndexableObject findIndexableObject(Context context, SolrDocument doc) throws SQLException {
        String type = (String) doc.getFirstValue(SearchUtils.RESOURCE_TYPE_FIELD);
        String id = (String) doc.getFirstValue(SearchUtils.RESOURCE_ID_FIELD);
        final IndexFactory indexableObjectService = indexObjectServiceFactory.
                getIndexFactoryByType(type);
        Optional<IndexableObject> indexableObject = indexableObjectService.findIndexableObject(context, id);

        if (!indexableObject.isPresent()) {
            log.warn("Not able to retrieve object RESOURCE_ID:" + id + " - RESOURCE_TYPE_ID:" + type);
        }
        return indexableObject.orElse(null);
    }

    public List<IndexableObject> search(Context context, String query, int offset, int max,
            String... filterquery) {
        return search(context, query, null, true, offset, max, filterquery);
    }

    @Override
    public List<IndexableObject> search(Context context, String query, String orderfield, boolean ascending,
            int offset, int max, String... filterquery) {

        try {
            if (solrSearchCore.getSolr() == null) {
                return Collections.emptyList();
            }

            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            //Only return obj identifier fields in result doc
            solrQuery.setFields(SearchUtils.RESOURCE_ID_FIELD, SearchUtils.RESOURCE_TYPE_FIELD);
            solrQuery.setStart(offset);
            solrQuery.setRows(max);
            if (orderfield != null) {
                solrQuery.addSort(orderfield, ascending ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
            }
            if (filterquery != null) {
                solrQuery.addFilterQuery(filterquery);
            }
            QueryResponse rsp = solrSearchCore.getSolr().query(solrQuery, SolrRequest.METHOD.POST);
            SolrDocumentList docs = rsp.getResults();

            Iterator iter = docs.iterator();
            List<IndexableObject> result = new ArrayList<IndexableObject>();
            while (iter.hasNext()) {
                SolrDocument doc = (SolrDocument) iter.next();
                IndexableObject o = findIndexableObject(context, doc);
                if (o != null) {
                    result.add(o);
                }
            }
            return result;
        } catch (Exception e) {
            // Any acception that we get ignore it.
            // We do NOT want any crashed to shown by the user
            log.error(LogManager.getHeader(context, "Error while quering solr", "Query: " + query), e);
            return new ArrayList<>(0);
        }
    }

    @Override
    public DiscoverFilterQuery toFilterQuery(Context context, String field, String operator, String value)
        throws SQLException {
        DiscoverFilterQuery result = new DiscoverFilterQuery();

        StringBuilder filterQuery = new StringBuilder();
        if (StringUtils.isNotBlank(field) && StringUtils.isNotBlank(value)) {
            filterQuery.append(field);


            if (operator.endsWith("equals")) {
                filterQuery.append("_keyword");
            } else if (operator.endsWith("authority")) {
                filterQuery.append("_authority");
            }

            if (operator.startsWith("not")) {
                filterQuery.insert(0, "-");
            }



            filterQuery.append(":");
            if ("equals".equals(operator) || "notequals".equals(operator)) {
                //DO NOT ESCAPE RANGE QUERIES !
                if (!value.matches("\\[.*TO.*\\]")) {
                    value = ClientUtils.escapeQueryChars(value);
                    filterQuery.append(value);
                } else {
                    if (value.matches("\\[\\d{1,4} TO \\d{1,4}\\]")) {
                        int minRange = Integer.parseInt(value.substring(1, value.length() - 1).split(" TO ")[0]);
                        int maxRange = Integer.parseInt(value.substring(1, value.length() - 1).split(" TO ")[1]);
                        value = "[" + String.format("%04d", minRange) + " TO " + String.format("%04d", maxRange) + "]";
                    }
                    filterQuery.append(value);
                }
            } else {
                //DO NOT ESCAPE RANGE QUERIES !
                if (!value.matches("\\[.*TO.*\\]")) {
                    value = ClientUtils.escapeQueryChars(value);
                    filterQuery.append("(").append(value).append(")");
                } else {
                    filterQuery.append(value);
                }
            }

            result.setDisplayedValue(transformDisplayedValue(context, field, value));
        }

        result.setFilterQuery(filterQuery.toString());
        return result;
    }

    @Override
    public List<Item> getRelatedItems(Context context, Item item, DiscoveryMoreLikeThisConfiguration mltConfig) {
        List<Item> results = new ArrayList<>();
        try {
            SolrQuery solrQuery = new SolrQuery();
            //Set the query to handle since this is unique
            solrQuery.setQuery(SearchUtils.RESOURCE_UNIQUE_ID + ": " + new IndexableItem(item).getUniqueIndexID());
            //Only return obj identifier fields in result doc
            solrQuery.setFields(SearchUtils.RESOURCE_TYPE_FIELD, SearchUtils.RESOURCE_ID_FIELD);
            //Add the more like this parameters !
            solrQuery.setParam(MoreLikeThisParams.MLT, true);
            //Add a comma separated list of the similar fields
            @SuppressWarnings("unchecked")
            java.util.Collection<String> similarityMetadataFields = CollectionUtils
                .collect(mltConfig.getSimilarityMetadataFields(), new Transformer() {
                    @Override
                    public Object transform(Object input) {
                        //Add the mlt appendix !
                        return input + "_mlt";
                    }
                });

            solrQuery.setParam(MoreLikeThisParams.SIMILARITY_FIELDS, StringUtils.join(similarityMetadataFields, ','));
            solrQuery.setParam(MoreLikeThisParams.MIN_TERM_FREQ, String.valueOf(mltConfig.getMinTermFrequency()));
            solrQuery.setParam(MoreLikeThisParams.DOC_COUNT, String.valueOf(mltConfig.getMax()));
            solrQuery.setParam(MoreLikeThisParams.MIN_WORD_LEN, String.valueOf(mltConfig.getMinWordLength()));

            if (solrSearchCore.getSolr() == null) {
                return Collections.emptyList();
            }
            QueryResponse rsp = solrSearchCore.getSolr().query(solrQuery, SolrRequest.METHOD.POST);
            NamedList mltResults = (NamedList) rsp.getResponse().get("moreLikeThis");
            if (mltResults != null && mltResults.get(item.getType() + "-" + item.getID()) != null) {
                SolrDocumentList relatedDocs = (SolrDocumentList) mltResults.get(item.getType() + "-" + item.getID());
                for (Object relatedDoc : relatedDocs) {
                    SolrDocument relatedDocument = (SolrDocument) relatedDoc;
                    IndexableObject relatedItem = findIndexableObject(context, relatedDocument);
                    if (relatedItem instanceof IndexableItem) {
                        results.add(((IndexableItem) relatedItem).getIndexedObject());
                    }
                }
            }
        } catch (Exception e) {
            log.error(
                LogManager.getHeader(context, "Error while retrieving related items", "Handle: "
                    + item.getHandle()), e);
        }
        return results;
    }

    @Override
    public String toSortFieldIndex(String metadataField, String type) {
        if (StringUtils.equalsIgnoreCase(DiscoverySortConfiguration.SCORE, metadataField)) {
            return DiscoverySortConfiguration.SCORE;
        } else if (StringUtils.equals(type, DiscoveryConfigurationParameters.TYPE_DATE)) {
            return metadataField + "_dt";
        } else {
            return metadataField + "_sort";
        }
    }

    protected String transformFacetField(DiscoverFacetField facetFieldConfig, String field, boolean removePostfix) {
        if (facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT)) {
            if (removePostfix) {
                return field.substring(0, field.lastIndexOf("_filter"));
            } else {
                return field + "_filter";
            }
        } else if (facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
            if (removePostfix) {
                return field.substring(0, field.lastIndexOf(".year"));
            } else {
                return field + ".year";
            }
        } else if (facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_AC)) {
            if (removePostfix) {
                return field.substring(0, field.lastIndexOf("_ac"));
            } else {
                return field + "_ac";
            }
        } else if (facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL)) {
            if (removePostfix) {
                return StringUtils.substringBeforeLast(field, "_tax_");
            } else {
                //Only display top level filters !
                return field + "_tax_0_filter";
            }
        } else if (facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_AUTHORITY)) {
            if (removePostfix) {
                return field.substring(0, field.lastIndexOf("_acid"));
            } else {
                return field + "_acid";
            }
        } else if (facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_STANDARD)) {
            return field;
        } else {
            return field;
        }
    }

    protected String transformDisplayedValue(Context context, String field, String value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (field.equals("location.comm") || field.equals("location.coll")) {
            value = locationToName(context, field, value);
        } else if (field.endsWith("_filter") || field.endsWith("_ac")
            || field.endsWith("_acid")) {
            //We have a filter make sure we split !
            String separator = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                    .getProperty("discovery.solr.facets.split.char");
            if (separator == null) {
                separator = SearchUtils.FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer valueBuffer = new StringBuffer();
            int start = fqParts.length / 2;
            for (int i = start; i < fqParts.length; i++) {
                String[] split = fqParts[i].split(SearchUtils.AUTHORITY_SEPARATOR, 2);
                valueBuffer.append(split[0]);
            }
            value = valueBuffer.toString();
        } else if (value.matches("\\((.*?)\\)")) {
            //The brackets where added for better solr results, remove the first & last one
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    protected String transformAuthorityValue(Context context, String field, String value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (field.equals("location.comm") || field.equals("location.coll")) {
            return value;
        }
        if (field.endsWith("_filter") || field.endsWith("_ac")
            || field.endsWith("_acid")) {
            //We have a filter make sure we split !
            String separator = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                    .getProperty("discovery.solr.facets.split.char");
            if (separator == null) {
                separator = SearchUtils.FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer authorityBuffer = new StringBuffer();
            int start = fqParts.length / 2;
            for (int i = start; i < fqParts.length; i++) {
                String[] split = fqParts[i].split(SearchUtils.AUTHORITY_SEPARATOR, 2);
                if (split.length == 2) {
                    authorityBuffer.append(split[1]);
                }
            }
            if (authorityBuffer.length() > 0) {
                return authorityBuffer.toString();
            }
        }
        return null;
    }

    protected String transformSortValue(Context context, String field, String value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (field.equals("location.comm") || field.equals("location.coll")) {
            value = locationToName(context, field, value);
        } else if (field.endsWith("_filter") || field.endsWith("_ac")
            || field.endsWith("_acid")) {
            //We have a filter make sure we split !
            String separator = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                    .getProperty("discovery.solr.facets.split.char");
            if (separator == null) {
                separator = SearchUtils.FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer valueBuffer = new StringBuffer();
            int end = fqParts.length / 2;
            for (int i = 0; i < end; i++) {
                valueBuffer.append(fqParts[i]);
            }
            value = valueBuffer.toString();
        } else if (value.matches("\\((.*?)\\)")) {
            //The brackets where added for better solr results, remove the first & last one
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    @Override
    public void indexContent(Context context, IndexableObject dso, boolean force,
                             boolean commit) throws SearchServiceException, SQLException {
        indexContent(context, dso, force);
        if (commit) {
            commit();
        }
    }

    @Override
    public void commit() throws SearchServiceException {
        try {
            if (solrSearchCore.getSolr() != null) {
                solrSearchCore.getSolr().commit();
            }
        } catch (Exception e) {
            throw new SearchServiceException(e.getMessage(), e);
        }
    }

    @Override
    public String escapeQueryChars(String query) {
        // Use Solr's built in query escape tool
        // WARNING: You should only escape characters from user entered queries,
        // otherwise you may accidentally BREAK field-based queries (which often
        // rely on special characters to separate the field from the query value)
        return ClientUtils.escapeQueryChars(query);
    }

    @Override
    public FacetYearRange getFacetYearRange(Context context, IndexableObject scope,
                                            DiscoverySearchFilterFacet facet, List<String> filterQueries,
                                            DiscoverQuery parentQuery) throws SearchServiceException {
        FacetYearRange result = new FacetYearRange(facet);
        result.calculateRange(context, filterQueries, scope, this, parentQuery);
        return result;
    }

    @Override
    public String calculateExtremeValue(Context context, String valueField,
                                        String sortField,
                                        DiscoverQuery.SORT_ORDER sortOrder)
        throws SearchServiceException {

        DiscoverQuery maxQuery = new DiscoverQuery();
        maxQuery.setMaxResults(1);
        //Set our query to anything that has this value
        maxQuery.addFieldPresentQueries(valueField);
        //Set sorting so our last value will appear on top
        maxQuery.setSortField(sortField, sortOrder);
        maxQuery.addSearchField(valueField);
        DiscoverResult maxResult = this.search(context,maxQuery);
        if (0 < maxResult.getIndexableObjects().size()) {
            List<DiscoverResult.SearchDocument> searchDocuments = maxResult
                .getSearchDocument(maxResult.getIndexableObjects().get(0));
            if (0 < searchDocuments.size() && 0 < searchDocuments.get(0).getSearchFieldValues
                (valueField).size()) {
                return searchDocuments.get(0).getSearchFieldValues(valueField).get(0);
            }
        }
        return null;
    }
}
