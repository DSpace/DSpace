/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.*;
import org.dspace.discovery.configuration.*;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SolrIndexer contains the methods that index Items and their metadata,
 * collections, communities, etc. It is meant to either be invoked from the
 * command line (see dspace/bin/index-all) or via the indexContent() methods
 * within DSpace.
 * <p/>
 * The Administrator can choose to run SolrIndexer in a cron that repeats
 * regularly, a failed attempt to index from the UI will be "caught" up on in
 * that cron.
 *
 * The SolrServiceImple is registered as a Service in the ServiceManager via
 * A spring configuration file located under
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

    private static final String LAST_INDEXED_FIELD = "SolrIndexer.lastIndexed";

    public static final String FILTER_SEPARATOR = "|||";

    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    private CommonsHttpSolrServer solr = null;

    /**
     * Non-Static Singelton instance of Configuration Service
     */
//    private ConfigurationService configurationService;


//    @Autowired
//    @Required
//    public void setConfigurationService(ConfigurationService configurationService) {
//        this.configurationService = configurationService;
//    }

    protected CommonsHttpSolrServer getSolr() throws java.net.MalformedURLException, org.apache.solr.client.solrj.SolrServerException
    {
        if ( solr == null)
        {
           String solrService = new DSpace().getConfigurationService().getProperty("discovery.search.server") ;

            log.debug("Solr URL: " + solrService);
                    solr = new CommonsHttpSolrServer(solrService);

            solr.setBaseURL(solrService);

            SolrQuery solrQuery = new SolrQuery()
                        .setQuery("search.resourcetype:2 AND search.resourceid:1");

            solr.query(solrQuery);
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
     * @throws SQLException
     * @throws IOException
     */
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
     * @throws SQLException
     * @throws IOException
     */
    public void indexContent(Context context, DSpaceObject dso,
                             boolean force) throws SQLException {

        String handle = dso.getHandle();

        if (handle == null) {
            handle = HandleManager.findHandle(context, dso);
        }

        try {
            switch (dso.getType()) {
                case Constants.ITEM:
                    Item item = (Item) dso;
                    if (item.isArchived() && !item.isWithdrawn()) {
                        /**
                         * If the item is in the repository now, add it to the index
                         */
                        if (requiresIndexing(handle, ((Item) dso).getLastModified())
                                || force) {
                            unIndexContent(context, handle);
                            buildDocument(context, (Item) dso);
                        }
                    } else {
                        /**
                         * Make sure the item is not in the index if it is not in
                         * archive. TODO: Someday DSIndexer should block withdrawn
                         * content on search/retrieval and allow admins the ability
                         * to still search for withdrawn Items.
                         */
                        unIndexContent(context, handle);
                        log.info("Removed Item: " + handle + " from Index");
                    }
                    break;

                case Constants.COLLECTION:
                    buildDocument((Collection) dso);
                    log.info("Wrote Collection: " + handle + " to Index");
                    break;

                case Constants.COMMUNITY:
                    buildDocument((Community) dso);
                    log.info("Wrote Community: " + handle + " to Index");
                    break;

                default:
                    log
                            .error("Only Items, Collections and Communities can be Indexed");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * unIndex removes an Item, Collection, or Community only works if the
     * DSpaceObject has a handle (uses the handle for its unique ID)
     *
     * @param context
     * @param dso     DSpace Object, can be Community, Item, or Collection
     * @throws SQLException
     * @throws IOException
     */
    public void unIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException {
        try {
            unIndexContent(context, dso.getHandle());
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            emailException(exception);
        }
    }

    /**
     * Unindex a Document in the Lucene index.
     * @param context the dspace context
     * @param handle the handle of the object to be deleted
     * @throws IOException
     * @throws SQLException
     */
    public void unIndexContent(Context context, String handle) throws IOException, SQLException {
        unIndexContent(context, handle, false);
    }

    /**
     * Unindex a Document in the Lucene Index.
     * @param context the dspace context
     * @param handle the handle of the object to be deleted
     * @throws SQLException
     * @throws IOException
     */
    public void unIndexContent(Context context, String handle, boolean commit)
            throws SQLException, IOException {

        try {
            getSolr().deleteById(handle);
            if(commit)
            {
                getSolr().commit();
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
    public void reIndexContent(Context context, DSpaceObject dso)
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
    public void updateIndex(Context context) {
        updateIndex(context, false);
    }

    /**
     * Iterates over all Items, Collections and Communities. And updates them in
     * the index. Uses decaching to control memory footprint. Uses indexContent
     * and isStale to check state of item in index.
     * <p/>
     * At first it may appear counterintuitive to have an IndexWriter/Reader
     * opened and closed on each DSO. But this allows the UI processes to step
     * in and attain a lock and write to the index even if other processes/jvms
     * are running a reindex.
     *
     * @param context the dspace context
     * @param force whether or not to force the reindexing
     */
    public void updateIndex(Context context, boolean force) {
        try {
            ItemIterator items = null;
            try {
                for (items = Item.findAll(context); items.hasNext();) {
                    Item item = items.next();
                    indexContent(context, item, force);
                    item.decache();
                }
            } finally {
                if (items != null)
                {
                    items.close();
                }
            }

            Collection[] collections = Collection.findAll(context);
            for (Collection collection : collections) {
                indexContent(context, collection, force);
                context.removeCached(collection, collection.getID());

            }

            Community[] communities = Community.findAll(context);
            for (Community community : communities) {
                indexContent(context, community, force);
                context.removeCached(community, community.getID());
            }

            getSolr().commit();

        } catch (Exception e) {
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
    public void cleanIndex(boolean force) throws IOException,
            SQLException, SearchServiceException {

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try
        {
            if (force) {
                getSolr().deleteByQuery("*:*");
            } else {
                SolrQuery query = new SolrQuery();
                query.setQuery("*:*");
                QueryResponse rsp = getSolr().query(query);
                SolrDocumentList docs = rsp.getResults();

                Iterator iter = docs.iterator();
                while (iter.hasNext()) {

                 SolrDocument doc = (SolrDocument) iter.next();

                String handle = (String) doc.getFieldValue("handle");

                DSpaceObject o = HandleManager.resolveToObject(context, handle);

                if (o == null) {
                    log.info("Deleting: " + handle);
                    /*
                          * Use IndexWriter to delete, its easier to manage
                          * write.lock
                          */
                    unIndexContent(context, handle);
                } else {
                    context.removeCached(o, o.getID());
                    log.debug("Keeping: " + handle);
                }
            }
            }
        } catch(Exception e){

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
    public void optimize() {
        try {
            long start = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Started:"+start);
            getSolr().optimize();
            long finish = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Finished:"+finish);
            System.out.println("SOLR Search Optimize -- Total time taken:"+(finish-start) + " (ms).");
        } catch (SolrServerException sse) {
            System.err.println(sse.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    // //////////////////////////////////
    // Private
    // //////////////////////////////////

    private void emailException(Exception exception) {
        // Also email an alert, system admin may need to check for stale lock
        try {
            String recipient = ConfigurationManager
                    .getProperty("alert.recipient");

            if (recipient != null) {
                Email email = ConfigurationManager
                        .getEmail(I18nUtil.getEmailFilename(
                                Locale.getDefault(), "internal_error"));
                email.addRecipient(recipient);
                email.addArgument(ConfigurationManager
                        .getProperty("dspace.url"));
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
     * @param handle the handle of the dso
     * @param lastModified the last modified date of the DSpace object
     * @return a boolean indicating if the dso should be re indexed again
     * @throws SQLException sql exception
     * @throws IOException io exception
     * @throws SearchServiceException if something went wrong with querying the solr server
     */
    private boolean requiresIndexing(String handle, Date lastModified)
            throws SQLException, IOException, SearchServiceException {

        boolean reindexItem = false;
        boolean inIndex = false;

        SolrQuery query = new SolrQuery();
        query.setQuery("handle:" + handle);
        QueryResponse rsp;

        try {
            rsp = getSolr().query(query);
        } catch (SolrServerException e) {
            throw new SearchServiceException(e.getMessage(),e);
        }

        for (SolrDocument doc : rsp.getResults()) {

            inIndex = true;

            Object value = doc.getFieldValue(LAST_INDEXED_FIELD);

            if(value instanceof Date)
            {
                Date lastIndexed = (Date) value;

                if (lastIndexed.before(lastModified)) {

                    reindexItem = true;
                }
            }
        }

        return reindexItem || !inIndex;
    }


    /**
     * @param myitem the item for which our locations are to be retrieved
     * @return a list containing the identifiers of the communities & collections
     * @throws SQLException sql exception
     */
    private List<String> getItemLocations(Item myitem)
            throws SQLException {
        List<String> locations = new Vector<String>();

        // build list of community ids
        Community[] communities = myitem.getCommunities();

        // build list of collection ids
        Collection[] collections = myitem.getCollections();

        // now put those into strings
        int i = 0;

        for (i = 0; i < communities.length; i++)
        {
            locations.add("m" + communities[i].getID());
        }

        for (i = 0; i < collections.length; i++)
        {
            locations.add("l" + collections[i].getID());
        }

        return locations;
    }

    private List<String> getCollectionLocations(Collection target) throws SQLException {
        List<String> locations = new Vector<String>();
        // build list of community ids
        Community[] communities = target.getCommunities();

        // now put those into strings
        for (Community community : communities) {
            locations.add("m" + community.getID());
        }

        return locations;
    }

    /**
     * Write the document to the index under the appropriate handle.
     * @param doc the solr document to be written to the server
     * @throws IOException IO exception
     */
    private void writeDocument(SolrInputDocument doc) throws IOException {

        try {
            getSolr().add(doc);
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Build a Lucene document for a DSpace Community.
     *
     * @param community Community to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private void buildDocument(Community community)
    throws SQLException, IOException {
        // Create Document
        SolrInputDocument doc = buildDocument(Constants.COMMUNITY, community.getID(),
                community.getHandle(), null);

        // and populate it
        String name = community.getMetadata("name");

        if (name != null) {
            doc.addField("name", name);
        }

        //Do any additional indexing, depends on the plugins
        List<SolrServiceIndexPlugin> solrServiceIndexPlugins = new DSpace().getServiceManager().getServicesByType(SolrServiceIndexPlugin.class);
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins) {
            solrServiceIndexPlugin.additionalIndex(community, doc);
        }

        writeDocument(doc);
    }

    /**
     * Build a Lucene document for a DSpace Collection.
     *
     * @param collection Collection to be indexed
     * @throws SQLException sql exception
     * @throws IOException IO exception
     */
    private void buildDocument(Collection collection)
    throws SQLException, IOException {
        List<String> locations = getCollectionLocations(collection);

        // Create Lucene Document
        SolrInputDocument doc = buildDocument(Constants.COLLECTION, collection.getID(),
                collection.getHandle(), locations);

        // and populate it
        String name = collection.getMetadata("name");

        if (name != null) {
            doc.addField("name", name);
        }

        //Do any additional indexing, depends on the plugins
        List<SolrServiceIndexPlugin> solrServiceIndexPlugins = new DSpace().getServiceManager().getServicesByType(SolrServiceIndexPlugin.class);
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins) {
            solrServiceIndexPlugin.additionalIndex(collection, doc);
        }

        writeDocument(doc);
    }

    /**
     * Build a Lucene document for a DSpace Item and write the index
     *
     * @param context Users Context
     * @param item    The DSpace Item to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private void buildDocument(Context context, Item item)
            throws SQLException, IOException {
        String handle = item.getHandle();

        if (handle == null) {
            handle = HandleManager.findHandle(context, item);
        }

        // get the location string (for searching by collection & community)
        List<String> locations = getItemLocations(item);

        SolrInputDocument doc = buildDocument(Constants.ITEM, item.getID(), handle,
                locations);

        log.debug("Building Item: " + handle);

        //Keep a list of our sort values which we added, sort values can only be added once
        List<String> sortFieldsAdded = new ArrayList<String>();
        try {
            List<DiscoveryConfiguration> discoveryConfigurations = SearchUtils.getAllDiscoveryConfigurations(item);

            //A map used to save each sidebarFacet config by the metadata fields
            Map<String, List<SidebarFacetConfiguration>> sidebarFacets = new HashMap<String, List<SidebarFacetConfiguration>>();
            Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<String, List<DiscoverySearchFilter>>();
            Map<String, DiscoverySortFieldConfiguration> sortFields = new HashMap<String, DiscoverySortFieldConfiguration>();
            Map<String, DiscoveryRecentSubmissionsConfiguration> recentSubmissionsConfigurationMap = new HashMap<String, DiscoveryRecentSubmissionsConfiguration>();
            for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations) {
                //Sidebar facet mapping configuration read in
                for(SidebarFacetConfiguration facet : discoveryConfiguration.getSidebarFacets()){
                    for (int i = 0; i < facet.getMetadataFields().size(); i++) {
                        String metadataField = facet.getMetadataFields().get(i);
                        List<SidebarFacetConfiguration> resultingList;
                        if(sidebarFacets.get(metadataField) != null){
                            resultingList = sidebarFacets.get(metadataField);
                        }else{
                            //New metadata field, create a new list for it
                            resultingList = new ArrayList<SidebarFacetConfiguration>();
                        }
                        resultingList.add(facet);

                        sidebarFacets.put(metadataField, resultingList);
                    }
                }

                for (int i = 0; i < discoveryConfiguration.getSearchFilters().size(); i++) {
                    DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration.getSearchFilters().get(i);
                    for (int j = 0; j < discoverySearchFilter.getMetadataFields().size(); j++) {
                        String metadataField = discoverySearchFilter.getMetadataFields().get(j);
                        List<DiscoverySearchFilter> resultingList;
                        if(searchFilters.get(metadataField) != null){
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
                if(sortConfiguration != null){
                    for (DiscoverySortFieldConfiguration discoverySortConfiguration : sortConfiguration.getSortFields()) {
                        sortFields.put(discoverySortConfiguration.getMetadataField(), discoverySortConfiguration);
                    }
                }

                DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration = discoveryConfiguration.getRecentSubmissionConfiguration();
                if(recentSubmissionConfiguration != null){
                    recentSubmissionsConfigurationMap.put(recentSubmissionConfiguration.getMetadataSortField(), recentSubmissionConfiguration);
                }
            }

            List<String> toIgnoreFields = new ArrayList<String>();
            String ignoreFieldsString = new DSpace().getConfigurationService().getProperty("discovery.index.ignore");
            if(ignoreFieldsString != null){
                if(ignoreFieldsString.indexOf(",") != -1){
                    for (int i = 0; i < ignoreFieldsString.split(",").length; i++) {
                        toIgnoreFields.add(ignoreFieldsString.split(",")[i].trim());
                    }
                } else {
                    toIgnoreFields.add(ignoreFieldsString);
                }
            }
            DCValue[] mydc = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            for (DCValue meta : mydc){
                String field = meta.schema + "." + meta.element;
                String unqualifiedField = field;

                String value = meta.value;

                if (value == null) {
                    continue;
                }

                if (meta.qualifier != null && !meta.qualifier.trim().equals("")) {
                    field += "." + meta.qualifier;
                }


                //We are not indexing provenance, this is useless
                if (toIgnoreFields.contains(field) || toIgnoreFields.contains(unqualifiedField + "." + Item.ANY)) {
                    continue;
                }

                if ((searchFilters.get(field) != null || searchFilters.get(unqualifiedField + "." + Item.ANY) != null)) {
                    List<DiscoverySearchFilter> searchFilterConfigs = searchFilters.get(field);
                    if(searchFilterConfigs == null){
                        searchFilterConfigs = searchFilters.get(unqualifiedField + "." + Item.ANY);
                    }

                    for (DiscoverySearchFilter searchFilter : searchFilterConfigs) {
                        if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)){
                            //For our search filters that are dates we format them properly
                            Date date = toDate(value);
                            if(date != null){
                                //TODO: make this date format configurable !
                                value = DateFormatUtils.formatUTC(date, "yyyy-MM-dd");
                            }
                        }

                        doc.addField(searchFilter.getIndexFieldName(), value);
                        //Add a dynamic fields for auto complete in search
                        if(searchFilter.isFullAutoComplete()){
                            doc.addField(searchFilter.getIndexFieldName() + "_ac", value);
                        }else{
                            String[] values = value.split(" ");
                            for (String val : values) {
                                doc.addField(searchFilter.getIndexFieldName() + "_ac", val);
                            }
                        }
                    }
                }

                if (sidebarFacets.get(field) != null || sidebarFacets.get(unqualifiedField + "." + Item.ANY) != null) {
                    //Retrieve the configurations
                    List<SidebarFacetConfiguration> facetConfigurations = sidebarFacets.get(field);
                    if(facetConfigurations == null){
                        facetConfigurations = sidebarFacets.get(unqualifiedField + "." + Item.ANY);
                    }

                    for (SidebarFacetConfiguration configuration : facetConfigurations) {
                        if(configuration.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT)){
                            //Add a special filter
                            //We use a separator to split up the lowercase and regular case, this is needed to get our filters in regular case
                            //Solr has issues with facet prefix and cases
                            String separator = new DSpace().getConfigurationService().getProperty("discovery.solr.facets.split.char");
                            if(separator == null){
                                separator = FILTER_SEPARATOR;
                            }
                            doc.addField(configuration.getIndexFieldName() + "_filter", value.toLowerCase() + separator + value);
                        }else
                        if(configuration.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)){
                            //For our sidebar filters that are dates we only add the year
                            Date date = toDate(value);
                            if(date != null){
                                String indexField = configuration.getIndexFieldName() + ".year";
                                doc.addField(indexField, DateFormatUtils.formatUTC(date, "yyyy"));
                                //Also save a sort value of this year, this is required for determining the upper & lower bound year of our facet
                                if(doc.getField(indexField + "_sort") == null){
                                    //We can only add one year so take the first one
                                    doc.addField(indexField + "_sort", DateFormatUtils.formatUTC(date, "yyyy"));
                                }
                            } else {
                                log.warn("Error while indexing sidebar date field, item: " + item.getHandle() + " metadata field: " + field + " date value: " + date);
                            }
                        }
                    }
                }

                if ((sortFields.get(field) != null || recentSubmissionsConfigurationMap.get(field) != null) && !sortFieldsAdded.contains(field)) {
                    //Only add sort value once
                    String type;
                    if(sortFields.get(field) != null){
                        type = sortFields.get(field).getType();
                    }else{
                        type = recentSubmissionsConfigurationMap.get(field).getType();
                    }

                    if(type.equals(DiscoveryConfigurationParameters.TYPE_DATE)){
                        Date date = toDate(value);
                        if(date != null){
                            doc.addField(field + "_dt", date);
                        }else{
                            log.warn("Error while indexing sort date field, item: " + item.getHandle() + " metadata field: " + field + " date value: " + date);
                        }
                    }else{
                        doc.addField(field + "_sort", value);
                    }
                    sortFieldsAdded.add(field);
                }

                doc.addField(field, value.toLowerCase());

                if (meta.language != null && !meta.language.trim().equals("")) {
                    String langField = field + "." + meta.language;
                    doc.addField(langField, value);
                }
            }

        } catch (Exception e)  {
            log.error(e.getMessage(), e);
        }


        log.debug("  Added Metadata");

        try {

            DCValue[] values = item.getMetadata("dc.relation.ispartof");

            if(values != null && values.length > 0 && values[0] != null && values[0].value != null)
            {
                // group on parent
                String handlePrefix = ConfigurationManager.getProperty("handle.canonical.prefix");
                if (handlePrefix == null || handlePrefix.length() == 0)
                {
                    handlePrefix = "http://hdl.handle.net/";
                }
                
                doc.addField("publication_grp",values[0].value.replaceFirst(handlePrefix,"") );
                
            }
            else
            {
                // group on self
                doc.addField("publication_grp", item.getHandle());
            }

        } catch (Exception e){
            log.error(e.getMessage(),e);
        }


        log.debug("  Added Grouping");


        Vector<InputStreamReader> readers = new Vector<InputStreamReader>();

        try {
            // now get full text of any bitstreams in the TEXT bundle
            // trundle through the bundles
            Bundle[] myBundles = item.getBundles();

            for (Bundle myBundle : myBundles) {
                if ((myBundle.getName() != null)
                        && myBundle.getName().equals("TEXT")) {
                    // a-ha! grab the text out of the bitstreams
                    Bitstream[] myBitstreams = myBundle.getBitstreams();

                    for (Bitstream myBitstream : myBitstreams) {
                        try {
                            InputStreamReader is = new InputStreamReader(
                                    myBitstream.retrieve()); // get input
                            readers.add(is);

                            // Add each InputStream to the Indexed Document
                            doc.addField("fulltext", IOUtils.toString(is));

                            log.debug("  Added BitStream: "
                                    + myBitstream.getStoreNumber() + "	"
                                    + myBitstream.getSequenceID() + "   "
                                    + myBitstream.getName());

                        } catch (Exception e) {
                            // this will never happen, but compiler is now
                            // happy.
                            log.trace(e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        }

        //Do any additional indexing, depends on the plugins
        List<SolrServiceIndexPlugin> solrServiceIndexPlugins = new DSpace().getServiceManager().getServicesByType(SolrServiceIndexPlugin.class);
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins) {
            solrServiceIndexPlugin.additionalIndex(item, doc);
        }

        // write the index and close the inputstreamreaders
        try {
            writeDocument(doc);
            log.info("Wrote Item: " + handle + " to Index");
        } catch (RuntimeException e) {
            log.error("Error while writing item to discovery index: " + handle + " message:"+ e.getMessage(), e);
        } finally {
            Iterator<InputStreamReader> itr = readers.iterator();
            while (itr.hasNext()) {
                InputStreamReader reader = itr.next();
                if (reader != null) {
                    reader.close();
                }
            }
            log.debug("closed " + readers.size() + " readers");
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
    private SolrInputDocument buildDocument(int type, int id, String handle,
                                            List<String> locations) {
        SolrInputDocument doc = new SolrInputDocument();

        // want to be able to check when last updated
        // (not tokenized, but it is indexed)
        doc.addField(LAST_INDEXED_FIELD, new Date());

        // New fields to weaken the dependence on handles, and allow for faster
        // list display
        doc.addField("search.resourcetype", Integer.toString(type));

        doc.addField("search.resourceid", Integer.toString(id));

        // want to be able to search for handle, so use keyword
        // (not tokenized, but it is indexed)
        if (handle != null) {
            // want to be able to search for handle, so use keyword
            // (not tokenized, but it is indexed)
            doc.addField("handle", handle);
        }

        if (locations != null) {
            for (String location : locations) {
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
    public static Date toDate(String t) {
        SimpleDateFormat[] dfArr;

        // Choose the likely date formats based on string length
        switch (t.length()) {
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

    public static String locationToName(Context context, String field, String value) throws SQLException {
        if("location.comm".equals(field) || "location.coll".equals(field)){
            int type = field.equals("location.comm") ? Constants.COMMUNITY : Constants.COLLECTION;
            DSpaceObject commColl = DSpaceObject.find(context, type, Integer.parseInt(value));
            if(commColl != null)
            {
                return commColl.getName();
            }

        }
        return value;
    }

    //******** SearchService implementation
    public DiscoverResult search(Context context, DSpaceObject dso, DiscoverQuery discoveryQuery) throws SearchServiceException {
        if(dso != null){
            if (dso instanceof Community) {
                discoveryQuery.addFilterQueries("location:m" + dso.getID());
            } else if (dso instanceof Collection) {
                discoveryQuery.addFilterQueries("location:l" + dso.getID());
            } else if (dso instanceof Item){
                discoveryQuery.addFilterQueries("handle:" + dso.getHandle());
            }
        }
        return search(context, discoveryQuery);

    }


    public DiscoverResult search(Context context, DiscoverQuery discoveryQuery) throws SearchServiceException {
        try {
            SolrQuery solrQuery = new SolrQuery();

            String query = "*:*";
            if(discoveryQuery.getQuery() != null){
                query = discoveryQuery.getQuery();
            }

            solrQuery.setQuery(query);

            for (int i = 0; i < discoveryQuery.getFilterQueries().size(); i++) {
                String filterQuery = discoveryQuery.getFilterQueries().get(i);
                solrQuery.addFilterQuery(filterQuery);
            }
            if(discoveryQuery.getDSpaceObjectFilter() != -1){
                solrQuery.addFilterQuery("search.resourcetype:" + discoveryQuery.getDSpaceObjectFilter());
            }

            for (int i = 0; i < discoveryQuery.getFieldPresentQueries().size(); i++) {
                String filterQuery = discoveryQuery.getFieldPresentQueries().get(i);
                solrQuery.addFilterQuery(filterQuery + ":[* TO *]");
            }

            if(discoveryQuery.getStart() != -1){
                solrQuery.setStart(discoveryQuery.getStart());
            }

            if(discoveryQuery.getMaxResults() != -1){
                solrQuery.setRows(discoveryQuery.getMaxResults());
            }

            if(discoveryQuery.getSortField() != null){
                SolrQuery.ORDER order = SolrQuery.ORDER.asc;
                if(discoveryQuery.getSortOrder().equals(DiscoverQuery.SORT_ORDER.desc))
                    order = SolrQuery.ORDER.desc;

                solrQuery.addSortField(discoveryQuery.getSortField(), order);
            }

            for(String property : discoveryQuery.getProperties().keySet()){
                List<String> values = discoveryQuery.getProperties().get(property);
                solrQuery.add(property, values.toArray(new String[values.size()]));
            }

            List<DiscoverFacetField> facetFields = discoveryQuery.getFacetFields();
            if(0 < facetFields.size()){
                //Only add facet information if there are any facets
                for (DiscoverFacetField facetFieldConfig : facetFields) {
                    String field = transformFacetField(facetFieldConfig, facetFieldConfig.getField(), false);
                    solrQuery.addFacetField(field);

                    // Setting the facet limit in this fashion ensures that each facet can have its own max
                    solrQuery.add("f." + field + "." + FacetParams.FACET_LIMIT, String.valueOf(facetFieldConfig.getLimit()));
                    String facetSort;
                    if(DiscoveryConfigurationParameters.SORT.COUNT.equals(facetFieldConfig.getSortOrder())){
                        facetSort = FacetParams.FACET_SORT_COUNT;
                    }else{
                        facetSort = FacetParams.FACET_SORT_INDEX;
                    }
                    solrQuery.add("f." + field + "." + FacetParams.FACET_SORT, facetSort);

                    if(facetFieldConfig.getPrefix() != null){
                        solrQuery.setFacetPrefix(field, facetFieldConfig.getPrefix());
                    }
                }

                List<String> facetQueries = discoveryQuery.getFacetQueries();
                for (String facetQuery : facetQueries) {
                    solrQuery.addFacetQuery(facetQuery);
                }

                /*
                if(discoveryQuery.getFacetLimit() != -1){
                    solrQuery.setFacetLimit(discoveryQuery.getFacetLimit());
                }
                */
                if(discoveryQuery.getFacetMinCount() != -1){
                    solrQuery.setFacetMinCount(discoveryQuery.getFacetMinCount());
                }

                solrQuery.setParam(FacetParams.FACET_OFFSET, String.valueOf(discoveryQuery.getFacetOffset()));
            }


            QueryResponse queryResponse = getSolr().query(solrQuery);
            return retrieveResult(context, discoveryQuery, queryResponse);

        } catch (Exception e) {
            throw new org.dspace.discovery.SearchServiceException(e.getMessage(),e);
        }
    }

    @Override
    public String searchJSON(DiscoverQuery query, DSpaceObject dso, String jsonIdentifier) throws SearchServiceException {
        if(dso != null){
            if (dso instanceof Community) {
                query.addFilterQueries("location:m" + dso.getID());
            } else if (dso instanceof Collection) {
                query.addFilterQueries("location:l" + dso.getID());
            } else if (dso instanceof Item){
                query.addFilterQueries("handle:" + dso.getHandle());
            }
        }
        return searchJSON(query, jsonIdentifier);
    }


    public String searchJSON(DiscoverQuery query, String jsonIdentifier) throws SearchServiceException {
        Map<String, String> params = new HashMap<String, String>();

        String solrRequestUrl = solr.getBaseURL() + "/select";

        //Add our default parameters
        params.put(CommonParams.ROWS, "0");
        params.put(CommonParams.WT, "json");
        //We uwe json as out output type
        params.put("json.nl", "map");
        params.put("json.wrf", jsonIdentifier);
        params.put(FacetParams.FACET, Boolean.TRUE.toString());

        //Generate our json out of the given params
        try
        {
            params.put(CommonParams.Q, URLEncoder.encode(query.getQuery(), org.dspace.constants.Constants.DEFAULT_ENCODING));
        }
        catch (UnsupportedEncodingException uee)
        {
            //Should never occur
            return null;
        }

        params.put(FacetParams.FACET_MINCOUNT, String.valueOf(query.getFacetMinCount()));

        solrRequestUrl = generateURL(solrRequestUrl, params);
        if (query.getFacetFields() != null || query.getFilterQueries() != null) {
            StringBuilder urlBuilder = new StringBuilder(solrRequestUrl);
            if(query.getFacetFields() != null){

                //Add our facet fields
                for (DiscoverFacetField facetFieldConfig : query.getFacetFields()) {
                    urlBuilder.append("&").append(FacetParams.FACET_FIELD).append("=");


                    //This class can only be used for autocomplete facet fields
                    try {
                        String field = facetFieldConfig.getField() + "_ac";

                        urlBuilder.append(URLEncoder.encode(field, org.dspace.constants.Constants.DEFAULT_ENCODING));
                        //Add the sort order
                        urlBuilder.append("&f.").append(field).append("." + FacetParams.FACET_SORT).append("=");
                        if(DiscoveryConfigurationParameters.SORT.COUNT.equals(facetFieldConfig.getSortOrder())){
                            urlBuilder.append(FacetParams.FACET_SORT_COUNT);
                        }else{
                            urlBuilder.append(FacetParams.FACET_SORT_INDEX);
                        }
                        urlBuilder.append("&f.").append(field).append("." + FacetParams.FACET_LIMIT).append("=").append(facetFieldConfig.getLimit());
                    } catch (UnsupportedEncodingException e) {
                        //Ignore this
                    }
                }
            }
            if(query.getFilterQueries() != null){
                for (String filterQuery : query.getFilterQueries()) {
                    try {
                        urlBuilder.append("&").append(CommonParams.FQ).append("=").append(URLEncoder.encode(filterQuery, org.dspace.constants.Constants.DEFAULT_ENCODING));
                    } catch (UnsupportedEncodingException e) {
                        //Ignore this
                    }
                }
            }

            solrRequestUrl = urlBuilder.toString();
        }

        try {
            GetMethod get = new GetMethod(solrRequestUrl);
            new HttpClient().executeMethod(get);
            return get.getResponseBodyAsString();

        } catch (Exception e) {
            log.error("Error while getting json solr result for discovery search recommendation", e);
            e.printStackTrace();
        }
        return null;
    }

    private String generateURL(String baseURL, Map<String, String> parameters)
    {
        boolean first = true;
        for (String key : parameters.keySet())
        {
            if (first)
            {
                baseURL += "?";
                first = false;
            }
            else
            {
                baseURL += "&";
            }

            baseURL += key + "=" + parameters.get(key);
        }

        return baseURL;
    }

    private DiscoverResult retrieveResult(Context context, DiscoverQuery query, QueryResponse solrQueryResponse) throws SQLException {
        DiscoverResult result = new DiscoverResult();

        if(solrQueryResponse != null){
            result.setStart(query.getStart());
            result.setMaxResults(query.getMaxResults());
            result.setTotalSearchResults(solrQueryResponse.getResults().getNumFound());

            List<String> searchFields = query.getSearchFields();
            for (SolrDocument doc : solrQueryResponse.getResults()) {
                DSpaceObject dso = findDSpaceObject(context, doc);

                if(dso != null){
                    result.addDSpaceObject(dso);
                } else {
                    log.error(LogManager.getHeader(context, "Error while retrieving DSpace object from discovery index", "Handle: " + doc.getFirstValue("handle")));
                    continue;
                }

                DiscoverResult.SearchDocument resultDoc = new DiscoverResult.SearchDocument();
                //Add information about our search fields
                for (String field : searchFields){
                    List<String> valuesAsString = new ArrayList<String>();
                    for (Object o : doc.getFieldValues(field)) {
                        valuesAsString.add(String.valueOf(o));
                    }
                    resultDoc.addSearchField(field, valuesAsString.toArray(new String[valuesAsString.size()]));
                }
                result.addSearchDocument(dso, resultDoc);
            }

            //Resolve our facet field values
            List<FacetField> facetFields = solrQueryResponse.getFacetFields();
            if(facetFields != null){
                for (int i = 0; i <  facetFields.size(); i++) {
                    FacetField facetField = facetFields.get(i);
                    DiscoverFacetField facetFieldConfig = query.getFacetFields().get(i);
                    List<FacetField.Count> facetValues = facetField.getValues();
                    if (facetValues != null) {
                        if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE) && facetFieldConfig.getSortOrder().equals(DiscoveryConfigurationParameters.SORT.VALUE)){
                            //If we have a date & are sorting by value, ensure that the results are flipped for a proper result
                           Collections.reverse(facetValues);
                        }

                        for (FacetField.Count facetValue : facetValues) {
                            String displayedValue = transformDisplayedValue(context, facetField.getName(), facetValue.getName());
                            String field = transformFacetField(facetFieldConfig, facetField.getName(), true);

                            result.addFacetResult(field, new DiscoverResult.FacetResult(facetValue.getAsFilterQuery(), displayedValue, facetValue.getCount()));
                        }
                    }
                }
            }

            if(solrQueryResponse.getFacetQuery() != null){
                //TODO: do not sort when not a date, just retrieve the facets in the order they where requested !
                //At the moment facet queries are only used for dates so we need to sort our results
                TreeMap<String, Integer> sortedFacetQueries = new TreeMap<String, Integer>(solrQueryResponse.getFacetQuery());
                for(String facetQuery : sortedFacetQueries.descendingKeySet()){
                    //TODO: do not assume this, people may want to use it for other ends, use a regex to make sure
                    //We have a facet query, the values looks something like: dateissued.year:[1990 TO 2000] AND -2000
                    //Prepare the string from {facet.field.name}:[startyear TO endyear] to startyear - endyear
                    String facetField = facetQuery.substring(0, facetQuery.indexOf(":"));
                    String name = facetQuery.substring(facetQuery.indexOf('[') + 1);
                    name = name.substring(0, name.lastIndexOf(']')).replaceAll("TO", "-");
                    Integer count = sortedFacetQueries.get(facetQuery);

                    //No need to show empty years
                    if(0 < count){
                        result.addFacetResult(facetField, new DiscoverResult.FacetResult(facetQuery, name, count));
                    }
                }
            }
        }

        return result;
    }

    private static DSpaceObject findDSpaceObject(Context context, SolrDocument doc) throws SQLException {

        Integer type = (Integer) doc.getFirstValue("search.resourcetype");
        Integer id = (Integer) doc.getFirstValue("search.resourceid");
        String handle = (String) doc.getFirstValue("handle");

        if (type != null && id != null) {
            return DSpaceObject.find(context, type, id);
        } else if (handle != null) {
            return HandleManager.resolveToObject(context, handle);
        }

        return null;
    }


    /** Simple means to return the search result as an InputStream */
    public java.io.InputStream searchAsInputStream(DiscoverQuery query) throws SearchServiceException, java.io.IOException {
        try {
            org.apache.commons.httpclient.methods.GetMethod method =
                new org.apache.commons.httpclient.methods.GetMethod(getSolr().getHttpClient().getHostConfiguration().getHostURL() + "");

            method.setQueryString(query.toString());

            getSolr().getHttpClient().executeMethod(method);

            return method.getResponseBodyAsStream();

        } catch (org.apache.solr.client.solrj.SolrServerException e) {
            throw new SearchServiceException(e.getMessage(), e);
        }

    }

    public List<DSpaceObject> search(Context context, String query, int offset, int max, String... filterquery) {
        return search(context, query, null, true, offset, max, filterquery);
    }

    public List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, int offset, int max, String... filterquery) {

        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFields("search.resourceid", "search.resourcetype");
            solrQuery.setStart(offset);
            solrQuery.setRows(max);
            if (orderfield != null) {
                solrQuery.setSortField(orderfield, ascending ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
            }
            if (filterquery != null) {
                solrQuery.addFilterQuery(filterquery);
            }
            QueryResponse rsp = getSolr().query(solrQuery);
            SolrDocumentList docs = rsp.getResults();

            Iterator iter = docs.iterator();
            List<DSpaceObject> result = new ArrayList<DSpaceObject>();
            while (iter.hasNext()) {
                SolrDocument doc = (SolrDocument) iter.next();

                DSpaceObject o = DSpaceObject.find(context, (Integer) doc.getFirstValue("search.resourcetype"), (Integer) doc.getFirstValue("search.resourceid"));

                if (o != null) {
                    result.add(o);
                }
            }
            return result;
		} catch (Exception e) {
			// Any acception that we get ignore it.
			// We do NOT want any crashed to shown by the user
			e.printStackTrace();
            return new ArrayList<DSpaceObject>(0);
		}
    }

    public DiscoverFilterQuery toFilterQuery(Context context, String filterQuery) throws SQLException {
        DiscoverFilterQuery result = new DiscoverFilterQuery();

        //TODO: what if user enters something with a ":" in it
        String field = filterQuery;
        String value = filterQuery;

        if(filterQuery.contains(":"))
        {
            field = filterQuery.substring(0, filterQuery.indexOf(":"));
            value = filterQuery.substring(filterQuery.indexOf(":") + 1, filterQuery.length());
        }else{
            //We have got no field, so we are using everything
            field = "*";
        }

        value = value.replace("\\", "");
        if("*".equals(field))
        {
            field = "all";
        }
        if(filterQuery.startsWith("*:") || filterQuery.startsWith(":"))
        {
            filterQuery = filterQuery.substring(filterQuery.indexOf(":") + 1, filterQuery.length());
        }
//        "(tea coffee)".replaceFirst("\\((.*?)\\)", "")

        value = transformDisplayedValue(context, field, value);

        result.setField(field);
        result.setFilterQuery(filterQuery);
        result.setDisplayedValue(value);

        return result;
    }

    public DiscoverFilterQuery toFilterQuery(Context context, String field, String value) throws SQLException{
        DiscoverFilterQuery result = new DiscoverFilterQuery();

        result.setField(field);
        result.setDisplayedValue(transformDisplayedValue(context, field, value));
//        TODO: solr escape of value ?
        result.setFilterQuery((field == null || field.equals("") ? "" : field + ":") +  "(" + value + ")");


        return result;
    }

    @Override
    public String toSortFieldIndex(String metadataField, String type) {
        if(type.equals(DiscoveryConfigurationParameters.TYPE_DATE)){
            return metadataField + "_dt";
        }else{
            return metadataField + "_sort";
        }
    }

    private String transformFacetField(DiscoverFacetField facetFieldConfig, String field, boolean removePostfix) {
        if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT)){
            if(removePostfix){
                return field.substring(0, field.lastIndexOf("_filter"));
            }else{
                return field + "_filter";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)){
            if(removePostfix){
                return field.substring(0, field.lastIndexOf(".year"));
            }else{
                return field + ".year";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_AC)){
            if(removePostfix){
                return field.substring(0, field.lastIndexOf("_ac"));
            }else{
                return field + "_ac";
            }
        }else{
            return field;
        }
    }

    private String transformDisplayedValue(Context context, String field, String value) throws SQLException {
        if(field.equals("location.comm") || field.equals("location.coll")){
            value = locationToName(context, field, value);
        }else
        if(field.endsWith("_filter")){
            //We have a filter make sure we split !
            String separator = new DSpace().getConfigurationService().getProperty("discovery.solr.facets.split.char");
            if(separator == null){
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer valueBuffer = new StringBuffer();
            int start = fqParts.length / 2;
            for(int i = start; i < fqParts.length; i++){
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
}
