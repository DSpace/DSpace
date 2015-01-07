/**
 * $Id: SolrLogger.java 4959 2010-05-15 05:22:06Z mdiggory $
 * $URL: https://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/SolrLogger.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.MapSolrParams;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.util.DnsLookup;
import org.dspace.statistics.util.LocationUtils;
import org.dspace.statistics.util.SpiderDetector;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Static SolrLogger used to hold HttpSolrClient connection pool to issue
 * usage logging events to Solr from DSpace libraries.
 * 
 * @author ben at atmire.com
 * @author kevinvandevelde at atmire.com
 * @author mdiggory at atmire.com
 */
public class SolrLogger
{
	
	private static Logger log = Logger.getLogger(SolrLogger.class);
	
    private static final CommonsHttpSolrServer solr;

    public static final String DATE_FORMAT_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT_DCDATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final LookupService locationService;

    private static final boolean useProxies;

    private static Map metadataStorageInfo;
    private static int count = 0;
    private static int commit = 10;

    private static final String REFERRER_SOLR_FIELD = "referrer";
    private static final String REFERRER_HTTP_HEADER = "referer";

    static
    {
    	log.info("solr.spidersfile:" + ConfigurationManager.getProperty("solr.spidersfile"));
		log.info("solr.log.server:" + ConfigurationManager.getProperty("solr.log.server"));
		log.info("solr.dbfile:" + ConfigurationManager.getProperty("solr.dbfile"));
    	
        CommonsHttpSolrServer server = null;
        
        if (ConfigurationManager.getProperty("solr.log.server") != null)
        {
            try
            {
                server = new CommonsHttpSolrServer(ConfigurationManager.getProperty("solr.log.server"));
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
        String dbfile = ConfigurationManager.getProperty("solr.dbfile");
        if (dbfile != null)
        {
            try
            {
                service = new LookupService(dbfile,
                        LookupService.GEOIP_STANDARD);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            // System.out.println("NO SOLR DB FILE !");
        }
        locationService = service;

        if ("true".equals(ConfigurationManager.getProperty("useProxies")))
            useProxies = true;
        else
            useProxies = false;

        log.info("useProxies=" + useProxies);

        metadataStorageInfo = new HashMap();
        int count = 1;
        String metadataVal;
        while ((metadataVal = ConfigurationManager.getProperty("solr.metadata.item." + count)) != null)
        {
            String storeVal = metadataVal.split(":")[0];
            String metadataField = metadataVal.split(":")[1];

            metadataStorageInfo.put(storeVal, metadataField);
            log.info("solr.metadata.item." + count + "=" + metadataVal);
            count++;
        }

        if (ConfigurationManager.getProperty("solr.statistics.interval") != null)
            commit = ConfigurationManager.getIntProperty("solr.statistics.interval");
    }

    public static void post(DSpaceObject dspaceObject, HttpServletRequest request,
            EPerson currentUser)
    {
        if (solr == null || locationService == null)
            return;

        boolean isSpiderBot = SpiderDetector.isSpider(request);

        String ignorablePersons = ConfigurationManager.getProperty("solr.statistics.eperson.isIgnorable");
        HashSet<String> epersons = new HashSet<String>();

        if (ignorablePersons != null) {
        	if (ignorablePersons.contains(";")) {
        		for (String personID : ignorablePersons.split(";")) {
        			epersons.add(personID.trim());
        		}
        	}
        	else {
        		epersons.add(ignorablePersons.trim());
        	}
        }
        
        try
        {
            if(isSpiderBot &&
                    !ConfigurationManager.getBooleanProperty("solr.statistics.logBots",true))
            {
                return;
            }


                        
            SolrInputDocument doc1 = new SolrInputDocument();
            // Save our basic info that we already have

            final String originalReferrer = request.getHeader(REFERRER_HTTP_HEADER);
            log.info("SolrLogger - referrer: " + originalReferrer);
            final Boolean reviewTokenPresent = SolrLoggerUtils.isReviewTokenPresent(originalReferrer);
            final Boolean reviewDOIPresent = SolrLoggerUtils.isReviewDOIPresent(originalReferrer);
            if(originalReferrer!=null) {
                String cleanedReferrer = originalReferrer;
                if(reviewTokenPresent) {
                    cleanedReferrer = SolrLoggerUtils.replaceReviewToken(cleanedReferrer, SolrLoggerUtils.DUMMY_TOKEN);
                }
                if(reviewDOIPresent) {
                    cleanedReferrer = SolrLoggerUtils.replaceReviewDOI(cleanedReferrer, SolrLoggerUtils.DUMMY_DOI);
                }
                doc1.addField(REFERRER_SOLR_FIELD, cleanedReferrer);
            }

            String ip = request.getRemoteAddr();
	    log.debug("IP is " + ip);
	    String xff = request.getHeader("X-Forwarded-For");
	    if(isUseProxies() &&  xff != null)
		{
		    log.debug("getting proxy IP from X-Forwarded-For:" + xff); 
		    
		    /* This header is a comma delimited list */
		    for(String xfip : xff.split(","))
			{
			    /* proxy itself will sometime populate this header with the same value in
			       remote address. ordering in spec is vague, we'll just take the last
			       not equal to the proxy
			    */
			    if(!xff.contains(ip))
				{
				    ip = xfip.trim();
				}
			}
	        }

            // Don't record IP addresses for review links
            if(!reviewTokenPresent) {
                doc1.addField("ip", ip);
            }


            doc1.addField("id", dspaceObject.getID());
            doc1.addField("type", dspaceObject.getType());
            // Save the current time
            doc1.addField("time", DateFormatUtils.format(new Date(),
                    DATE_FORMAT_8601));
            if (currentUser != null) {
            	int userID = currentUser.getID();
            	
                doc1.addField("epersonid", userID);

                if (epersons.contains(Integer.toString(userID))) {
                	return;
                }
            }

            try
            {
		if(ip.equals("::1")) {
		    doc1.addField("dns", "localhost");
		} else {
                    // Don't record DNS addresses for review links
                    if(!reviewTokenPresent) {
                        //
                        String dns = DnsLookup.reverseDns(ip);
                        doc1.addField("dns", dns.toLowerCase());
                    }
		}
            }
            catch (Exception e)
            {
		log.warn("Failed DNS Lookup for IP " + ip, e);
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
                    log.error("COUNTRY ERROR: " + location.countryCode, e);
                }
                doc1.addField("countryCode", location.countryCode);
                doc1.addField("city", location.city);
                doc1.addField("latitude", location.latitude);
                doc1.addField("longitude", location.longitude);
                doc1.addField("isBot",isSpiderBot);

                if(request.getHeader("User-Agent") != null)
                    doc1.addField("userAgent", request.getHeader("User-Agent"));
            }
            
            if (dspaceObject instanceof Item)
            {
                Item item = (Item) dspaceObject;
                // Store the metadata
                for (Object storedField : metadataStorageInfo.keySet())
                {
                    String dcField = (String) metadataStorageInfo
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


            storeParents(doc1, dspaceObject);

            solr.add(doc1);
            count++;
            if (commit > 0 && count % commit == 0)
                solr.commit(false, false);
   
        } catch (Exception e) {
        	log.error(e.getMessage(), e);
        }
    }

    public static Map getMetadataStorageInfo()
    {
        return metadataStorageInfo;
    }

    /**
     * Method just used to log the parents Community log: owning comms
     * Collection log: owning comms & their comms Item log: owning colls/comms
     * Bitstream log: owning item/colls/comms
     * 
     * @param doc1
     *            the current solrinputdoc
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
                return currentValsStored;

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
         * Overide to manage individual documents
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


    /**
     * Removes ip/dns and scrubs review link from solr for items downloaded in review
     */
    public static void deleteReviewIdentifiers() throws SolrServerException, IOException {
        final List<SolrInputDocument> documents = new ArrayList<SolrInputDocument>();
        ResultProcessor processor = new ResultProcessor() {
            @Override
            public void process(SolrDocument doc) throws IOException, SolrServerException {
                doc.removeFields("ip");
                doc.removeFields("dns");
                // Remove the original referrer
                final String originalReferrer = doc.getFieldValue("referrer").toString();
                final String cleanedReferrer = SolrLoggerUtils.replaceReviewToken(originalReferrer, SolrLoggerUtils.DUMMY_TOKEN);
                doc.setField(REFERRER_SOLR_FIELD, cleanedReferrer);
                SolrInputDocument newInput = ClientUtils.toSolrInputDocument(doc);
                documents.add(newInput);
            }
        };
        String query = REFERRER_SOLR_FIELD + ":http*token=*";
        processor.execute(query);
        solr.deleteByQuery(query);
        solr.add(documents);
        solr.commit();
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
        long start = new Date().getTime();
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
                        solrDocument.removeFields(fieldName);

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
     * Query used to get values grouped by the given facetfield
     *
     * @param query
     *            the query to be used
     * @param facetField
     *            the facet field on which to group our values
     * @param max
     *            the max number of values given back (in case of 10 the top 10
     *            will be given)
     * @param showTotal
     *            a boolean determening whether the total amount should be given
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
            return new ObjectCount[0];

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
     * Query used to get values grouped by the date
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
     *            a boolean determening whether the total amount should be given
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
            return new ObjectCount[0];

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
                name = simpleFormat.format(date);

        }
        return name;
    }

    private static QueryResponse query(String query, String filterQuery,
            String facetField, int max, String dateType, String dateStart,
            String dateEnd, List<String> facetQueries)
            throws SolrServerException
    {
        if (solr == null)
            return null;

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
                solrQuery.setFacet(true);
        }

        if (facetField != null)
            solrQuery.addFacetField(facetField);

        // Set the top x of if present
        if (max != -1)
            solrQuery.setFacetLimit(max);

        // A filter is used instead of a regular query to improve
        // performance and ensure the search result ordering will
        // not be influenced

        // Choose to filter by the Legacy spider IP list (may get too long to properly filter all IP's
        if(ConfigurationManager.getBooleanProperty("solr.statistics.query.filter.spiderIp",false))
            solrQuery.addFilterQuery(getIgnoreSpiderIPs());

        // Choose to filter by isBot field, may be overriden in future
        // to allow views on stats based on bots.
        if(ConfigurationManager.getBooleanProperty("solr.statistics.query.filter.isBot",true))
            solrQuery.addFilterQuery("-isBot:true");

        if (filterQuery != null)
            solrQuery.addFilterQuery(filterQuery);

        QueryResponse response = null;
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
            String query = "";
            boolean first = true;
            for (String ip : SpiderDetector.getSpiderIpAddresses()) {
                if (first) {
                    query += " AND ";
                    first = false;
                }

                query += " NOT(ip: " + ip + ")";
            }
            filterQuery = query;
        }

        return filterQuery;

    }
    
}
