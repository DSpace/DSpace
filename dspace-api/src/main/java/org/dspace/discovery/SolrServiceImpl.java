/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.*;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.*;
import org.dspace.discovery.configuration.*;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.dspace.util.MultiFormatDateParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;

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
 * a spring configuration file located under
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

    private static final Logger log = Logger.getLogger(SolrServiceImpl.class);

    protected static final String LAST_INDEXED_FIELD = "SolrIndexer.lastIndexed";
    protected static final String HANDLE_FIELD = "handle";
    protected static final String RESOURCE_TYPE_FIELD = "search.resourcetype";
    protected static final String RESOURCE_ID_FIELD = "search.resourceid";

    public static final String FILTER_SEPARATOR = "\n|||\n";

    public static final String AUTHORITY_SEPARATOR = "###";

    public static final String STORE_SEPARATOR = "\n|||\n";

    public static final String VARIANTS_STORE_SEPARATOR = "###";

    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;
    @Autowired(required = true)
    protected ChoiceAuthorityService choiceAuthorityService;
    @Autowired(required = true)
    protected CommunityService communityService;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected MetadataAuthorityService metadataAuthorityService;

    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    private HttpSolrServer solr = null;


    protected SolrServiceImpl()
    {

    }

    protected HttpSolrServer getSolr()
    {
        if ( solr == null)
        {
            String solrService = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("discovery.search.server");

            UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
            if (urlValidator.isValid(solrService)||ConfigurationManager.getBooleanProperty("discovery","solr.url.validation.enabled",true))
            {
                try {
                    log.debug("Solr URL: " + solrService);
                    solr = new HttpSolrServer(solrService);

                    solr.setBaseURL(solrService);
                    solr.setUseMultiPartPost(true);
                    // Dummy/test query to search for Item (type=2) of ID=1
                    SolrQuery solrQuery = new SolrQuery()
                            .setQuery(RESOURCE_TYPE_FIELD + ":2 AND " + RESOURCE_ID_FIELD + ":1");
                    // Only return obj identifier fields in result doc
                    solrQuery.setFields(RESOURCE_TYPE_FIELD, RESOURCE_ID_FIELD);
                    solr.query(solrQuery, SolrRequest.METHOD.POST);

                    // As long as Solr initialized, check with DatabaseUtils to see
                    // if a reindex is in order. If so, reindex everything
                    DatabaseUtils.checkReindexDiscovery(this);
                } catch (SolrServerException e) {
                    log.error("Error while initializing solr server", e);
                }
            }
            else
            {
                log.error("Error while initializing solr, invalid url: " + solrService);
            }
        }

        return solr;
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
    public void indexContent(Context context, DSpaceObject dso)
            throws SQLException {
        indexContent(context, dso, false);
    }

    /**
     * If the handle for the "dso" already exists in the index, and the "dso"
     * has a lastModified timestamp that is newer than the document in the index
     * then it is updated, otherwise a new document is added.
     *
     * @param context Users Context
     * @param dso     DSpace Object (Item, Collection or Community
     * @param force   Force update even if not stale.
     * @throws SQLException if error
     */
    @Override
    public void indexContent(Context context, DSpaceObject dso,
                             boolean force) throws SQLException {

        String handle = dso.getHandle();

        if (handle == null)
        {
            handle = handleService.findHandle(context, dso);
        }

        try {
            switch (dso.getType())
            {
                case Constants.ITEM:
                    Item item = (Item) dso;
                    if (item.isArchived() || item.isWithdrawn())
                    {
                        /**
                         * If the item is in the repository now, add it to the index
                         */
                        if (requiresIndexing(handle, ((Item) dso).getLastModified())
                                || force)
                        {
                            unIndexContent(context, handle);
                            buildDocument(context, (Item) dso);
                        }
                    } else {
                        /**
                         * Make sure the item is not in the index if it is not in
                         * archive or withwrawn.
                         */
                        unIndexContent(context, item);
                        log.info("Removed Item: " + handle + " from Index");
                    }
                    break;

                case Constants.COLLECTION:
                    buildDocument(context, (Collection) dso);
                    log.info("Wrote Collection: " + handle + " to Index");
                    break;

                case Constants.COMMUNITY:
                    buildDocument(context, (Community) dso);
                    log.info("Wrote Community: " + handle + " to Index");
                    break;

                default:
                    log
                            .error("Only Items, Collections and Communities can be Indexed");
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * unIndex removes an Item, Collection, or Community
     *
     * @param context
     * @param dso     DSpace Object, can be Community, Item, or Collection
     * @throws SQLException if database error
     * @throws IOException if IO error
     */
    @Override
    public void unIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException {
        unIndexContent(context, dso, false);
    }

    /**
     * unIndex removes an Item, Collection, or Community
     *
     * @param context
     * @param dso     DSpace Object, can be Community, Item, or Collection
     * @param commit if <code>true</code> force an immediate commit on SOLR
     * @throws SQLException if database error
     * @throws IOException if IO error
     */
    @Override
    public void unIndexContent(Context context, DSpaceObject dso, boolean commit)
            throws SQLException, IOException {
        try {
            if (dso == null)
            {
                return;
            }
            String uniqueID = dso.getType()+"-"+dso.getID();
            getSolr().deleteById(uniqueID);
            if(commit)
            {
                getSolr().commit();
            }
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            emailException(exception);
        }
    }

    /**
     * Unindex a Document in the Lucene index.
     * @param context the dspace context
     * @param handle the handle of the object to be deleted
     * @throws IOException if IO error
     * @throws SQLException if database error
     */
    @Override
    public void unIndexContent(Context context, String handle) throws IOException, SQLException {
        unIndexContent(context, handle, false);
    }

    /**
     * Unindex a Document in the Lucene Index.
     * @param context the dspace context
     * @param handle the handle of the object to be deleted
     * @throws SQLException if database error
     * @throws IOException if IO error
     */
    @Override
    public void unIndexContent(Context context, String handle, boolean commit)
            throws SQLException, IOException {

        try {
            if(getSolr() != null){
                getSolr().deleteByQuery(HANDLE_FIELD + ":\"" + handle + "\"");
                if(commit)
                {
                    getSolr().commit();
                }
            }
        } catch (SolrServerException e)
        {
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
    public void reIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException {
        try {
            indexContent(context, dso);
        } catch (Exception exception)
        {
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
    public void updateIndex(Context context)
    {
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
     * @param force whether or not to force the reindexing
     */
    @Override
    public void updateIndex(Context context, boolean force)
    {
        try {
            Iterator<Item> items = null;
            for (items = itemService.findAllUnfiltered(context); items.hasNext();)
            {
                Item item = items.next();
                indexContent(context, item, force);
                //To prevent memory issues, discard an object from the cache after processing
                context.uncacheEntity(item);
            }

            List<Collection> collections = collectionService.findAll(context);
            for (Collection collection : collections)
            {
                indexContent(context, collection, force);
            }

            List<Community> communities = communityService.findAll(context);
            for (Community community : communities)
            {
                indexContent(context, community, force);
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Iterates over all documents in the Lucene index and verifies they are in
     * database, if not, they are removed.
     *
     * @param force whether or not to force a clean index
     * @throws IOException IO exception
     * @throws SQLException sql exception
     * @throws SearchServiceException occurs when something went wrong with querying the solr server
     */
    @Override
    public void cleanIndex(boolean force) throws IOException,
            SQLException, SearchServiceException {

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try
        {
            if(getSolr() == null)
            {
                return;
            }
            if (force)
            {
                getSolr().deleteByQuery(RESOURCE_TYPE_FIELD + ":[2 TO 4]");
            } else {
                SolrQuery query = new SolrQuery();
                // Query for all indexed Items, Collections and Communities,
                // returning just their handle
                query.setFields(HANDLE_FIELD);
                query.setQuery(RESOURCE_TYPE_FIELD + ":[2 TO 4]");
                QueryResponse rsp = getSolr().query(query, SolrRequest.METHOD.POST);
                SolrDocumentList docs = rsp.getResults();

                Iterator iter = docs.iterator();
                while (iter.hasNext())
                {

                 SolrDocument doc = (SolrDocument) iter.next();

                String handle = (String) doc.getFieldValue(HANDLE_FIELD);

                DSpaceObject o = handleService.resolveToObject(context, handle);

                if (o == null)
                {
                    log.info("Deleting: " + handle);
                    /*
                          * Use IndexWriter to delete, its easier to manage
                          * write.lock
                          */
                    unIndexContent(context, handle);
                } else {
                    log.debug("Keeping: " + handle);
                }
            }
            }
        } catch(Exception e)
        {

            throw new SearchServiceException(e.getMessage(), e);
        } finally
        {
            context.abort();
        }




    }

    /**
     * Maintenance to keep a SOLR index efficient.
     * Note: This might take a long time.
     */
    @Override
    public void optimize()
    {
        try {
            if(getSolr() == null)
            {
                return;
            }
            long start = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Started:" + start);
            getSolr().optimize();
            long finish = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Finished:" + finish);
            System.out.println("SOLR Search Optimize -- Total time taken:" + (finish - start) + " (ms).");
        } catch (SolrServerException sse)
        {
            System.err.println(sse.getMessage());
        } catch (IOException ioe)
        {
            System.err.println(ioe.getMessage());
        }
    }

    @Override
    public void buildSpellCheck() throws SearchServiceException {
        try {
            if (getSolr() == null) {
                return;
            }
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.set("spellcheck", true);
            solrQuery.set(SpellingParams.SPELLCHECK_BUILD, true);
            getSolr().query(solrQuery, SolrRequest.METHOD.POST);
        }catch (SolrServerException e)
        {
            //Make sure to also log the exception since this command is usually run from a crontab.
            log.error(e, e);
            throw new SearchServiceException(e);
        }
    }

    // //////////////////////////////////
    // Private
    // //////////////////////////////////

    protected void emailException(Exception exception)
    {
        // Also email an alert, system admin may need to check for stale lock
        try {
            String recipient = ConfigurationManager
                    .getProperty("alert.recipient");

            if (StringUtils.isNotBlank(recipient))
            {
                Email email = Email
                        .getEmail(I18nUtil.getEmailFilename(
                                Locale.getDefault(), "internal_error"));
                email.addRecipient(recipient);
                email.addArgument(ConfigurationManager
                        .getProperty("dspace.url"));
                email.addArgument(new Date());

                String stackTrace;

                if (exception != null)
                {
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
        } catch (Exception e)
        {
            // Not much we can do here!
            log.warn("Unable to send email alert", e);
        }

    }


    /**
     * Is stale checks the lastModified time stamp in the database and the index
     * to determine if the index is stale.
     *
     * @param handle the handle of the dso
     * @param lastModified the last modified date of the DSpace object
     * @return a boolean indicating if the dso should be re indexed again
     * @throws SQLException sql exception
     * @throws IOException io exception
     * @throws SearchServiceException if something went wrong with querying the solr server
     */
    protected boolean requiresIndexing(String handle, Date lastModified)
            throws SQLException, IOException, SearchServiceException {

        boolean reindexItem = false;
        boolean inIndex = false;

        SolrQuery query = new SolrQuery();
        query.setQuery(HANDLE_FIELD + ":" + handle);
        // Specify that we ONLY want the LAST_INDEXED_FIELD returned in the field list (fl)
        query.setFields(LAST_INDEXED_FIELD);
        QueryResponse rsp;

        try {
            if(getSolr() == null)
            {
                return false;
            }
            rsp = getSolr().query(query, SolrRequest.METHOD.POST);
        } catch (SolrServerException e)
        {
            throw new SearchServiceException(e.getMessage(),e);
        }

        for (SolrDocument doc : rsp.getResults())
        {

            inIndex = true;

            Object value = doc.getFieldValue(LAST_INDEXED_FIELD);

            if(value instanceof Date)
            {
                Date lastIndexed = (Date) value;

                if (lastIndexed.before(lastModified))
                {

                    reindexItem = true;
                }
            }
        }

        return reindexItem || !inIndex;
    }


    /**
     * @param context DSpace context
     * @param myitem the item for which our locations are to be retrieved
     * @return a list containing the identifiers of the communities and collections
     * @throws SQLException sql exception
     */
    protected List<String> getItemLocations(Context context, Item myitem)
            throws SQLException {
        List<String> locations = new Vector<String>();

        // build list of community ids
        List<Community> communities = itemService.getCommunities(context, myitem);

        // build list of collection ids
        List<Collection> collections = myitem.getCollections();

        // now put those into strings
        int i = 0;

        for (i = 0; i < communities.size(); i++)
        {
            locations.add("m" + communities.get(i).getID());
        }

        for (i = 0; i < collections.size(); i++)
        {
            locations.add("l" + collections.get(i).getID());
        }

        return locations;
    }

    protected List<String> getCollectionLocations(Context context, Collection target) throws SQLException {
        List<String> locations = new Vector<String>();
        // build list of community ids
        List<Community> communities = communityService.getAllParents(context, target);

        // now put those into strings
        for (Community community : communities)
        {
            locations.add("m" + community.getID());
        }

        return locations;
    }
    
    @Override
    public String createLocationQueryForAdministrableItems(Context context)
            throws SQLException
    {
        StringBuilder locationQuery = new StringBuilder();
        
        if (context.getCurrentUser() != null) 
        {
            List<Group> groupList = EPersonServiceFactory.getInstance().getGroupService()
                    .allMemberGroups(context, context.getCurrentUser());
            
            List<ResourcePolicy> communitiesPolicies = AuthorizeServiceFactory.getInstance().getResourcePolicyService()
                    .find(context, context.getCurrentUser(), groupList, Constants.ADMIN, Constants.COMMUNITY);

            List<ResourcePolicy> collectionsPolicies = AuthorizeServiceFactory.getInstance().getResourcePolicyService()
                    .find(context, context.getCurrentUser(), groupList, Constants.ADMIN, Constants.COLLECTION);

            List<Collection> allCollections = new ArrayList<>();
            
            for( ResourcePolicy rp: collectionsPolicies){
                Collection collection = ContentServiceFactory.getInstance().getCollectionService()
                        .find(context, rp.getdSpaceObject().getID());
                allCollections.add(collection);
            }

            if (CollectionUtils.isNotEmpty(communitiesPolicies) || CollectionUtils.isNotEmpty(allCollections)) 
            {
                locationQuery.append("location:( ");

                for (int i = 0; i< communitiesPolicies.size(); i++) 
                {
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
     * Write the document to the index under the appropriate handle.
     *
     * @param doc
     *     the solr document to be written to the server
     * @param streams
     *     list of bitstream content streams
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     */
    protected void writeDocument(SolrInputDocument doc, FullTextContentStreams streams) throws IOException {

        try {
            if(getSolr() != null)
            {
                if (streams != null && !streams.isEmpty())
                {
                    ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update/extract");
                    req.addContentStream(streams);

                    ModifiableSolrParams params = new ModifiableSolrParams();

                    //req.setParam(ExtractingParams.EXTRACT_ONLY, "true");
                    for(String name : doc.getFieldNames())
                    {
                        for(Object val : doc.getFieldValues(name))
                        {
                             params.add(ExtractingParams.LITERALS_PREFIX + name,val.toString());
                        }
                    }

                    req.setParams(params);
                    req.setParam(ExtractingParams.UNKNOWN_FIELD_PREFIX, "attr_");
                    req.setParam(ExtractingParams.MAP_PREFIX + "content", "fulltext");
                    req.setParam(ExtractingParams.EXTRACT_FORMAT, "text");
                    req.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
                    req.process(getSolr());
                }
                else
                {
                    getSolr().add(doc);
                }
            }
        } catch (SolrServerException e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Build a solr document for a DSpace Community.
     *
     * @param community Community to be indexed
     * @throws SQLException if database error
     * @throws IOException if IO error
     */
    protected void buildDocument(Context context, Community community)
    throws SQLException, IOException {
        // Create Document
        SolrInputDocument doc = buildDocument(Constants.COMMUNITY, community.getID(),
                community.getHandle(), null);

        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(community);
        DiscoveryHitHighlightingConfiguration highlightingConfiguration = discoveryConfiguration.getHitHighlightingConfiguration();
        List<String> highlightedMetadataFields = new ArrayList<String>();
        if(highlightingConfiguration != null)
        {
            for (DiscoveryHitHighlightFieldConfiguration configuration : highlightingConfiguration.getMetadataFields())
            {
                highlightedMetadataFields.add(configuration.getField());
            }
        }

        // and populate it
        String description = communityService.getMetadata(community, "introductory_text");
        String description_abstract = communityService.getMetadata(community, "short_description");
        String description_table = communityService.getMetadata(community, "side_bar_text");
        String rights = communityService.getMetadata(community, "copyright_text");
        String title = communityService.getMetadata(community, "name");

        List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(community.getType());
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description", description);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description.abstract", description_abstract);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description.tableofcontents", description_table);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.rights", rights);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.title", title);

        //Do any additional indexing, depends on the plugins
        List<SolrServiceIndexPlugin> solrServiceIndexPlugins = DSpaceServicesFactory.getInstance().getServiceManager().getServicesByType(SolrServiceIndexPlugin.class);
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
        {
            solrServiceIndexPlugin.additionalIndex(context, community, doc);
        }

        writeDocument(doc, null);
    }

    /**
     * Build a solr document for a DSpace Collection.
     *
     * @param collection Collection to be indexed
     * @throws SQLException sql exception
     * @throws IOException IO exception
     */
    protected void buildDocument(Context context, Collection collection)
    throws SQLException, IOException {
        List<String> locations = getCollectionLocations(context, collection);

        // Create Lucene Document
        SolrInputDocument doc = buildDocument(Constants.COLLECTION, collection.getID(),
                collection.getHandle(), locations);

        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(collection);
        DiscoveryHitHighlightingConfiguration highlightingConfiguration = discoveryConfiguration.getHitHighlightingConfiguration();
        List<String> highlightedMetadataFields = new ArrayList<String>();
        if(highlightingConfiguration != null)
        {
            for (DiscoveryHitHighlightFieldConfiguration configuration : highlightingConfiguration.getMetadataFields())
            {
                highlightedMetadataFields.add(configuration.getField());
            }
        }


        // and populate it
        String description = collectionService.getMetadata(collection, "introductory_text");
        String description_abstract = collectionService.getMetadata(collection, "short_description");
        String description_table = collectionService.getMetadata(collection, "side_bar_text");
        String provenance = collectionService.getMetadata(collection, "provenance_description");
        String rights = collectionService.getMetadata(collection, "copyright_text");
        String rights_license = collectionService.getMetadata(collection, "license");
        String title = collectionService.getMetadata(collection, "name");

        List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(collection.getType());
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description", description);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description.abstract", description_abstract);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.description.tableofcontents", description_table);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.provenance", provenance);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.rights", rights);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.rights.license", rights_license);
        addContainerMetadataField(doc, highlightedMetadataFields, toIgnoreMetadataFields, "dc.title", title);


        //Do any additional indexing, depends on the plugins
        List<SolrServiceIndexPlugin> solrServiceIndexPlugins = DSpaceServicesFactory.getInstance().getServiceManager().getServicesByType(SolrServiceIndexPlugin.class);
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
        {
            solrServiceIndexPlugin.additionalIndex(context, collection, doc);
        }

        writeDocument(doc, null);
    }

    /**
     * Add the metadata value of the community/collection to the solr document
     * IF needed highlighting is added !
     * @param doc the solr document
     * @param highlightedMetadataFields the list of metadata fields that CAN be highlighted
     * @param metadataField the metadata field added
     * @param value the value (can be NULL !)
     */
    protected void addContainerMetadataField(SolrInputDocument doc, List<String> highlightedMetadataFields, List<String> toIgnoreMetadataFields, String metadataField, String value)
    {
        if(toIgnoreMetadataFields == null || !toIgnoreMetadataFields.contains(metadataField))
        {
            if(StringUtils.isNotBlank(value))
            {
                doc.addField(metadataField, value);
                if(highlightedMetadataFields.contains(metadataField))
                {
                    doc.addField(metadataField + "_hl", value);
                }
            }
        }
    }

    /**
     * Build a Lucene document for a DSpace Item and write the index
     *
     * @param context Users Context
     * @param item    The DSpace Item to be indexed
     * @throws SQLException if database error
     * @throws IOException if IO error
     */
    protected void buildDocument(Context context, Item item)
            throws SQLException, IOException {
        String handle = item.getHandle();

        if (handle == null)
        {
            handle = handleService.findHandle(context, item);
        }

        // get the location string (for searching by collection & community)
        List<String> locations = getItemLocations(context, item);

        SolrInputDocument doc = buildDocument(Constants.ITEM, item.getID(), handle,
                locations);

        log.debug("Building Item: " + handle);

        doc.addField("archived", item.isArchived());
        doc.addField("withdrawn", item.isWithdrawn());
        doc.addField("discoverable", item.isDiscoverable());
        doc.addField("lastModified", item.getLastModified());

        //Keep a list of our sort values which we added, sort values can only be added once
        List<String> sortFieldsAdded = new ArrayList<String>();
        Set<String> hitHighlightingFields = new HashSet<String>();
        try {
            List<DiscoveryConfiguration> discoveryConfigurations = SearchUtils.getAllDiscoveryConfigurations(item);

            //A map used to save each sidebarFacet config by the metadata fields
            Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<String, List<DiscoverySearchFilter>>();
            Map<String, DiscoverySortFieldConfiguration> sortFields = new HashMap<String, DiscoverySortFieldConfiguration>();
            Map<String, DiscoveryRecentSubmissionsConfiguration> recentSubmissionsConfigurationMap = new HashMap<String, DiscoveryRecentSubmissionsConfiguration>();
            Set<String> moreLikeThisFields = new HashSet<String>();
            for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations)
            {
                for (int i = 0; i < discoveryConfiguration.getSearchFilters().size(); i++)
                {
                    DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration.getSearchFilters().get(i);
                    for (int j = 0; j < discoverySearchFilter.getMetadataFields().size(); j++)
                    {
                        String metadataField = discoverySearchFilter.getMetadataFields().get(j);
                        List<DiscoverySearchFilter> resultingList;
                        if(searchFilters.get(metadataField) != null)
                        {
                            resultingList = searchFilters.get(metadataField);
                        }else{
                            //New metadata field, create a new list for it
                            resultingList = new ArrayList<DiscoverySearchFilter>();
                        }
                        resultingList.add(discoverySearchFilter);

                        searchFilters.put(metadataField, resultingList);
                    }
                }

                DiscoverySortConfiguration sortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
                if(sortConfiguration != null)
                {
                    for (DiscoverySortFieldConfiguration discoverySortConfiguration : sortConfiguration.getSortFields())
                    {
                        sortFields.put(discoverySortConfiguration.getMetadataField(), discoverySortConfiguration);
                    }
                }

                DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration = discoveryConfiguration.getRecentSubmissionConfiguration();
                if(recentSubmissionConfiguration != null)
                {
                    recentSubmissionsConfigurationMap.put(recentSubmissionConfiguration.getMetadataSortField(), recentSubmissionConfiguration);
                }

                DiscoveryHitHighlightingConfiguration hitHighlightingConfiguration = discoveryConfiguration.getHitHighlightingConfiguration();
                if(hitHighlightingConfiguration != null)
                {
                    List<DiscoveryHitHighlightFieldConfiguration> fieldConfigurations = hitHighlightingConfiguration.getMetadataFields();
                    for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : fieldConfigurations)
                    {
                        hitHighlightingFields.add(fieldConfiguration.getField());
                    }
            	}
                DiscoveryMoreLikeThisConfiguration moreLikeThisConfiguration = discoveryConfiguration.getMoreLikeThisConfiguration();
                if(moreLikeThisConfiguration != null)
                {
                    for(String metadataField : moreLikeThisConfiguration.getSimilarityMetadataFields())
                    {
                        moreLikeThisFields.add(metadataField);
                    }
                }
            }


            List<String> toProjectionFields = new ArrayList<String>();
            String[] projectionFields = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("discovery.index.projection");
            if(projectionFields != null){
                for (String field : projectionFields) {
                    toProjectionFields.add(field.trim());
                }
            }

            List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(item.getType());
            List<MetadataValue> mydc = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            for (MetadataValue meta : mydc)
            {
                MetadataField metadataField = meta.getMetadataField();
                MetadataSchema metadataSchema = metadataField.getMetadataSchema();
                String field = metadataSchema.getName() + "." + metadataField.getElement();
                String unqualifiedField = field;

                String value = meta.getValue();

                if (value == null)
                {
                    continue;
                }

                if (metadataField.getQualifier() != null && !metadataField.getQualifier().trim().equals(""))
                {
                    field += "." + metadataField.getQualifier();
                }

                //We are not indexing provenance, this is useless
                if (toIgnoreMetadataFields != null && (toIgnoreMetadataFields.contains(field) || toIgnoreMetadataFields.contains(unqualifiedField + "." + Item.ANY)))
                {
                    continue;
                }

                String authority = null;
                String preferedLabel = null;
                List<String> variants = null;
                boolean isAuthorityControlled = metadataAuthorityService
                        .isAuthorityControlled(metadataField);

                int minConfidence = isAuthorityControlled? metadataAuthorityService
                        .getMinConfidence(metadataField) : Choices.CF_ACCEPTED;

                if (isAuthorityControlled && meta.getAuthority() != null
                        && meta.getConfidence() >= minConfidence)
                {
                    boolean ignoreAuthority = DSpaceServicesFactory.getInstance().getConfigurationService()
                            .getPropertyAsType(
                                    "discovery.index.authority.ignore." + field,
                                    DSpaceServicesFactory.getInstance().getConfigurationService()
                                            .getPropertyAsType(
                                                    "discovery.index.authority.ignore",
                                                    new Boolean(false)), true);
                    if (!ignoreAuthority)
                    {
                        authority = meta.getAuthority();

                        boolean ignorePrefered = DSpaceServicesFactory.getInstance().getConfigurationService()
                                .getPropertyAsType(
                                        "discovery.index.authority.ignore-prefered."
                                                + field,
                                        DSpaceServicesFactory.getInstance().getConfigurationService()
                                                .getPropertyAsType(
                                                        "discovery.index.authority.ignore-prefered",
                                                        new Boolean(false)),
                                        true);
                        if (!ignorePrefered)
                        {

                            preferedLabel = choiceAuthorityService
                                    .getLabel(meta, meta.getLanguage());
                        }

                        boolean ignoreVariants = DSpaceServicesFactory.getInstance().getConfigurationService()
                                .getPropertyAsType(
                                        "discovery.index.authority.ignore-variants."
                                                + field,
                                        DSpaceServicesFactory.getInstance().getConfigurationService()
                                                .getPropertyAsType(
                                                        "discovery.index.authority.ignore-variants",
                                                        new Boolean(false)),
                                        true);
                        if (!ignoreVariants)
                        {
                            variants = choiceAuthorityService
                                    .getVariants(meta);
                        }

                    }
                }

                if ((searchFilters.get(field) != null || searchFilters.get(unqualifiedField + "." + Item.ANY) != null))
                {
                    List<DiscoverySearchFilter> searchFilterConfigs = searchFilters.get(field);
                    if(searchFilterConfigs == null)
                    {
                        searchFilterConfigs = searchFilters.get(unqualifiedField + "." + Item.ANY);
                    }

                    for (DiscoverySearchFilter searchFilter : searchFilterConfigs)
                    {
                        Date date = null;
                        String separator = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("discovery.solr.facets.split.char");
                        if(separator == null)
                        {
                            separator = FILTER_SEPARATOR;
                        }
                        if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
                        {
                            //For our search filters that are dates we format them properly
                            date = MultiFormatDateParser.parse(value);
                            if(date != null)
                            {
                                //TODO: make this date format configurable !
                                value = DateFormatUtils.formatUTC(date, "yyyy-MM-dd");
                            }
                        }
                        doc.addField(searchFilter.getIndexFieldName(), value);
                        doc.addField(searchFilter.getIndexFieldName() + "_keyword", value);

                        if (authority != null && preferedLabel == null)
                        {
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword", value + AUTHORITY_SEPARATOR
                                    + authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_authority", authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_acid", value.toLowerCase()
                                    + separator + value
                                    + AUTHORITY_SEPARATOR + authority);
                        }

                        if (preferedLabel != null)
                        {
                            doc.addField(searchFilter.getIndexFieldName(),
                                    preferedLabel);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword", preferedLabel);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword", preferedLabel
                                    + AUTHORITY_SEPARATOR + authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_authority", authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_acid", preferedLabel.toLowerCase()
                                    + separator + preferedLabel
                                    + AUTHORITY_SEPARATOR + authority);
                        }
                        if (variants != null)
                        {
                            for (String var : variants)
                            {
                                doc.addField(searchFilter.getIndexFieldName() + "_keyword", var);
                                doc.addField(searchFilter.getIndexFieldName()
                                        + "_acid", var.toLowerCase()
                                        + separator + var
                                        + AUTHORITY_SEPARATOR + authority);
                            }
                        }

                        //Add a dynamic fields for auto complete in search
                        doc.addField(searchFilter.getIndexFieldName() + "_ac",
                                value.toLowerCase() + separator + value);
                        if (preferedLabel != null)
                        {
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_ac", preferedLabel.toLowerCase()
                                    + separator + preferedLabel);
                        }
                        if (variants != null)
                        {
                            for (String var : variants)
                            {
                                doc.addField(searchFilter.getIndexFieldName()
                                        + "_ac", var.toLowerCase() + separator
                                        + var);
                            }
                        }

                        if(searchFilter.getFilterType().equals(DiscoverySearchFilterFacet.FILTER_TYPE_FACET))
                        {
                            if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT))
                            {
                            	//Add a special filter
                           	 	//We use a separator to split up the lowercase and regular case, this is needed to get our filters in regular case
                            	//Solr has issues with facet prefix and cases
                            	if (authority != null)
                            	{
                                	String facetValue = preferedLabel != null?preferedLabel:value;
                                	doc.addField(searchFilter.getIndexFieldName() + "_filter", facetValue.toLowerCase() + separator + facetValue + AUTHORITY_SEPARATOR + authority);
                            	}
                            	else
                            	{
                                	doc.addField(searchFilter.getIndexFieldName() + "_filter", value.toLowerCase() + separator + value);
                            	}
                            }else
                                if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
                                {
                                    if(date != null)
                                    {
                                        String indexField = searchFilter.getIndexFieldName() + ".year";
                                        String yearUTC = DateFormatUtils.formatUTC(date, "yyyy");
										doc.addField(searchFilter.getIndexFieldName() + "_keyword", yearUTC);
										// add the year to the autocomplete index
										doc.addField(searchFilter.getIndexFieldName() + "_ac", yearUTC);
										doc.addField(indexField, yearUTC);

                                    	if (yearUTC.startsWith("0"))
                                        {
        									doc.addField(
        											searchFilter.getIndexFieldName()
        													+ "_keyword",
        													yearUTC.replaceFirst("0*", ""));
        									// add date without starting zeros for autocomplete e filtering
        									doc.addField(
        											searchFilter.getIndexFieldName()
        													+ "_ac",
        													yearUTC.replaceFirst("0*", ""));
        									doc.addField(
        											searchFilter.getIndexFieldName()
        													+ "_ac",
        													value.replaceFirst("0*", ""));
        									doc.addField(
        											searchFilter.getIndexFieldName()
        													+ "_keyword",
        													value.replaceFirst("0*", ""));
                                        }

                                    	//Also save a sort value of this year, this is required for determining the upper & lower bound year of our facet
                                        if(doc.getField(indexField + "_sort") == null)
                                        {
                                        	//We can only add one year so take the first one
                                        	doc.addField(indexField + "_sort", yearUTC);
                                    	}
                                }
                            }else
                            if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL))
                            {
                                HierarchicalSidebarFacetConfiguration hierarchicalSidebarFacetConfiguration = (HierarchicalSidebarFacetConfiguration) searchFilter;
                                String[] subValues = value.split(hierarchicalSidebarFacetConfiguration.getSplitter());
                                if(hierarchicalSidebarFacetConfiguration.isSkipFirstNodeLevel() && 1 < subValues.length)
                                {
                                    //Remove the first element of our array
                                    subValues = (String[]) ArrayUtils.subarray(subValues, 1, subValues.length);
                                }
                                for (int i = 0; i < subValues.length; i++)
                                {
                                    StringBuilder valueBuilder = new StringBuilder();
                                    for(int j = 0; j <= i; j++)
                                    {
                                        valueBuilder.append(subValues[j]);
                                        if(j < i)
                                        {
                                            valueBuilder.append(hierarchicalSidebarFacetConfiguration.getSplitter());
                                        }
                                    }

                                    String indexValue = valueBuilder.toString().trim();
                                    doc.addField(searchFilter.getIndexFieldName() + "_tax_" + i + "_filter", indexValue.toLowerCase() + separator + indexValue);
                                    //We add the field x times that it has occurred
                                    for(int j = i; j < subValues.length; j++)
                                    {
                                        doc.addField(searchFilter.getIndexFieldName() + "_filter", indexValue.toLowerCase() + separator + indexValue);
                                        doc.addField(searchFilter.getIndexFieldName() + "_keyword", indexValue);
                                    }
                                }
                            }
                        }
                    }
                }

                if ((sortFields.get(field) != null || recentSubmissionsConfigurationMap.get(field) != null) && !sortFieldsAdded.contains(field))
                {
                    //Only add sort value once
                    String type;
                    if(sortFields.get(field) != null)
                    {
                        type = sortFields.get(field).getType();
                    }else{
                        type = recentSubmissionsConfigurationMap.get(field).getType();
                    }

                    if(type.equals(DiscoveryConfigurationParameters.TYPE_DATE))
                    {
                        Date date = MultiFormatDateParser.parse(value);
                        if(date != null)
                        {
                            doc.addField(field + "_dt", date);
                        }else{
                            log.warn("Error while indexing sort date field, item: " + item.getHandle() + " metadata field: " + field + " date value: " + date);
                        }
                    }else{
                        doc.addField(field + "_sort", value);
                    }
                    sortFieldsAdded.add(field);
                }

                if(hitHighlightingFields.contains(field) || hitHighlightingFields.contains("*") || hitHighlightingFields.contains(unqualifiedField + "." + Item.ANY))
                {
                    doc.addField(field + "_hl", value);
                }

                if(moreLikeThisFields.contains(field) || moreLikeThisFields.contains(unqualifiedField + "." + Item.ANY))
                {
                    doc.addField(field + "_mlt", value);
                }

                doc.addField(field, value);
                if (toProjectionFields.contains(field) || toProjectionFields.contains(unqualifiedField + "." + Item.ANY))
                {
                    StringBuffer variantsToStore = new StringBuffer();
                    if (variants != null)
                    {
                        for (String var : variants)
                        {
                            variantsToStore.append(VARIANTS_STORE_SEPARATOR);
                            variantsToStore.append(var);
                        }
                    }
                    doc.addField(
                            field + "_stored",
                            value + STORE_SEPARATOR + preferedLabel
                                    + STORE_SEPARATOR
                                    + (variantsToStore.length() > VARIANTS_STORE_SEPARATOR
                                            .length() ? variantsToStore
                                            .substring(VARIANTS_STORE_SEPARATOR
                                                    .length()) : "null")
                                    + STORE_SEPARATOR + authority
                                    + STORE_SEPARATOR + meta.getLanguage());
                }

                if (meta.getLanguage() != null && !meta.getLanguage().trim().equals(""))
                {
                    String langField = field + "." + meta.getLanguage();
                    doc.addField(langField, value);
                }
            }

        } catch (Exception e)  {
            log.error(e.getMessage(), e);
        }


        log.debug("  Added Metadata");

        try {

            List<MetadataValue> values = itemService.getMetadataByMetadataString(item, "dc.relation.ispartof");

            if(values != null && values.size() > 0 && values.get(0) != null && values.get(0).getValue() != null)
            {
                // group on parent
                String handlePrefix = handleService.getCanonicalPrefix();

                doc.addField("publication_grp",values.get(0).getValue().replaceFirst(handlePrefix, "") );

            }
            else
            {
                // group on self
                doc.addField("publication_grp", item.getHandle());
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(),e);
        }


        log.debug("  Added Grouping");

        //Do any additional indexing, depends on the plugins
        List<SolrServiceIndexPlugin> solrServiceIndexPlugins = DSpaceServicesFactory.getInstance().getServiceManager().getServicesByType(SolrServiceIndexPlugin.class);
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
        {
            solrServiceIndexPlugin.additionalIndex(context, item, doc);
        }

        // write the index and close the inputstreamreaders
        try {
            writeDocument(doc, new FullTextContentStreams(context, item));
            log.info("Wrote Item: " + handle + " to Index");
        } catch (RuntimeException e)
        {
            log.error("Error while writing item to discovery index: " + handle + " message:"+ e.getMessage(), e);
        }
    }

    /**
     * Create Lucene document with all the shared fields initialized.
     *
     * @param type      Type of DSpace Object
     * @param id
     * @param handle
     * @param locations @return
     */
    protected SolrInputDocument buildDocument(int type, UUID id, String handle,
                                            List<String> locations)
    {
        SolrInputDocument doc = new SolrInputDocument();

        // want to be able to check when last updated
        // (not tokenized, but it is indexed)
        doc.addField(LAST_INDEXED_FIELD, new Date());

        // New fields to weaken the dependence on handles, and allow for faster
        // list display
        doc.addField("search.uniqueid", type+"-"+id);
        doc.addField(RESOURCE_TYPE_FIELD, Integer.toString(type));
        doc.addField(RESOURCE_ID_FIELD, id.toString());

        // want to be able to search for handle, so use keyword
        // (not tokenized, but it is indexed)
        if (handle != null)
        {
            // want to be able to search for handle, so use keyword
            // (not tokenized, but it is indexed)
            doc.addField(HANDLE_FIELD, handle);
        }

        if (locations != null)
        {
            for (String location : locations)
            {
                doc.addField("location", location);
                if (location.startsWith("m"))
                {
                    doc.addField("location.comm", location.substring(1));
                }
                else
                {
                    doc.addField("location.coll", location.substring(1));
                }
            }
        }

        return doc;
    }

    /**
     * Helper function to retrieve a date using a best guess of the potential
     * date encodings on a field
     *
     * @param t the string to be transformed to a date
     * @return a date if the formatting was successful, null if not able to transform to a date
     */
    public Date toDate(String t)
    {
        SimpleDateFormat[] dfArr;

        // Choose the likely date formats based on string length
        switch (t.length())
        {
			// case from 1 to 3 go through adding anyone a single 0. Case 4 define
			// for all the SimpleDateFormat
        	case 1:
        		t = "0" + t;
        	case 2:
        		t = "0" + t;
        	case 3:
        		t = "0" + t;
            case 4:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy")};
                break;
            case 6:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyyMM")};
                break;
            case 7:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy-MM")};
                break;
            case 8:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyyMMdd"),
                        new SimpleDateFormat("yyyy MMM")};
                break;
            case 10:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy-MM-dd")};
                break;
            case 11:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy MMM dd")};
                break;
            case 20:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'")};
                break;
            default:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")};
                break;
        }

        for (SimpleDateFormat df : dfArr)
        {
            try {
                // Parse the date
                df.setCalendar(Calendar
                        .getInstance(TimeZone.getTimeZone("UTC")));
                df.setLenient(false);
                return df.parse(t);
            } catch (ParseException pe)
            {
                log.error("Unable to parse date format", pe);
            }
        }

        return null;
    }

    public String locationToName(Context context, String field, String value) throws SQLException {
        if("location.comm".equals(field) || "location.coll".equals(field))
        {
            int type = ("location.comm").equals(field) ? Constants.COMMUNITY : Constants.COLLECTION;
            DSpaceObject commColl = null;
            if (StringUtils.isNotBlank(value))
            {
                commColl = contentServiceFactory.getDSpaceObjectService(type).find(context, UUID.fromString(value));
            }
            if(commColl != null)
            {
                return commColl.getName();
            }

        }
        return value;
    }

    //========== SearchService implementation
    @Override
    public DiscoverResult search(Context context, DiscoverQuery query) throws SearchServiceException
    {
        return search(context, query, false);
    }

    @Override
    public DiscoverResult search(Context context, DSpaceObject dso,
            DiscoverQuery query)
            throws SearchServiceException
    {
        return search(context, dso, query, false);
    }

    @Override
    public DiscoverResult search(Context context, DSpaceObject dso, DiscoverQuery discoveryQuery, boolean includeUnDiscoverable) throws SearchServiceException {
        if(dso != null)
        {
            if (dso instanceof Community)
            {
                discoveryQuery.addFilterQueries("location:m" + dso.getID());
            } else if (dso instanceof Collection)
            {
                discoveryQuery.addFilterQueries("location:l" + dso.getID());
            } else if (dso instanceof Item)
            {
                discoveryQuery.addFilterQueries(HANDLE_FIELD + ":" + dso.getHandle());
            }
        }
        return search(context, discoveryQuery, includeUnDiscoverable);

    }


    @Override
    public DiscoverResult search(Context context, DiscoverQuery discoveryQuery, boolean includeUnDiscoverable) throws SearchServiceException {
        try {
            if(getSolr() == null){
                return new DiscoverResult();
            }
            SolrQuery solrQuery = resolveToSolrQuery(context, discoveryQuery, includeUnDiscoverable);


            QueryResponse queryResponse = getSolr().query(solrQuery, SolrRequest.METHOD.POST);
            return retrieveResult(context, discoveryQuery, queryResponse);

        } catch (Exception e)
        {
            throw new org.dspace.discovery.SearchServiceException(e.getMessage(),e);
        }
    }

    protected SolrQuery resolveToSolrQuery(Context context, DiscoverQuery discoveryQuery, boolean includeUnDiscoverable)
    {
        SolrQuery solrQuery = new SolrQuery();

        String query = "*:*";
        if(discoveryQuery.getQuery() != null)
        {
        	query = discoveryQuery.getQuery();
		}

        solrQuery.setQuery(query);

        // Add any search fields to our query. This is the limited list
        // of fields that will be returned in the solr result
        for(String fieldName : discoveryQuery.getSearchFields())
        {
            solrQuery.addField(fieldName);
        }
        // Also ensure a few key obj identifier fields are returned with every query
        solrQuery.addField(HANDLE_FIELD);
        solrQuery.addField(RESOURCE_TYPE_FIELD);
        solrQuery.addField(RESOURCE_ID_FIELD);

        if(discoveryQuery.isSpellCheck())
        {
            solrQuery.setParam(SpellingParams.SPELLCHECK_Q, query);
            solrQuery.setParam(SpellingParams.SPELLCHECK_COLLATE, Boolean.TRUE);
            solrQuery.setParam("spellcheck", Boolean.TRUE);
        }

        if (!includeUnDiscoverable)
        {
        	solrQuery.addFilterQuery("NOT(withdrawn:true)");
        	solrQuery.addFilterQuery("NOT(discoverable:false)");
		}

        for (int i = 0; i < discoveryQuery.getFilterQueries().size(); i++)
        {
            String filterQuery = discoveryQuery.getFilterQueries().get(i);
            solrQuery.addFilterQuery(filterQuery);
        }
        if(discoveryQuery.getDSpaceObjectFilter() != -1)
        {
            solrQuery.addFilterQuery(RESOURCE_TYPE_FIELD + ":" + discoveryQuery.getDSpaceObjectFilter());
        }

        for (int i = 0; i < discoveryQuery.getFieldPresentQueries().size(); i++)
        {
            String filterQuery = discoveryQuery.getFieldPresentQueries().get(i);
            solrQuery.addFilterQuery(filterQuery + ":[* TO *]");
        }

        if(discoveryQuery.getStart() != -1)
        {
            solrQuery.setStart(discoveryQuery.getStart());
        }

        if(discoveryQuery.getMaxResults() != -1)
        {
            solrQuery.setRows(discoveryQuery.getMaxResults());
        }

        if(discoveryQuery.getSortField() != null)
        {
            SolrQuery.ORDER order = SolrQuery.ORDER.asc;
            if(discoveryQuery.getSortOrder().equals(DiscoverQuery.SORT_ORDER.desc))
                order = SolrQuery.ORDER.desc;

            solrQuery.addSortField(discoveryQuery.getSortField(), order);
        }

        for(String property : discoveryQuery.getProperties().keySet())
        {
            List<String> values = discoveryQuery.getProperties().get(property);
            solrQuery.add(property, values.toArray(new String[values.size()]));
        }

        List<DiscoverFacetField> facetFields = discoveryQuery.getFacetFields();
        if(0 < facetFields.size())
        {
            //Only add facet information if there are any facets
            for (DiscoverFacetField facetFieldConfig : facetFields)
            {
                String field = transformFacetField(facetFieldConfig, facetFieldConfig.getField(), false);
                solrQuery.addFacetField(field);

                // Setting the facet limit in this fashion ensures that each facet can have its own max
                solrQuery.add("f." + field + "." + FacetParams.FACET_LIMIT, String.valueOf(facetFieldConfig.getLimit()));
                String facetSort;
                if(DiscoveryConfigurationParameters.SORT.COUNT.equals(facetFieldConfig.getSortOrder()))
                {
                    facetSort = FacetParams.FACET_SORT_COUNT;
                }else{
                    facetSort = FacetParams.FACET_SORT_INDEX;
                }
                solrQuery.add("f." + field + "." + FacetParams.FACET_SORT, facetSort);
                if (facetFieldConfig.getOffset() != -1)
                {
                    solrQuery.setParam("f." + field + "."
                            + FacetParams.FACET_OFFSET,
                            String.valueOf(facetFieldConfig.getOffset()));
                }
                if(facetFieldConfig.getPrefix() != null)
                {
                    solrQuery.setFacetPrefix(field, facetFieldConfig.getPrefix());
                }
            }

            List<String> facetQueries = discoveryQuery.getFacetQueries();
            for (String facetQuery : facetQueries)
            {
                solrQuery.addFacetQuery(facetQuery);
            }

            if(discoveryQuery.getFacetMinCount() != -1)
            {
                solrQuery.setFacetMinCount(discoveryQuery.getFacetMinCount());
            }

            solrQuery.setParam(FacetParams.FACET_OFFSET, String.valueOf(discoveryQuery.getFacetOffset()));
        }

        if(0 < discoveryQuery.getHitHighlightingFields().size())
        {
            solrQuery.setHighlight(true);
            solrQuery.add(HighlightParams.USE_PHRASE_HIGHLIGHTER, Boolean.TRUE.toString());
            for (DiscoverHitHighlightingField highlightingField : discoveryQuery.getHitHighlightingFields())
            {
                solrQuery.addHighlightField(highlightingField.getField() + "_hl");
                solrQuery.add("f." + highlightingField.getField() + "_hl." + HighlightParams.FRAGSIZE, String.valueOf(highlightingField.getMaxChars()));
                solrQuery.add("f." + highlightingField.getField() + "_hl." + HighlightParams.SNIPPETS, String.valueOf(highlightingField.getMaxSnippets()));
            }

        }

        //Add any configured search plugins !
        List<SolrServiceSearchPlugin> solrServiceSearchPlugins = DSpaceServicesFactory.getInstance().getServiceManager().getServicesByType(SolrServiceSearchPlugin.class);
        for (SolrServiceSearchPlugin searchPlugin : solrServiceSearchPlugins)
        {
            searchPlugin.additionalSearchParameters(context, discoveryQuery, solrQuery);
        }
        return solrQuery;
    }

    @Override
    public InputStream searchJSON(Context context, DiscoverQuery query, DSpaceObject dso, String jsonIdentifier) throws SearchServiceException {
        if(dso != null)
        {
            if (dso instanceof Community)
            {
                query.addFilterQueries("location:m" + dso.getID());
            } else if (dso instanceof Collection)
            {
                query.addFilterQueries("location:l" + dso.getID());
            } else if (dso instanceof Item)
            {
                query.addFilterQueries(HANDLE_FIELD + ":" + dso.getHandle());
            }
        }
        return searchJSON(context, query, jsonIdentifier);
    }


    @Override
    public InputStream searchJSON(Context context, DiscoverQuery discoveryQuery, String jsonIdentifier) throws SearchServiceException {
        if(getSolr() == null)
        {
            return null;
        }

        SolrQuery solrQuery = resolveToSolrQuery(context, discoveryQuery, false);
        //We use json as out output type
        solrQuery.setParam("json.nl", "map");
        solrQuery.setParam("json.wrf", jsonIdentifier);
        solrQuery.setParam(CommonParams.WT, "json");

        StringBuilder urlBuilder = new StringBuilder();
        //urlBuilder.append(getSolr().getBaseURL()).append("/select?");
        //urlBuilder.append(solrQuery.toString());

        // New url without any query params appended
        urlBuilder.append(getSolr().getBaseURL()).append("/select");

        // Post setup
        NamedList<Object> solrParameters = solrQuery.toNamedList();
        List<NameValuePair> postParameters = new ArrayList<>();
        for (Map.Entry<String, Object> solrParameter : solrParameters) {
            if (solrParameter.getValue() instanceof String[]) {
                // Multi-valued solr parameter
                for(String val : (String[])solrParameter.getValue()) {
                    postParameters.add(new BasicNameValuePair(solrParameter.getKey(), val));
                }

            }
            else if(solrParameter.getValue() instanceof String) {
                postParameters.add(new BasicNameValuePair(solrParameter.getKey(), solrParameter.getValue().toString()));
            }
            else {
                log.warn("Search parameters contain non-string value: " + solrParameter.getValue().toString());
            }
        }

        try {
            HttpPost post = new HttpPost(urlBuilder.toString());
            post.setEntity(new UrlEncodedFormEntity(postParameters));
            HttpResponse response = new DefaultHttpClient().execute(post);
            return response.getEntity().getContent();

        } catch (Exception e)
        {
            log.error("Error while getting json solr result for discovery search recommendation", e);
        }
        return null;
    }

    protected DiscoverResult retrieveResult(Context context, DiscoverQuery query, QueryResponse solrQueryResponse) throws SQLException {
        DiscoverResult result = new DiscoverResult();

        if(solrQueryResponse != null)
        {
            result.setSearchTime(solrQueryResponse.getQTime());
            result.setStart(query.getStart());
            result.setMaxResults(query.getMaxResults());
            result.setTotalSearchResults(solrQueryResponse.getResults().getNumFound());

            List<String> searchFields = query.getSearchFields();
            for (SolrDocument doc : solrQueryResponse.getResults())
            {
                DSpaceObject dso = findDSpaceObject(context, doc);

                if(dso != null)
                {
                    result.addDSpaceObject(dso);
                } else {
                    log.error(LogManager.getHeader(context, "Error while retrieving DSpace object from discovery index", "Handle: " + doc.getFirstValue(HANDLE_FIELD)));
                    continue;
                }

                DiscoverResult.SearchDocument resultDoc = new DiscoverResult.SearchDocument();
                //Add information about our search fields
                for (String field : searchFields)
                {
                    List<String> valuesAsString = new ArrayList<String>();
                    for (Object o : doc.getFieldValues(field))
                    {
                        valuesAsString.add(String.valueOf(o));
                    }
                    resultDoc.addSearchField(field, valuesAsString.toArray(new String[valuesAsString.size()]));
                }
                result.addSearchDocument(dso, resultDoc);

                if(solrQueryResponse.getHighlighting() != null)
                {
                    Map<String, List<String>> highlightedFields = solrQueryResponse.getHighlighting().get(dso.getType() + "-" + dso.getID());
                    if(MapUtils.isNotEmpty(highlightedFields))
                    {
                        //We need to remove all the "_hl" appendix strings from our keys
                        Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
                        for(String key : highlightedFields.keySet())
                        {
                            resultMap.put(key.substring(0, key.lastIndexOf("_hl")), highlightedFields.get(key));
                        }

                        result.addHighlightedResult(dso, new DiscoverResult.DSpaceObjectHighlightResult(dso, resultMap));
                    }
                }
            }

            //Resolve our facet field values
            List<FacetField> facetFields = solrQueryResponse.getFacetFields();
            if(facetFields != null)
            {
                for (int i = 0; i <  facetFields.size(); i++)
                {
                    FacetField facetField = facetFields.get(i);
                    DiscoverFacetField facetFieldConfig = query.getFacetFields().get(i);
                    List<FacetField.Count> facetValues = facetField.getValues();
                    if (facetValues != null)
                    {
                        if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE) && facetFieldConfig.getSortOrder().equals(DiscoveryConfigurationParameters.SORT.VALUE))
                        {
                            //If we have a date & are sorting by value, ensure that the results are flipped for a proper result
                           Collections.reverse(facetValues);
                        }

                        for (FacetField.Count facetValue : facetValues)
                        {
                            String displayedValue = transformDisplayedValue(context, facetField.getName(), facetValue.getName());
                            String field = transformFacetField(facetFieldConfig, facetField.getName(), true);
                            String authorityValue = transformAuthorityValue(context, facetField.getName(), facetValue.getName());
                            String sortValue = transformSortValue(context, facetField.getName(), facetValue.getName());
                            String filterValue = displayedValue;
                            if (StringUtils.isNotBlank(authorityValue))
                            {
                                filterValue = authorityValue;
                            }
                            result.addFacetResult(
                                    field,
                                    new DiscoverResult.FacetResult(filterValue,
                                            displayedValue, authorityValue,
                                            sortValue, facetValue.getCount()));
                        }
                    }
                }
            }

            if(solrQueryResponse.getFacetQuery() != null)
            {
				// just retrieve the facets in the order they where requested!
				// also for the date we ask it in proper (reverse) order
				// At the moment facet queries are only used for dates
                LinkedHashMap<String, Integer> sortedFacetQueries = new LinkedHashMap<String, Integer>(solrQueryResponse.getFacetQuery());
                for(String facetQuery : sortedFacetQueries.keySet())
                {
                    //TODO: do not assume this, people may want to use it for other ends, use a regex to make sure
                    //We have a facet query, the values looks something like: dateissued.year:[1990 TO 2000] AND -2000
                    //Prepare the string from {facet.field.name}:[startyear TO endyear] to startyear - endyear
                    String facetField = facetQuery.substring(0, facetQuery.indexOf(":"));
                    String name = "";
                    String filter = "";
                    if (facetQuery.indexOf('[') > -1 && facetQuery.lastIndexOf(']') > -1)
                    {
                        name = facetQuery.substring(facetQuery.indexOf('[') + 1);
                        name = name.substring(0, name.lastIndexOf(']')).replaceAll("TO", "-");
                        filter = facetQuery.substring(facetQuery.indexOf('['));
                        filter = filter.substring(0, filter.lastIndexOf(']') + 1);
                    }

                    Integer count = sortedFacetQueries.get(facetQuery);

                    //No need to show empty years
                    if(0 < count)
                    {
                        result.addFacetResult(facetField, new DiscoverResult.FacetResult(filter, name, null, name, count));
                    }
                }
            }

            if(solrQueryResponse.getSpellCheckResponse() != null)
            {
                String recommendedQuery = solrQueryResponse.getSpellCheckResponse().getCollatedResult();
                if(StringUtils.isNotBlank(recommendedQuery))
                {
                    result.setSpellCheckQuery(recommendedQuery);
                }
            }
        }

        return result;
    }

    protected DSpaceObject findDSpaceObject(Context context, SolrDocument doc) throws SQLException {

        Integer type = (Integer) doc.getFirstValue(RESOURCE_TYPE_FIELD);
        UUID id = UUID.fromString((String) doc.getFirstValue(RESOURCE_ID_FIELD));
        String handle = (String) doc.getFirstValue(HANDLE_FIELD);

        if (type != null && id != null)
        {
            return contentServiceFactory.getDSpaceObjectService(type).find(context, id);
        } else if (handle != null)
        {
            return handleService.resolveToObject(context, handle);
        }

        return null;
    }


    /** Simple means to return the search result as an InputStream */
    public java.io.InputStream searchAsInputStream(DiscoverQuery query) throws SearchServiceException, java.io.IOException {
        if(getSolr() == null)
        {
            return null;
        }
        HttpHost hostURL = (HttpHost)(getSolr().getHttpClient().getParams().getParameter(ClientPNames.DEFAULT_HOST));

        HttpPost post = new HttpPost(hostURL.toHostString());
        List<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair("q",query.toString()));
        post.setEntity(new UrlEncodedFormEntity(postParameters));

        HttpResponse response = getSolr().getHttpClient().execute(post);

        return response.getEntity().getContent();
    }

    public List<DSpaceObject> search(Context context, String query, int offset, int max, String... filterquery)
    {
        return search(context, query, null, true, offset, max, filterquery);
    }

    @Override
    public List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, int offset, int max, String... filterquery)
    {

        try {
            if(getSolr() == null)
            {
                return Collections.emptyList();
            }

            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            //Only return obj identifier fields in result doc
            solrQuery.setFields(RESOURCE_ID_FIELD, RESOURCE_TYPE_FIELD);
            solrQuery.setStart(offset);
            solrQuery.setRows(max);
            if (orderfield != null)
            {
                solrQuery.setSortField(orderfield, ascending ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
            }
            if (filterquery != null)
            {
                solrQuery.addFilterQuery(filterquery);
            }
            QueryResponse rsp = getSolr().query(solrQuery, SolrRequest.METHOD.POST);
            SolrDocumentList docs = rsp.getResults();

            Iterator iter = docs.iterator();
            List<DSpaceObject> result = new ArrayList<DSpaceObject>();
            while (iter.hasNext())
            {
                SolrDocument doc = (SolrDocument) iter.next();

                DSpaceObject o = contentServiceFactory.getDSpaceObjectService((Integer) doc.getFirstValue(RESOURCE_TYPE_FIELD)).find(context, UUID.fromString((String) doc.getFirstValue(RESOURCE_ID_FIELD)));

                if (o != null)
                {
                    result.add(o);
                }
            }
            return result;
		} catch (Exception e)
        {
			// Any acception that we get ignore it.
			// We do NOT want any crashed to shown by the user
            log.error(LogManager.getHeader(context, "Error while quering solr", "Queyr: " + query), e);
            return new ArrayList<DSpaceObject>(0);
		}
    }

    @Override
    public DiscoverFilterQuery toFilterQuery(Context context, String field, String operator, String value) throws SQLException{
        DiscoverFilterQuery result = new DiscoverFilterQuery();

        StringBuilder filterQuery = new StringBuilder();
        if(StringUtils.isNotBlank(field) && StringUtils.isNotBlank(value))
        {
            filterQuery.append(field);
            if("equals".equals(operator))
            {
                //Query the keyword indexed field !
                filterQuery.append("_keyword");
            }
            else if ("authority".equals(operator))
            {
                //Query the authority indexed field !
                filterQuery.append("_authority");
            }
            else if ("notequals".equals(operator)
                    || "notcontains".equals(operator)
                    || "notauthority".equals(operator))
            {
                filterQuery.insert(0, "-");
            }
            filterQuery.append(":");
            if("equals".equals(operator) || "notequals".equals(operator))
            {
                //DO NOT ESCAPE RANGE QUERIES !
                if(!value.matches("\\[.*TO.*\\]"))
                {
                    value = ClientUtils.escapeQueryChars(value);
                    filterQuery.append(value);
                }
                else
                {
                	if (value.matches("\\[\\d{1,4} TO \\d{1,4}\\]"))
                	{
                		int minRange = Integer.parseInt(value.substring(1, value.length()-1).split(" TO ")[0]);
                		int maxRange = Integer.parseInt(value.substring(1, value.length()-1).split(" TO ")[1]);
                		value = "["+String.format("%04d", minRange) + " TO "+ String.format("%04d", maxRange) + "]";
                	}
                	filterQuery.append(value);
                }
            }
            else{
                //DO NOT ESCAPE RANGE QUERIES !
                if(!value.matches("\\[.*TO.*\\]"))
                {
                    value = ClientUtils.escapeQueryChars(value);
                    filterQuery.append("(").append(value).append(")");
                }
                else
                {
                    filterQuery.append(value);
                }
            }

            result.setDisplayedValue(transformDisplayedValue(context, field, value));
        }

        result.setFilterQuery(filterQuery.toString());
        return result;
    }

    @Override
    public List<Item> getRelatedItems(Context context, Item item, DiscoveryMoreLikeThisConfiguration mltConfig)
    {
        List<Item> results = new ArrayList<Item>();
        try{
            SolrQuery solrQuery = new SolrQuery();
            //Set the query to handle since this is unique
            solrQuery.setQuery(HANDLE_FIELD + ":" + item.getHandle());
            //Only return obj identifier fields in result doc
            solrQuery.setFields(HANDLE_FIELD, RESOURCE_TYPE_FIELD, RESOURCE_ID_FIELD);
            //Add the more like this parameters !
            solrQuery.setParam(MoreLikeThisParams.MLT, true);
            //Add a comma separated list of the similar fields
            @SuppressWarnings("unchecked")
            java.util.Collection<String> similarityMetadataFields = CollectionUtils.collect(mltConfig.getSimilarityMetadataFields(), new Transformer()
            {
                @Override
                public Object transform(Object input)
                {
                    //Add the mlt appendix !
                    return input + "_mlt";
                }
            });

            solrQuery.setParam(MoreLikeThisParams.SIMILARITY_FIELDS, StringUtils.join(similarityMetadataFields, ','));
            solrQuery.setParam(MoreLikeThisParams.MIN_TERM_FREQ, String.valueOf(mltConfig.getMinTermFrequency()));
            solrQuery.setParam(MoreLikeThisParams.DOC_COUNT, String.valueOf(mltConfig.getMax()));
            solrQuery.setParam(MoreLikeThisParams.MIN_WORD_LEN, String.valueOf(mltConfig.getMinWordLength()));

            if(getSolr() == null)
            {
                return Collections.emptyList();
            }
            QueryResponse rsp = getSolr().query(solrQuery, SolrRequest.METHOD.POST);
            NamedList mltResults = (NamedList) rsp.getResponse().get("moreLikeThis");
            if(mltResults != null && mltResults.get(item.getType() + "-" + item.getID()) != null)
            {
                SolrDocumentList relatedDocs = (SolrDocumentList) mltResults.get(item.getType() + "-" + item.getID());
                for (Object relatedDoc : relatedDocs)
                {
                    SolrDocument relatedDocument = (SolrDocument) relatedDoc;
                    DSpaceObject relatedItem = findDSpaceObject(context, relatedDocument);
                    if (relatedItem.getType() == Constants.ITEM)
                    {
                        results.add((Item) relatedItem);
                    }
                }
            }


        } catch (Exception e)
        {
            log.error(LogManager.getHeader(context, "Error while retrieving related items", "Handle: " + item.getHandle()), e);
        }
        return results;
    }

    @Override
    public String toSortFieldIndex(String metadataField, String type)
    {
        if(type.equals(DiscoveryConfigurationParameters.TYPE_DATE))
        {
            return metadataField + "_dt";
        }else{
            return metadataField + "_sort";
        }
    }

    protected String transformFacetField(DiscoverFacetField facetFieldConfig, String field, boolean removePostfix)
    {
        if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf("_filter"));
            }else{
                return field + "_filter";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf(".year"));
            }else{
                return field + ".year";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_AC))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf("_ac"));
            }else{
                return field + "_ac";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL))
        {
            if(removePostfix)
            {
                return StringUtils.substringBeforeLast(field, "_tax_");
            }else{
                //Only display top level filters !
                return field + "_tax_0_filter";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_AUTHORITY))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf("_acid"));
            }else{
                return field + "_acid";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_STANDARD))
        {
            return field;
        }else{
            return field;
        }
    }

    protected String transformDisplayedValue(Context context, String field, String value) throws SQLException {
        if(field.equals("location.comm") || field.equals("location.coll"))
        {
            value = locationToName(context, field, value);
        }
        else if (field.endsWith("_filter") || field.endsWith("_ac")
          || field.endsWith("_acid"))
        {
            //We have a filter make sure we split !
            String separator = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("discovery.solr.facets.split.char");
            if(separator == null)
            {
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer valueBuffer = new StringBuffer();
            int start = fqParts.length / 2;
            for(int i = start; i < fqParts.length; i++)
            {
                String[] split = fqParts[i].split(AUTHORITY_SEPARATOR, 2);
                valueBuffer.append(split[0]);
            }
            value = valueBuffer.toString();
        }else if(value.matches("\\((.*?)\\)"))
        {
            //The brackets where added for better solr results, remove the first & last one
            value = value.substring(1, value.length() -1);
        }
        return value;
    }

    protected String transformAuthorityValue(Context context, String field, String value) throws SQLException {
    	if(field.equals("location.comm") || field.equals("location.coll"))
    	{
            return value;
    	}
    	if (field.endsWith("_filter") || field.endsWith("_ac")
                || field.endsWith("_acid"))
        {
            //We have a filter make sure we split !
            String separator = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("discovery.solr.facets.split.char");
            if(separator == null)
            {
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer authorityBuffer = new StringBuffer();
            int start = fqParts.length / 2;
            for(int i = start; i < fqParts.length; i++)
            {
                String[] split = fqParts[i].split(AUTHORITY_SEPARATOR, 2);
                if (split.length == 2)
                {
                    authorityBuffer.append(split[1]);
                }
            }
            if (authorityBuffer.length() > 0)
            {
                return authorityBuffer.toString();
            }
        }
        return null;
    }

    protected String transformSortValue(Context context, String field, String value) throws SQLException {
        if(field.equals("location.comm") || field.equals("location.coll"))
        {
            value = locationToName(context, field, value);
        }
        else if (field.endsWith("_filter") || field.endsWith("_ac")
                || field.endsWith("_acid"))
        {
            //We have a filter make sure we split !
            String separator = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("discovery.solr.facets.split.char");
            if(separator == null)
            {
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer valueBuffer = new StringBuffer();
            int end = fqParts.length / 2;
            for(int i = 0; i < end; i++)
            {
                valueBuffer.append(fqParts[i]);
            }
            value = valueBuffer.toString();
        }else if(value.matches("\\((.*?)\\)"))
        {
            //The brackets where added for better solr results, remove the first & last one
            value = value.substring(1, value.length() -1);
        }
        return value;
    }

	@Override
	public void indexContent(Context context, DSpaceObject dso, boolean force,
			boolean commit) throws SearchServiceException, SQLException {
		indexContent(context, dso, force);
		if (commit)
		{
			commit();
		}
	}

	@Override
	public void commit() throws SearchServiceException {
		try {
            if(getSolr() != null)
            {
                getSolr().commit();
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
}
