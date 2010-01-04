/*
 * SolrServiceImpl.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 10:02:24 -0700 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.discovery;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.*;
import org.dspace.handle.HandleManager;
import org.dspace.sort.SortOption;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
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
 * @author Mark Diggory
 * @author Ben Bosman
 */
public class SolrServiceImpl implements SearchService, IndexingService {

    private static final Logger log = Logger.getLogger(SolrServiceImpl.class);

    private static final String LAST_INDEXED_FIELD = "SolrIndexer.lastIndexed";

    public static final String DATE_FORMAT_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT_DCDATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static String[] facetFields;


    /**
     * Configuration for indexing Content
     */
    private List<IndexConfig> config = null;

    /**
     * CommonsHttpSolrServer for processing indexing events.
     */
    private CommonsHttpSolrServer solr = null;

    /**
     * Location of Solr Server to work with
     */
    private String solrService = "http://localhost:8080/solr";

    /**
     * Get the SolrService location URL
     *
     * @return
     */
    public String getSolrService() {
        return solrService;
    }

    /**
     * Set the SolrService location URL
     *
     * @param solrService
     */
    public void setSolrService(String solrService) throws Exception {
        this.solrService = solrService;


        if (solrService != null) {
            log.debug("Solr URL: " + solrService);
                    solr = new CommonsHttpSolrServer(solrService);
                    solr.setBaseURL(solrService);
            SolrQuery solrQuery = new SolrQuery()
                        .setQuery("search.resourcetype:2 AND search.resourceid:1");
                solr.query(solrQuery);

        }
        else
        {
            throw new Exception("solr service  URL cannot be null.");
        }
    }

    /**
     * Construct a SolrIndexer, will share a common static HttpClient for connecting to server.
     */
    public SolrServiceImpl() {
    }


    public List<IndexConfig> getConfig() {
        return config;
    }

    public void setConfig(List<IndexConfig> config) {
        this.config = config;
    }


    //private  String index_directory = ConfigurationManager
//			.getProperty("search.dir");

    private int maxfieldlength = -1;

    // TODO: Support for analyzers per language, or multiple indices
    /**
     * The analyzer for this DSpace instance
     */
    private Analyzer analyzer = null;


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

        Term t = new Term("handle", handle);

        try {
            switch (dso.getType()) {
                case Constants.ITEM:
                    Item item = (Item) dso;
                    if (item.isArchived() && !item.isWithdrawn()) {
                        /**
                         * If the item is in the repository now, add it to the index
                         */
                        if (requiresIndexing(t, ((Item) dso).getLastModified())
                                || force) {
                            unIndexContent(context, handle);
                            buildDocument(context, (Item) dso, t);
                        }
                    } else {
                        /**
                         * Make sure the item is not in the index if it is not in
                         * archive. TODO: Someday DSIndexer should block withdrawn
                         * content on search/retrieval and allow admins the ablitity
                         * to still search for withdrawn Items.
                         */
                        unIndexContent(context, handle);
                        log.info("Removed Item: " + handle + " from Index");
                    }
                    break;

                case Constants.COLLECTION:
                    buildDocument(context, (Collection) dso, t);
                    log.info("Wrote Collection: " + handle + " to Index");
                    break;

                case Constants.COMMUNITY:
                    buildDocument(context, (Community) dso, t);
                    log.info("Wrote Community: " + handle + " to Index");
                    break;

                default:
                    log
                            .error("Only Items, Collections and Communities can be Indexed");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        try {
            solr.commit();
        } catch (SolrServerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
     * Unindex a Docment in the Lucene Index.
     *
     * @param context
     * @param handle
     * @throws SQLException
     * @throws IOException
     */
    public void unIndexContent(Context context, String handle)
            throws SQLException, IOException {

        try {
            solr.deleteById(handle);
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }
        /*
          IndexWriter writer = openIndex(false);

          try {
              if (handle != null) {
                  // we have a handle (our unique ID, so remove)
                  Term t = new Term("handle", handle);
                  writer.deleteDocuments(t);
              } else {
                  log.warn("unindex of content with null handle attempted");

                  // FIXME: no handle, fail quietly - should log failure
                  // System.out.println("Error in unIndexContent: Object had no
                  // handle!");
              }
          } finally {
              writer.close();
          }
          */
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
     * and isStale ot check state of item in index.
     *
     * @param context
     */
    public void updateIndex(Context context) {
        updateIndex(context, false);
    }

    /**
     * Iterates over all Items, Collections and Communities. And updates them in
     * the index. Uses decaching to control memory footprint. Uses indexContent
     * and isStale ot check state of item in index.
     * <p/>
     * At first it may appear counterintuitive to have an IndexWriter/Reader
     * opened and closed on each DSO. But this allows the UI processes to step
     * in and attain a lock and write to the index even if other processes/jvms
     * are running a reindex.
     *
     * @param context
     * @param force
     */
    public void updateIndex(Context context, boolean force) {
        try {
            ItemIterator items = null;
            try {
                for (items = Item.findAll(context); items.hasNext();) {
                    Item item = (Item) items.next();
                    indexContent(context, item, force);
                    item.decache();
                }
            } finally {
                if (items != null)
                    items.close();
            }

            Collection[] collections = Collection.findAll(context);
            for (int i = 0; i < collections.length; i++) {
                indexContent(context, collections[i], force);
                context.removeCached(collections[i], collections[i].getID());

            }

            Community[] communities = Community.findAll(context);
            for (int i = 0; i < communities.length; i++) {
                indexContent(context, communities[i], force);
                context.removeCached(communities[i], communities[i].getID());
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Iterates over all documents in the Lucene index and verifies they are in
     * database, if not, they are removed.
     *
     * @param force
     * @throws IOException
     * @throws SQLException
     * @throws SolrServerException
     */
    public void cleanIndex(boolean force) throws IOException,
            SQLException, SearchServiceException {

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try
        {
            if (force) {
                solr.deleteByQuery("*:*");
            } else {
                SolrQuery query = new SolrQuery();
                query.setQuery("*:*");
                QueryResponse rsp = solr.query(query);
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
     * @param t
     * @param lastModified
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws SolrServerException
     */
    private boolean requiresIndexing(Term t, Date lastModified)
            throws SQLException, IOException, SearchServiceException {

        boolean reindexItem = false;
        boolean inIndex = false;

        SolrQuery query = new SolrQuery();
        query.setQuery("*:*");
        QueryResponse rsp = null;

        try {
            rsp = solr.query(query);
        } catch (SolrServerException e) {
            throw new SearchServiceException(e.getMessage(),e);
        }

        for (SolrDocument doc : rsp.getResults()) {

            inIndex = true;

            Object value = doc.getFieldValue(LAST_INDEXED_FIELD);

            if(value instanceof Date)
            {
                Date lastIndexed = (Date) value;

                if (lastIndexed == null
                    || lastIndexed.before(lastModified)) {

                    reindexItem = true;
                }
            }
        }

        return reindexItem || !inIndex;
    }


    /**
     * @param c
     * @param myitem
     * @return
     * @throws SQLException
     */
    private List<String> getItemLocations(Context c, Item myitem)
            throws SQLException {
        List<String> locations = new Vector<String>();

        // build list of community ids
        Community[] communities = myitem.getCommunities();

        // build list of collection ids
        Collection[] collections = myitem.getCollections();

        // now put those into strings
        int i = 0;

        for (i = 0; i < communities.length; i++)
            locations.add(new String("m" + communities[i].getID()));

        for (i = 0; i < collections.length; i++)
            locations.add(new String("l" + collections[i].getID()));

        return locations;
    }

    private List<String> getCollectionLocations(Context c,
                                                Collection target) throws SQLException {
        List<String> locations = new Vector<String>();
        // build list of community ids
        Community[] communities = target.getCommunities();

        // now put those into strings
        String location = "";
        int i = 0;

        for (i = 0; i < communities.length; i++)
            locations.add(new String("m" + communities[i].getID()));

        return locations;
    }

    /**
     * Write the document to the index under the appropriate term (t).
     *
     * @param t
     * @param doc
     * @throws IOException
     */
    private void writeDocument(Term t, SolrInputDocument doc) throws IOException {

        try {
            solr.add(doc);
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }

        /*IndexWriter writer = null;

          try {
              writer = openIndex(false);
              writer.updateDocument(t, doc);
          } catch (Exception e) {
              log.error(e.getMessage(), e);
          } finally {
              if (writer != null) {
                  writer.close();
              }
          }*/
    }

    /**
     * Build a Lucene document for a DSpace Community.
     *
     * @param context   Users Context
     * @param community Community to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private void buildDocument(Context context, Community community,
                               Term t) throws SQLException, IOException {
        // Create Lucene Document
        SolrInputDocument doc = buildDocument(Constants.COMMUNITY, community.getID(),
                community.getHandle(), null);

        // and populate it
        String name = community.getMetadata("name");

        if (name != null) {
            doc.addField("name", name);
            //TODO: multilang
            //doc.add(new Field("name", name, Field.Store.NO,
            //		Field.Index.TOKENIZED));
//			doc.addField("default",name);
            //doc.add(new Field("default", name, Field.Store.NO,
            //		Field.Index.TOKENIZED));
        }

        writeDocument(t, doc);
    }

    /**
     * Build a Lucene document for a DSpace Collection.
     *
     * @param context    Users Context
     * @param collection Collection to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private void buildDocument(Context context, Collection collection,
                               Term t) throws SQLException, IOException {
        List<String> locations = getCollectionLocations(context,
                collection);

        // Create Lucene Document
        SolrInputDocument doc = buildDocument(Constants.COLLECTION, collection.getID(),
                collection.getHandle(), locations);

        // and populate it
        String name = collection.getMetadata("name");

        if (name != null) {
            doc.addField("name", name);
            //TODO: multilang
            //doc.add(new Field("name", name, Field.Store.NO,
            //		Field.Index.TOKENIZED));
//			doc.addField("default",name);
            //doc.add(new Field("default", name, Field.Store.NO,
            //		Field.Index.TOKENIZED));
        }

        writeDocument(t, doc);
    }

    /**
     * Build a Lucene document for a DSpace Item and write the index
     *
     * @param context Users Context
     * @param item    The DSpace Item to be indexed
     * @param t       The Item handle term
     * @throws SQLException
     * @throws IOException
     */
    private void buildDocument(Context context, Item item, Term t)
            throws SQLException, IOException {
        String handle = item.getHandle();

        if (handle == null) {
            handle = HandleManager.findHandle(context, item);
        }

        // get the location string (for searching by collection & community)
        List<String> locations = getItemLocations(context, item);

        SolrInputDocument doc = buildDocument(Constants.ITEM, item.getID(), handle,
                locations);

        log.debug("Building Item: " + handle);


        try {

            DCValue[] mydc = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            for(DCValue meta : mydc)
            {
                String field = meta.schema + "." + meta.element;

                String value = meta.value;

                if (meta.element.equals("date") && meta.schema.equals("dc"))
                {
                    try{
                        value = DateFormatUtils.formatUTC(this.toDate(value), "yyyy-MM-dd'T'HH:mm:ss'Z'");
                    } catch (Exception e)  {
                        log.error(e.getMessage(), e);
                    }
                }
                
                if(meta.qualifier != null && !meta.qualifier.trim().equals("")) {
                    field += "." + meta.qualifier;
                }

                doc.addField(field, value);

                if(meta.language != null && !meta.language.trim().equals("")) {
                    field += "." + meta.language;
                    doc.addField(field, value);
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
                doc.addField("publication_grp",values[0].value.replaceFirst("http://hdl.handle.net/","") );
                
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

            for (int i = 0; i < myBundles.length; i++) {
                if ((myBundles[i].getName() != null)
                        && myBundles[i].getName().equals("TEXT")) {
                    // a-ha! grab the text out of the bitstreams
                    Bitstream[] myBitstreams = myBundles[i].getBitstreams();

                    for (int j = 0; j < myBitstreams.length; j++) {
                        try {
                            InputStreamReader is = new InputStreamReader(
                                    myBitstreams[j].retrieve()); // get input
                            readers.add(is);

                            // Add each InputStream to the Indexed Document
                            // (Acts like an Append)
//							doc.addField("default", is);
                            //doc.add(new Field("default", is));

                            log.debug("  Added BitStream: "
                                    + myBitstreams[j].getStoreNumber() + "	"
                                    + myBitstreams[j].getSequenceID() + "   "
                                    + myBitstreams[j].getName());

                        } catch (Exception e) {
                            // this will never happen, but compiler is now
                            // happy.
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // write the index and close the inputstreamreaders
        try {
            writeDocument(t, doc);
            log.info("Wrote Item: " + handle + " to Index");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
            for (int i = 0; i < locations.size(); i++) {
                doc.addField("location", locations.get(i));
            }
        }

        return doc;
    }

    /**
     * Helper function to retrieve a date using a best guess of the potential
     * date encodings on a field
     *
     * @param t
     * @return
     */
    private Date toDate(String t) {
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

    //******** SearchService implementation
    public QueryResponse search(SolrQuery query) throws SearchServiceException {
        if (solr != null) {
            try {
                return solr.query(query);
            } catch (SolrServerException e) {
                throw new SearchServiceException(e.getMessage(),e);
            }
        } else {
            return null;
        }
    }

    public void setFacetFields(String[] stringArray) {
        this.facetFields = stringArray;

    }

    public String[] getFacetFields() {
        return facetFields;
    }


    public DSpaceObject findDSpaceObject(Context context, SolrDocument doc) throws SQLException {

        Integer type = (Integer) doc.getFirstValue("search.resourcetype");
        Integer id = (Integer) doc.getFirstValue("search.resourceid");
        String handle = (String)doc.getFirstValue("handle");

        if(type != null && id != null) {
             return DSpaceObject.find(context,type,id);
        } else if( handle != null) {
             return HandleManager.resolveToObject(context, handle);
        }
          
        return null;
    }

    public List<DSpaceObject> search(Context context, String query, int offset, int max, String... filterquery) {
        return search(context, query, null, true, offset, max, filterquery);
    }

    public List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, int offset, int max, String... filterquery) {
        if (solr == null)
            return new ArrayList<DSpaceObject>(0);
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
            QueryResponse rsp = solr.query(solrQuery);
            SolrDocumentList docs = rsp.getResults();

            Iterator iter = docs.iterator();
            List<DSpaceObject> result = new ArrayList<DSpaceObject>();
            while (iter.hasNext()) {
                SolrDocument doc = (SolrDocument) iter.next();

                String handle = (String) doc.getFieldValue("handle");

                DSpaceObject o = DSpaceObject.find(context, (Integer) doc.getFirstValue("search.resourcetype"), (Integer) doc.getFirstValue("search.resourceid"));

                if (o == null) {
                    log.warn("Deleted object: " + doc.getFirstValue("search.resourcetype") + ", " + doc.getFirstValue("search.resourceid"));
                    unIndexContent(context, handle);
                } else {
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

}
