/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

import java.io.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MapSolrParams;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.util.DnsLookup;
import org.dspace.statistics.util.LocationUtils;
import org.dspace.statistics.util.SpiderDetector;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Static holder for a HttpSolrClient connection pool to issue
 * usage logging events to Solr from DSpace libraries, and some static query
 * composers.
 * 
 * @author ben at atmire.com
 * @author kevinvandevelde at atmire.com
 * @author mdiggory at atmire.com
 */
public class SolrLogger
{
    private static final Logger log = Logger.getLogger(SolrLogger.class);
	
    private static final CommonsHttpSolrServer solr;

    public static final String DATE_FORMAT_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT_DCDATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final LookupService locationService;

    private static final boolean useProxies;

    private static Map<String, String> metadataStorageInfo;

    static
    {
        log.info("solr-statistics.spidersfile:" + ConfigurationManager.getProperty("solr-statistics", "spidersfile"));
        log.info("solr-statistics.server:" + ConfigurationManager.getProperty("solr-statistics", "server"));
        log.info("usage-statistics.dbfile:" + ConfigurationManager.getProperty("usage-statistics", "dbfile"));
    	
        CommonsHttpSolrServer server = null;
        
        if (ConfigurationManager.getProperty("solr-statistics", "server") != null)
        {
            try
            {
                server = new CommonsHttpSolrServer(ConfigurationManager.getProperty("solr-statistics", "server"));
                SolrQuery solrQuery = new SolrQuery()
                        .setQuery("type:2 AND id:1");
                server.query(solrQuery);
            } catch (Exception e) {
            	log.error(e.getMessage(), e);
            }
        }
        solr = server;

        // Read in the file so we don't have to do it all the time
        //spiderIps = SpiderDetector.getSpiderIpAddresses();

        LookupService service = null;
        // Get the db file for the location
        String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
        if (dbfile != null)
        {
            try
            {
                service = new LookupService(dbfile,
                        LookupService.GEOIP_STANDARD);
            }
            catch (FileNotFoundException fe)
            {
                log.error("The GeoLite Database file is missing (" + dbfile + ")! Solr Statistics cannot generate location based reports! Please see the DSpace installation instructions for instructions to install this file.", fe);
            }
            catch (IOException e)
            {
                log.error("Unable to load GeoLite Database file (" + dbfile + ")! You may need to reinstall it. See the DSpace installation instructions for more details.", e);
            }
        }
        else
        {
            log.error("The required 'dbfile' configuration is missing in usage-statistics.cfg!");
        }
        locationService = service;

        if ("true".equals(ConfigurationManager.getProperty("useProxies")))
        {
            useProxies = true;
        }
        else
        {
            useProxies = false;
        }

        log.info("useProxies=" + useProxies);

        metadataStorageInfo = new HashMap<String, String>();
        int count = 1;
        String metadataVal;
        while ((metadataVal = ConfigurationManager.getProperty("solr-statistics","metadata.item." + count)) != null)
        {
            String storeVal = metadataVal.split(":")[0];
            String metadataField = metadataVal.split(":")[1];

            metadataStorageInfo.put(storeVal, metadataField);
            log.info("solr-statistics.metadata.item." + count + "=" + metadataVal);
            count++;
        }
    }

    /**
     * Store a usage event into Solr.
     * 
     * @param dspaceObject the object used.
     * @param request the current request context.
     * @param currentUser the current session's user.
     */
    public static void post(DSpaceObject dspaceObject, HttpServletRequest request,
            EPerson currentUser)
    {
        if (solr == null || locationService == null)
        {
            return;
        }

        boolean isSpiderBot = SpiderDetector.isSpider(request);

        try
        {
            if(isSpiderBot &&
                    !ConfigurationManager.getBooleanProperty("usage-statistics", "logBots",true))
            {
                return;
            }


                        
            SolrInputDocument doc1 = new SolrInputDocument();
            // Save our basic info that we already have

            String ip = request.getRemoteAddr();

	        if(isUseProxies() && request.getHeader("X-Forwarded-For") != null)
            {
                /* This header is a comma delimited list */
	            for(String xfip : request.getHeader("X-Forwarded-For").split(","))
                {
                    /* proxy itself will sometime populate this header with the same value in
                        remote address. ordering in spec is vague, we'll just take the last
                        not equal to the proxy
                    */
                    if(!request.getHeader("X-Forwarded-For").contains(ip))
                    {
                        ip = xfip.trim();
                    }
                }
	        }

            doc1.addField("ip", ip);
                        
            doc1.addField("id", dspaceObject.getID());
            doc1.addField("type", dspaceObject.getType());
            // Save the current time
            doc1.addField("time", DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
            if (currentUser != null)
            {
                doc1.addField("epersonid", currentUser.getID());
            }

            try
            {
            	String dns = DnsLookup.reverseDns(ip);
                doc1.addField("dns", dns.toLowerCase());
            }
            catch (Exception e)
            {
    			log.error("Failed DNS Lookup for IP:" + ip);
                log.debug(e.getMessage(),e);
    		}
            
            // Save the location information if valid, save the event without
            // location information if not valid
            Location location = locationService.getLocation(ip);
            if (location != null
                    && !("--".equals(location.countryCode)
                            && location.latitude == -180 && location.longitude == -180))
            {
                try
                {
                    doc1.addField("continent", LocationUtils
                            .getContinentCode(location.countryCode));
                }
                catch (Exception e)
                {
                    System.out
                            .println("COUNTRY ERROR: " + location.countryCode);
                }
                doc1.addField("countryCode", location.countryCode);
                doc1.addField("city", location.city);
                doc1.addField("latitude", location.latitude);
                doc1.addField("longitude", location.longitude);
                doc1.addField("isBot",isSpiderBot);

                if(request.getHeader("User-Agent") != null)
                {
                    doc1.addField("userAgent", request.getHeader("User-Agent"));
                }
            }
            
            if (dspaceObject instanceof Item)
            {
                Item item = (Item) dspaceObject;
                // Store the metadata
                for (Object storedField : metadataStorageInfo.keySet())
                {
                    String dcField = metadataStorageInfo
                            .get(storedField);

                    DCValue[] vals = item.getMetadata(dcField.split("\\.")[0],
                            dcField.split("\\.")[1], dcField.split("\\.")[2],
                            Item.ANY);
                    for (DCValue val1 : vals)
                    {
                        String val = val1.value;
                        doc1.addField(String.valueOf(storedField), val);
                        doc1.addField(storedField + "_search", val
                                .toLowerCase());
                    }
                }
            }

            if(dspaceObject instanceof Bitstream)
            {
                Bitstream bit = (Bitstream) dspaceObject;
                Bundle[] bundles = bit.getBundles();
                for (Bundle bundle : bundles) {
                    doc1.addField("bundleName", bundle.getName());
                }
            }

            storeParents(doc1, dspaceObject);

            solr.add(doc1);
            //commits are executed automatically using the solr autocommit
//            solr.commit(false, false);
   
        }
        catch (RuntimeException re)
        {
            throw re;
        }
        catch (Exception e)
        {
        	log.error(e.getMessage(), e);
        }
    }

    //@TODO remove metadataStorage object from all see: DS-421
    public static Map<String, String> getMetadataStorageInfo()
    {
        return metadataStorageInfo;
    }

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
     * @throws java.sql.SQLException
     *             ignore it
     */
    public static void storeParents(SolrInputDocument doc1, DSpaceObject dso)
            throws SQLException
    {
        if (dso instanceof Community)
        {
            Community comm = (Community) dso;
            while (comm != null && comm.getParentCommunity() != null)
            {
                comm = comm.getParentCommunity();
                doc1.addField("owningComm", comm.getID());
            }
        }
        else if (dso instanceof Collection)
        {
            Collection coll = (Collection) dso;
            for (int i = 0; i < coll.getCommunities().length; i++)
            {
                Community community = coll.getCommunities()[i];
                doc1.addField("owningComm", community.getID());
                storeParents(doc1, community);
            }
        }
        else if (dso instanceof Item)
        {
            Item item = (Item) dso;
            for (int i = 0; i < item.getCollections().length; i++)
            {
                Collection collection = item.getCollections()[i];
                doc1.addField("owningColl", collection.getID());
                storeParents(doc1, collection);
            }
        }
        else if (dso instanceof Bitstream)
        {
            Bitstream bitstream = (Bitstream) dso;
            for (int i = 0; i < bitstream.getBundles().length; i++)
            {
                Bundle bundle = bitstream.getBundles()[i];
                for (int j = 0; j < bundle.getItems().length; j++)
                {
                    Item item = bundle.getItems()[j];
                    doc1.addField("owningItem", item.getID());
                    storeParents(doc1, item);
                }
            }
        }
    }

    public static boolean isUseProxies()
    {
        return useProxies;
    }

    /**
     * Delete data from the index, as described by a query.
     * 
     * @param query description of the records to be deleted.
     * @throws IOException
     * @throws SolrServerException
     */
    public static void removeIndex(String query) throws IOException,
            SolrServerException
    {
        solr.deleteByQuery(query);
        solr.commit();
    }

    public static Map<String, List<String>> queryField(String query,
            List oldFieldVals, String field)
    {
        Map<String, List<String>> currentValsStored = new HashMap<String, List<String>>();
        try
        {
            // Get one document (since all the metadata for all the values
            // should be the same just get the first one we find
            Map<String, String> params = new HashMap<String, String>();
            params.put("q", query);
            params.put("rows", "1");
            MapSolrParams solrParams = new MapSolrParams(params);
            QueryResponse response = solr.query(solrParams);
            // Make sure we at least got a document
            if (response.getResults().getNumFound() == 0)
            {
                return currentValsStored;
            }

            // We have at least one document good
            SolrDocument document = response.getResults().get(0);
            for (Object storedField : metadataStorageInfo.keySet())
            {
                // For each of these fields that are stored we are to create a
                // list of the values it holds now
                java.util.Collection collection = document
                        .getFieldValues((String) storedField);
                List<String> storedVals = new ArrayList<String>();
                storedVals.addAll(collection);
                // Now add it to our hashmap
                currentValsStored.put((String) storedField, storedVals);
            }

            // System.out.println("HERE");
            // Get the info we need
        }
        catch (SolrServerException e)
        {
            e.printStackTrace();
        }
        return currentValsStored;
    }


    public static class ResultProcessor
    {

        public void execute(String query) throws SolrServerException, IOException {
            Map<String, String> params = new HashMap<String, String>();
            params.put("q", query);
            params.put("rows", "10");
            MapSolrParams solrParams = new MapSolrParams(params);
            QueryResponse response = solr.query(solrParams);
            
            long numbFound = response.getResults().getNumFound();

            // process the first batch
            process(response.getResults());

            // Run over the rest
            for (int i = 10; i < numbFound; i += 10)
            {
                params.put("start", String.valueOf(i));
                solrParams = new MapSolrParams(params);
                response = solr.query(solrParams);
                process(response.getResults());
            }

        }

        public void commit() throws IOException, SolrServerException {
            solr.commit();
        }

        /**
         * Override to manage pages of documents
         * @param docs
         */
        public void process(List<SolrDocument> docs) throws IOException, SolrServerException {
            for(SolrDocument doc : docs){
                process(doc);
            }
        }

        /**
         * Override to manage individual documents
         * @param doc
         */
        public void process(SolrDocument doc) throws IOException, SolrServerException {


        }
    }


    public static void markRobotsByIP()
    {
        for(String ip : SpiderDetector.getSpiderIpAddresses()){

            try {

                /* Result Process to alter record to be identified as a bot */
                ResultProcessor processor = new ResultProcessor(){
                    public void process(SolrDocument doc) throws IOException, SolrServerException {
                        doc.removeFields("isBot");
                        doc.addField("isBot", true);
                        SolrInputDocument newInput = ClientUtils.toSolrInputDocument(doc);
                        solr.add(newInput);
                        log.info("Marked " + doc.getFieldValue("ip") + " as bot");
                    }
                };

                /* query for ip, exclude results previously set as bots. */
                processor.execute("ip:"+ip+ "* AND -isBot:true");

                solr.commit();

            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }


        }

    }

    public static void markRobotByUserAgent(String agent){
        try {

                /* Result Process to alter record to be identified as a bot */
                ResultProcessor processor = new ResultProcessor(){
                    public void process(SolrDocument doc) throws IOException, SolrServerException {
                        doc.removeFields("isBot");
                        doc.addField("isBot", true);
                        SolrInputDocument newInput = ClientUtils.toSolrInputDocument(doc);
                        solr.add(newInput);
                    }
                };

                /* query for ip, exclude results previously set as bots. */
                processor.execute("userAgent:"+agent+ " AND -isBot:true");

                solr.commit();
            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }
    }

    public static void deleteRobotsByIsBotFlag()
    {
        try {
           solr.deleteByQuery("isBot:true");
        } catch (Exception e) {
           log.error(e.getMessage(),e);
        }
    }

    public static void deleteIP(String ip)
    {
        try {
            solr.deleteByQuery("ip:"+ip + "*");
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
    }


    public static void deleteRobotsByIP()
    {
        for(String ip : SpiderDetector.getSpiderIpAddresses()){
            deleteIP(ip);
        }
    }

    /*
     * //TODO: below are not used public static void
     * update(String query, boolean addField, String fieldName, Object
     * fieldValue, Object oldFieldValue) throws SolrServerException, IOException
     * { List<Object> vals = new ArrayList<Object>(); vals.add(fieldValue);
     * List<Object> oldvals = new ArrayList<Object>(); oldvals.add(fieldValue);
     * update(query, addField, fieldName, vals, oldvals); }
     */
    public static void update(String query, String action,
            List<String> fieldNames, List<List<Object>> fieldValuesList)
            throws SolrServerException, IOException
    {
        // Since there is NO update
        // We need to get our documents
        // QueryResponse queryResponse = solr.query()//query(query, null, -1,
        // null, null, null);

        final List<SolrDocument> docsToUpdate = new ArrayList<SolrDocument>();

        ResultProcessor processor = new ResultProcessor(){
                public void process(List<SolrDocument> docs) throws IOException, SolrServerException {
                    docsToUpdate.addAll(docs);
                }
            };

        processor.execute(query);

        // We have all the docs delete the ones we don't need
        solr.deleteByQuery(query);

        // Add the new (updated onces
        for (int i = 0; i < docsToUpdate.size(); i++)
        {
            SolrDocument solrDocument = docsToUpdate.get(i);
            // Now loop over our fieldname actions
            for (int j = 0; j < fieldNames.size(); j++)
            {
                String fieldName = fieldNames.get(j);
                List<Object> fieldValues = fieldValuesList.get(j);

                if (action.equals("addOne") || action.equals("replace"))
                {
                    if (action.equals("replace"))
                    {
                        solrDocument.removeFields(fieldName);
                    }

                    for (Object fieldValue : fieldValues)
                    {
                        solrDocument.addField(fieldName, fieldValue);
                    }
                }
                else if (action.equals("remOne"))
                {
                    // Remove the field
                    java.util.Collection<Object> values = solrDocument
                            .getFieldValues(fieldName);
                    solrDocument.removeFields(fieldName);
                    for (Object value : values)
                    {
                        // Keep all the values besides the one we need to remove
                        if (!fieldValues.contains((value)))
                        {
                            solrDocument.addField(fieldName, value);
                        }
                    }
                }
            }
            SolrInputDocument newInput = ClientUtils
                    .toSolrInputDocument(solrDocument);
            solr.add(newInput);
        }
        solr.commit();
        // System.out.println("SolrLogger.update(\""+query+"\"):"+(new
        // Date().getTime() - start)+"ms,"+numbFound+"records");
    }

    public static void query(String query, int max) throws SolrServerException
    {
        query(query, null, null, max, null, null, null, null);
    }

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
    public static ObjectCount[] queryFacetField(String query,
            String filterQuery, String facetField, int max, boolean showTotal,
            List<String> facetQueries) throws SolrServerException
    {
        QueryResponse queryResponse = query(query, filterQuery, facetField,
                max, null, null, null, facetQueries);
        if (queryResponse == null)
        {
            return new ObjectCount[0];
        }

        FacetField field = queryResponse.getFacetField(facetField);
        // At least make sure we have one value
        if (0 < field.getValueCount())
        {
            // Create an array for our result
            ObjectCount[] result = new ObjectCount[field.getValueCount()
                    + (showTotal ? 1 : 0)];
            // Run over our results & store them
            for (int i = 0; i < field.getValues().size(); i++)
            {
                FacetField.Count fieldCount = field.getValues().get(i);
                result[i] = new ObjectCount();
                result[i].setCount(fieldCount.getCount());
                result[i].setValue(fieldCount.getName());
            }
            if (showTotal)
            {
                result[result.length - 1] = new ObjectCount();
                result[result.length - 1].setCount(queryResponse.getResults()
                        .getNumFound());
                result[result.length - 1].setValue("total");
            }
            return result;
        }
        else
        {
            // Return an empty array cause we got no data
            return new ObjectCount[0];
        }
    }

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
    public static ObjectCount[] queryFacetDate(String query,
            String filterQuery, int max, String dateType, String dateStart,
            String dateEnd, boolean showTotal) throws SolrServerException
    {
        QueryResponse queryResponse = query(query, filterQuery, null, max,
                dateType, dateStart, dateEnd, null);
        if (queryResponse == null)
        {
            return new ObjectCount[0];
        }

        FacetField dateFacet = queryResponse.getFacetDate("time");
        // TODO: check if this cannot crash I checked it, it crashed!!!
        // Create an array for our result
        ObjectCount[] result = new ObjectCount[dateFacet.getValueCount()
                + (showTotal ? 1 : 0)];
        // Run over our datefacet & store all the values
        for (int i = 0; i < dateFacet.getValues().size(); i++)
        {
            FacetField.Count dateCount = dateFacet.getValues().get(i);
            result[i] = new ObjectCount();
            result[i].setCount(dateCount.getCount());
            result[i].setValue(getDateView(dateCount.getName(), dateType));
        }
        if (showTotal)
        {
            result[result.length - 1] = new ObjectCount();
            result[result.length - 1].setCount(queryResponse.getResults()
                    .getNumFound());
            // TODO: Make sure that this total is gotten out of the msgs.xml
            result[result.length - 1].setValue("total");
        }
        return result;
    }

    public static Map<String, Integer> queryFacetQuery(String query,
            String filterQuery, List<String> facetQueries)
            throws SolrServerException
    {
        QueryResponse response = query(query, filterQuery, null, 1, null, null,
                null, facetQueries);
        return response.getFacetQuery();
    }

    public static ObjectCount queryTotal(String query, String filterQuery)
            throws SolrServerException
    {
        QueryResponse queryResponse = query(query, filterQuery, null, -1, null,
                null, null, null);
        ObjectCount objCount = new ObjectCount();
        objCount.setCount(queryResponse.getResults().getNumFound());

        return objCount;
    }

    private static String getDateView(String name, String type)
    {
        if (name != null && name.matches("^[0-9]{4}\\-[0-9]{2}.*"))
        {
            /*
             * if("YEAR".equalsIgnoreCase(type)) return name.substring(0, 4);
             * else if("MONTH".equalsIgnoreCase(type)) return name.substring(0,
             * 7); else if("DAY".equalsIgnoreCase(type)) return
             * name.substring(0, 10); else if("HOUR".equalsIgnoreCase(type))
             * return name.substring(11, 13);
             */
            // Get our date
            Date date = null;
            try
            {
                SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_8601);
                date = format.parse(name);
            }
            catch (ParseException e)
            {
                try
                {
                    // We should use the dcdate (the dcdate is used when
                    // generating random data)
                    SimpleDateFormat format = new SimpleDateFormat(
                            DATE_FORMAT_DCDATE);
                    date = format.parse(name);
                }
                catch (ParseException e1)
                {
                    e1.printStackTrace();
                }
                // e.printStackTrace();
            }
            String dateformatString = "dd-MM-yyyy";
            if ("DAY".equals(type))
            {
                dateformatString = "dd-MM-yyyy";
            }
            else if ("MONTH".equals(type))
            {
                dateformatString = "MMMM yyyy";

            }
            else if ("YEAR".equals(type))
            {
                dateformatString = "yyyy";
            }
            SimpleDateFormat simpleFormat = new SimpleDateFormat(
                    dateformatString);
            if (date != null)
            {
                name = simpleFormat.format(date);
            }

        }
        return name;
    }

    private static QueryResponse query(String query, String filterQuery,
            String facetField, int max, String dateType, String dateStart,
            String dateEnd, List<String> facetQueries)
            throws SolrServerException
    {
        if (solr == null)
        {
            return null;
        }

        // System.out.println("QUERY");
        SolrQuery solrQuery = new SolrQuery().setRows(0).setQuery(query)
                .setFacetMinCount(1);

        // Set the date facet if present
        if (dateType != null)
        {
            solrQuery.setParam("facet.date", "time")
                    .
                    // EXAMPLE: NOW/MONTH+1MONTH
                    setParam("facet.date.end",
                            "NOW/" + dateType + dateEnd + dateType).setParam(
                            "facet.date.gap", "+1" + dateType)
                    .
                    // EXAMPLE: NOW/MONTH-" + nbMonths + "MONTHS
                    setParam("facet.date.start",
                            "NOW/" + dateType + dateStart + dateType + "S")
                    .setFacet(true);
        }
        if (facetQueries != null)
        {
            for (int i = 0; i < facetQueries.size(); i++)
            {
                String facetQuery = facetQueries.get(i);
                solrQuery.addFacetQuery(facetQuery);
            }
            if (0 < facetQueries.size())
            {
                solrQuery.setFacet(true);
            }
        }

        if (facetField != null)
        {
            solrQuery.addFacetField(facetField);
        }

        // Set the top x of if present
        if (max != -1)
        {
            solrQuery.setFacetLimit(max);
        }

        // A filter is used instead of a regular query to improve
        // performance and ensure the search result ordering will
        // not be influenced

        // Choose to filter by the Legacy spider IP list (may get too long to properly filter all IP's
        if(ConfigurationManager.getBooleanProperty("solr-statistics", "query.filter.spiderIp",false))
        {
            solrQuery.addFilterQuery(getIgnoreSpiderIPs());
        }

        // Choose to filter by isBot field, may be overriden in future
        // to allow views on stats based on bots.
        if(ConfigurationManager.getBooleanProperty("solr-statistics", "query.filter.isBot",true))
        {
            solrQuery.addFilterQuery("-isBot:true");
        }

        String bundles;
        if((bundles = ConfigurationManager.getProperty("solr-statistics", "query.filter.bundles")) != null && 0 < bundles.length()){

            /**
             * The code below creates a query that will allow only records which do not have a bundlename
             * (items, collections, ...) or bitstreams that have a configured bundle name
             */
            StringBuffer bundleQuery = new StringBuffer();
            //Also add the possibility that if no bundle name is there these results will also be returned !
            bundleQuery.append("-(bundleName:[* TO *]");
            String[] split = bundles.split(",");
            for (int i = 0; i < split.length; i++) {
                String bundle = split[i].trim();
                bundleQuery.append("-bundleName:").append(bundle);
                if(i != split.length - 1){
                    bundleQuery.append(" AND ");
                }
            }
            bundleQuery.append(")");


            solrQuery.addFilterQuery(bundleQuery.toString());
        }

        if (filterQuery != null)
        {
            solrQuery.addFilterQuery(filterQuery);
        }

        QueryResponse response;
        try
        {
            // solr.set
            response = solr.query(solrQuery);
        }
        catch (SolrServerException e)
        {
            System.err.println("Error using query " + query);
            throw e;
        }
        return response;
    }


    /** String of IP and Ranges in IPTable as a Solr Query */
    private static String filterQuery = null;

    /**
     * Returns in a filterQuery string all the ip addresses that should be ignored
     *
     * @return a string query with ip addresses
     */
    public static String getIgnoreSpiderIPs() {
        if (filterQuery == null) {
            StringBuilder query = new StringBuilder();
            boolean first = true;
            for (String ip : SpiderDetector.getSpiderIpAddresses()) {
                if (first) {
                    query.append(" AND ");
                    first = false;
                }

                query.append(" NOT(ip: ").append(ip).append(")");
            }
            filterQuery = query.toString();
        }

        return filterQuery;

    }
    
    /**
     * Maintenance to keep a SOLR index efficient.
     * Note: This might take a long time.
     */
    public static void optimizeSOLR() {
        try {
            long start = System.currentTimeMillis();
            System.out.println("SOLR Optimize -- Process Started:"+start);
            solr.optimize();
            long finish = System.currentTimeMillis();
            System.out.println("SOLR Optimize -- Process Finished:"+finish);
            System.out.println("SOLR Optimize -- Total time taken:"+(finish-start) + " (ms).");
        } catch (SolrServerException sse) {
            System.err.println(sse.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    public static void reindexBitstreamHits(boolean removeDeletedBitstreams) throws Exception {
        Context context = new Context();

        try {
            //First of all retrieve the total number of records to be updated
            SolrQuery query = new SolrQuery();
            query.setQuery("*:*");
            query.addFilterQuery("type:" + Constants.BITSTREAM);
            //Only retrieve records which do not have a bundle name
            query.addFilterQuery("-bundleName:[* TO *]");
            query.setRows(0);
            long totalRecords = solr.query(query).getResults().getNumFound();

            File tempDirectory = new File(ConfigurationManager.getProperty("dspace.dir") + File.separator + "temp" + File.separator);
            tempDirectory.mkdirs();
            List<File> tempCsvFiles = new ArrayList<File>();
            for(int i = 0; i < totalRecords; i+=10000){
                Map<String, String> params = new HashMap<String, String>();
                params.put(CommonParams.Q, "*:*");
                params.put(CommonParams.FQ, "-bundleName:[* TO *] AND type:" + Constants.BITSTREAM);
                params.put(CommonParams.WT, "csv");
                params.put(CommonParams.ROWS, String.valueOf(10000));
                params.put(CommonParams.START, String.valueOf(i));

                String solrRequestUrl = solr.getBaseURL() + "/select";
                solrRequestUrl = generateURL(solrRequestUrl, params);

                GetMethod get = new GetMethod(solrRequestUrl);
                new HttpClient().executeMethod(get);

                InputStream  csvOutput = get.getResponseBodyAsStream();
                Reader csvReader = new InputStreamReader(csvOutput);
                String[][] csvParsed = CSVParser.parse(csvReader);
                String[] header = csvParsed[0];
                //Attempt to find the bitstream id index !
                int idIndex = 0;
                for (int j = 0; j < header.length; j++) {
                    if(header[j].equals("id")){
                        idIndex = j;
                    }
                }

                File tempCsv = new File(tempDirectory.getPath() + File.separatorChar + "temp." + i + ".csv");
                tempCsvFiles.add(tempCsv);
                FileOutputStream outputStream = new FileOutputStream(tempCsv);
                CSVPrinter csvp = new CSVPrinter(outputStream);
                csvp.setAlwaysQuote(false);

                //Write the header !
                csvp.write(header);
                csvp.write("bundleName");
                csvp.writeln();
                Map<Integer, String> bitBundleCache = new HashMap<Integer, String>();
                //Loop over each line (skip the headers though)!
                for (int j = 1; j < csvParsed.length; j++){
                    String[] csvLine = csvParsed[j];
                    //Write the default line !
                    int bitstreamId = Integer.parseInt(csvLine[idIndex]);
                    //Attempt to retrieve our bundle name from the cache !
                    String bundleName = bitBundleCache.get(bitstreamId);
                    if(bundleName == null){
                        //Nothing found retrieve the bitstream
                        Bitstream bitstream = Bitstream.find(context, bitstreamId);
                        //Attempt to retrieve our bitstream !
                        if (bitstream != null){
                            Bundle[] bundles = bitstream.getBundles();
                            if(bundles != null && 0 < bundles.length){
                                Bundle bundle = bundles[0];
                                bundleName = bundle.getName();
                                context.removeCached(bundle, bundle.getID());
                            }else{
                                //No bundle found, we are either a collection or a community logo, check for it !
                                DSpaceObject parentObject = bitstream.getParentObject();
                                if(parentObject instanceof Collection){
                                    bundleName = "LOGO-COLLECTION";
                                }else
                                if(parentObject instanceof Community){
                                    bundleName = "LOGO-COMMUNITY";
                                }
                                if(parentObject != null){
                                    context.removeCached(parentObject, parentObject.getID());
                                }

                            }
                            //Cache the bundle name
                            bitBundleCache.put(bitstream.getID(), bundleName);
                            //Remove the bitstream from cache
                            context.removeCached(bitstream, bitstreamId);
                        }
                        //Check if we don't have a bundlename
                        //If we don't have one & we do not need to delete the deleted bitstreams ensure that a BITSTREAM_DELETED bundle name is given !
                        if(bundleName == null && !removeDeletedBitstreams){
                            bundleName = "BITSTREAM_DELETED";
                        }
                    }
                    csvp.write(csvLine);
                    csvp.write(bundleName);
                    csvp.writeln();
                }

                //Loop over our parsed csv
                csvp.flush();
                csvp.close();
            }

            //Add all the separate csv files
            for (File tempCsv : tempCsvFiles) {
                ContentStreamUpdateRequest contentStreamUpdateRequest = new ContentStreamUpdateRequest("/update/csv");
                contentStreamUpdateRequest.setParam("stream.contentType", "text/plain;charset=utf-8");
                contentStreamUpdateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
                contentStreamUpdateRequest.addFile(tempCsv);

                solr.request(contentStreamUpdateRequest);
            }

            //Now that all our new bitstream stats are in place, delete all the old ones !
            solr.deleteByQuery("-bundleName:[* TO *] AND type:" + Constants.BITSTREAM);
            //Commit everything to wrap up
            solr.commit(true, true);
            //Clean up our directory !
            FileUtils.deleteDirectory(tempDirectory);
        } catch (Exception e) {
            log.error("Error while updating the bitstream statistics", e);
            throw e;
        } finally {
            context.abort();
        }
    }

    private static String generateURL(String baseURL, Map<String, String> parameters) throws UnsupportedEncodingException {
        boolean first = true;
        StringBuilder result = new StringBuilder(baseURL);
        for (String key : parameters.keySet())
        {
            if (first)
            {
                result.append("?");
                first = false;
            }
            else
            {
                result.append("&");
            }

            result.append(key).append("=").append(URLEncoder.encode(parameters.get(key), "UTF-8"));
        }

        return result.toString();
    }
}

