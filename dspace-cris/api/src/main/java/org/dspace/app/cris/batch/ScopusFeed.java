package org.dspace.app.cris.batch;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;
import org.dspace.app.cris.batch.bte.ImpRecordItem;
import org.dspace.app.cris.batch.bte.ImpRecordOutputGenerator;
import org.dspace.app.cris.batch.dao.ImpRecordDAO;
import org.dspace.app.cris.batch.dao.ImpRecordDAOFactory;
import org.dspace.app.cris.batch.dto.DTOImpRecord;
import org.dspace.app.util.XMLUtils;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.submit.lookup.MultipleSubmissionLookupDataLoader;
import org.dspace.submit.lookup.SubmissionItemDataLoader;
import org.dspace.submit.lookup.SubmissionLookupOutputGenerator;
import org.dspace.submit.util.ItemSubmissionLookupDTO;
import org.dspace.utils.DSpace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.core.TransformationSpec;
import gr.ekt.bte.exceptions.BadTransformationSpec;
import gr.ekt.bte.exceptions.MalformedSourceException;

public class ScopusFeed
{

    private static final String LAST_MODIFIED = "last_modified";

    private static final String IMP_SOURCE_REF = "imp_sourceref";

    private static final String IMP_RECORD_ID = "imp_record_id";

    private static final String IMP_ITEM_ID = "imp_item_id";

    private static final Logger log = Logger.getLogger(ScopusFeed.class);

    private static final String ENDPOINT_SEARCH = "http://api.elsevier.com/content/search/scopus";

    private static final String QUERY_END_DATE = "2999-01-01";

    private static final String QUERY_PAGESIZE = "25";

    private static final String IMP_RECORDTOITEM_TABLE = "IMP_RECORD_TO_ITEM";

    private static int timeout = 1000;

    private static DateFormat df = new SimpleDateFormat("yyyy");

    private static List<String> eidList = new ArrayList<String>();
    
    private static HashMap<String,String> type2collection = new HashMap<String,String>();    

    private static String eidMetadata = "dc.identifier.eid";

    private static String apiKey = ConfigurationManager.getProperty("submission.lookup.scopus.apikey");    

    // p = workspace, w = workflow step 1, y = workflow step 2, x =
    // workflow step 3, z = inarchive
    private static String status = "p";

    private static TransformationEngine scopusFeedTransformationEngine = new DSpace()
            .getServiceManager()
            .getServiceByName("scopusFeedTransformationEngine",
                    TransformationEngine.class);

    private static TransformationEngine scopusFeedPhase1TransformationEngine = new DSpace()
            .getServiceManager()
            .getServiceByName("scopusFeedPhase1TransformationEngine",
                    TransformationEngine.class);

    public static void main(String[] args)
            throws SQLException, BadTransformationSpec, MalformedSourceException
    {

        String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
        String proxyPort = ConfigurationManager.getProperty("http.proxy.port");
        

        System.out.println(proxyHost + " ---- " + proxyPort);

        Context context = new Context();
        geteidList(context);


        String endpoint = ConfigurationManager.getProperty("scopus",
                "rest.endpoint");
        if (!StringUtils.isNotBlank(endpoint))
        {
            endpoint = ENDPOINT_SEARCH;
        }

        String queryParam = ConfigurationManager.getProperty("scopus",
                "query.param.default");
        String usage = "org.dspace.app.cris.batch.ScopusFeed -q query -p submitter -s start_date(YYYY) -e end_date(YYYY) -c collectionID";

        HelpFormatter formatter = new HelpFormatter();

        Options options = new Options();
        CommandLine line = null;

        options.addOption(
                OptionBuilder.withArgName("query Parameters").hasArg(true)
                        .withDescription(
                                "Query to retrieve data publications from Scopus, default query in scopus.cfg")
                .create("q"));

        options.addOption(
                OptionBuilder.withArgName("query Start Date").isRequired(true).hasArg(true)
                        .withDescription(
                                "Year parameter to retrieve data publications from Scopus")
                .create("s"));

        options.addOption(
                OptionBuilder.withArgName("query End Date").hasArg(true)
                        .withDescription(
                                "Query End date to retrieve data publications from Scopus")
                .create("e"));

        options.addOption(OptionBuilder.isRequired(true)
                .withArgName("collectionID").hasArg(true)
                .withDescription("Default collection for import").create("c"));

        options.addOption(OptionBuilder.withArgName("test").hasArg(false)
                .withDescription("dry run, do not save any change")
                .create("t"));

        options.addOption(OptionBuilder.isRequired(true).withArgName("Eperson")
                .hasArg(true).withDescription("Submitter of the records")
                .create("p"));
        
        options.addOption(OptionBuilder.withArgName("identifiereid")
                .hasArg(true).withDescription("Name of the metadata for eid")
                .create("m"));
        
        options.addOption(OptionBuilder.withArgName("status")
                .hasArg(true).withDescription("Status of new item p = workspace, w = workflow step 1, y = workflow step 2, x = workflow step 3, z = inarchive")
                .create("o"));
        try
        {
            line = new PosixParser().parse(options, args);
        }
        catch (ParseException e)
        {
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }

        if (!line.hasOption("c") || !line.hasOption("p"))
        {
            formatter.printHelp(usage, "", options, "");
            System.exit(1);
        }

        EPerson eperson = EPerson.find(context,
                Integer.parseInt(line.getOptionValue("p")));
        context.setCurrentUser(eperson);

        int coll_id = Integer.parseInt(line.getOptionValue("c"));

        String startDate = "";
        if (line.hasOption("s"))
        {
            startDate = line.getOptionValue("s");
        }

        String queryParamDate = "date=" + startDate;        
        
        String endDate = "";
        if (line.hasOption("e"))
        {
            endDate = line.getOptionValue("e");
            queryParamDate+="-"+endDate;
        }

        if (line.hasOption("q"))
        {
            queryParam = line.getOptionValue("q");
        }

        String query = queryParam;

        String pagesize = ConfigurationManager.getProperty("scopuse",
                "query.pagesize");
        if (!StringUtils.isNotBlank(pagesize))
        {
            pagesize = QUERY_PAGESIZE;
        }

        if (line.hasOption("z"))
        {
            query = ConfigurationManager.getProperty("scopus",
                    "query.param.scratch");
        }

        
        if (line.hasOption("m"))
        {
            eidMetadata = line.getOptionValue("m");
        }

        if (line.hasOption("0"))
        {
            status = line.getOptionValue("o");
        }
                
        HttpGet method = null;
        HttpHost proxy = null;
        log.info("Starting query with parameter start date:" + startDate
                + "; end date:" + endDate + "; page size:" + pagesize);
        System.out.println("Starting query");

        HashMap< String, List<String>> type2eid = new HashMap<String, List<String>>();
        try
        {
            int start = 0;

            HttpClient client = new DefaultHttpClient();
            client.getParams().setIntParameter(
                    CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
            URIBuilder uriBuilder = new URIBuilder(endpoint);
            uriBuilder.addParameter("apiKey", apiKey);
            uriBuilder.addParameter("view", "COMPLETE");            
            uriBuilder.addParameter("field", "eid,subtype");
            uriBuilder.addParameter("query", query);
            uriBuilder.addParameter("start", Integer.toString(start));
            boolean lastPageReached = false;

            if (StringUtils.isNotBlank(proxyHost)
                    && StringUtils.isNotBlank(proxyPort))
            {
                proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort),
                        "http");
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
                        proxy);
                System.out.println(client.getParams()
                        .getParameter(ConnRoutePNames.DEFAULT_PROXY));
            }

            while (!lastPageReached)
            {
                uriBuilder.setParameter("start", Integer.toString(start));
                method = new HttpGet(uriBuilder.build());

                // Execute the method.
                HttpResponse response = client.execute(method);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();

                if (statusCode != HttpStatus.SC_OK)
                {
                    throw new RuntimeException("WS call failed: " + statusLine);
                }

                System.out.println("STATUS RESPONSE:" + statusCode);
                DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                factory.setValidating(false);
                factory.setIgnoringComments(true);
                factory.setIgnoringElementContentWhitespace(true);
                DocumentBuilder builder;
                try
                {

                    builder = factory.newDocumentBuilder();
                    Document inDoc = builder
                            .parse(response.getEntity().getContent());
                    inDoc.normalize();
                    Element xmlRoot = inDoc.getDocumentElement();
                    List<Element> pages = XMLUtils.getElementList(xmlRoot,
            				"link");                    
                    lastPageReached=true;
                    for(Element page: pages){
                    	String refPage = page.getAttribute("ref");
                    	if(StringUtils.equalsIgnoreCase(refPage, "next")){
                    		lastPageReached= false;
                    		break;
                    	}
                    }
            		List<Element> pubArticles = XMLUtils.getElementList(xmlRoot,
            				"entry");

            		for (Element entry : pubArticles)
            		{
            			String eid = XMLUtils.getElementValue(entry, "eid");
            			if(eidList.contains(eid)){
            			   continue;
            			}
            			String type = "default";
            			String scopusType = XMLUtils.getElementValue(entry, "subtype");
            			if(StringUtils.isNotBlank(scopusType)){
            				type=scopusType;
            			}
            			
            			List<String> eids = new ArrayList<String>();
            			if(type2eid.containsKey(type)){
            				eids = type2eid.get(type);
            			}
            			eids.add(eid);
            			type2eid.put(type, eids);
            		}
                    
                }
                catch (ParserConfigurationException e1)
                {
                    log.error(e1.getMessage(), e1);
                }
                catch (SAXException e1)
                {
                    log.error(e1.getMessage(), e1);
                    lastPageReached= true;
                }
            }

            //System.out.println("Retrieved " + eidList.size() + " record");

            Set<String> types = type2eid.keySet();
            for(String t: types){
            	int collection_id=coll_id;
            	List<List<String>> chunks = new ArrayList<List<String>>();
                List<String> eidList = type2eid.get(t);
                
                String collID = ConfigurationManager.getProperty("scopus","scopus.type"+t+".collectionid");
                if(StringUtils.isNotBlank(collID)){
                	collection_id = Integer.parseInt(collID);
                }
                
            	for (int i = 0; i < eidList.size(); i += 50)
            	{
            		List<String> chunk = eidList.subList(i,
            				Math.min(eidList.size(), i + 50));
            		chunks.add(chunk);
            	}
	
	            int total = 0;
	            int imported = 0;
	            List<ImpRecordItem> impItemList = new ArrayList<ImpRecordItem>();
	
	            ImpRecordDAO dao = ImpRecordDAOFactory.getInstance(context);
	            for (List<String> ch : chunks)
	            {
	                int size = ch.size();
	
	                for (String eID : ch)
	                {
	                    impItemList.addAll(convertToImpItem(eID));
	                }
	                for (ImpRecordItem pmeItem : impItemList)
	                {
	                    String sourceId = pmeItem.getSourceId();
	                    String action = "insert";
	                    DTOImpRecord impRecord = writeImpRecord(context, dao,
	                            collection_id, pmeItem, action, eperson.getID());
	                    Set<String> metadata = pmeItem.getMetadata()
	                            .get(eidMetadata);
	
	                    dao.write(impRecord, false);
	                }
	                context.commit();
	                total += size;
	                System.out.println("Imported " + total  + " record; ");
	                impItemList.clear();
	                context.clearCache();
	            }
            }

            context.complete();

        }
        catch (URISyntaxException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (method != null)
            {
                method.releaseConnection();
            }
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

    }

    private static DTOImpRecord writeImpRecord(Context context,
            ImpRecordDAO dao, int collection_id, ImpRecordItem pmeItem,
            String action, Integer epersonId) throws SQLException
    {
        DTOImpRecord dto = new DTOImpRecord(dao);

        HashMap<String, Set<String>> meta = pmeItem.getMetadata();
        for (String md : meta.keySet())
        {
            Set<String> values = meta.get(md);
            String[] splitMd = StringUtils.split(md, "\\.");
            int metadata_order = 0;
            for (String value : values)
            {
                metadata_order++;
                if (splitMd.length > 2)
                {
                    dto.addMetadata(splitMd[0], splitMd[1], splitMd[2], value,
                            null, -1, metadata_order, -1);
                }
                else
                {
                    dto.addMetadata(splitMd[0], splitMd[1], null, value, null,
                            -1, metadata_order, -1);
                }
            }
        }

        dto.setImp_collection_id(collection_id);
        dto.setImp_eperson_id(epersonId);
        dto.setOperation(action);
        dto.setImp_sourceRef(pmeItem.getSourceRef());
        dto.setImp_record_id(pmeItem.getSourceId());
        dto.setStatus(status);
        return dto;

    }

    private static List<ImpRecordItem> convertToImpItem(String eid)
            throws BadTransformationSpec, MalformedSourceException
    {
        List<ImpRecordItem> pmeResult = new ArrayList<ImpRecordItem>();

        TransformationEngine transformationEngine1 = getScopusFeedPhase1TransformationEngine();
        List<ItemSubmissionLookupDTO> result = new ArrayList<ItemSubmissionLookupDTO>();
        if (transformationEngine1 != null)
        {
            HashMap<String, Set<String>> map = new HashMap<String, Set<String>>();
            HashSet<String> set = new HashSet<String>();
            set.add(eid);
            map.put("scopus", set);

            MultipleSubmissionLookupDataLoader mdataLoader = (MultipleSubmissionLookupDataLoader) transformationEngine1
                    .getDataLoader();
            mdataLoader.setIdentifiers(map);
            SubmissionLookupOutputGenerator outputGenerator = (SubmissionLookupOutputGenerator) transformationEngine1
                    .getOutputGenerator();
            outputGenerator
                    .setDtoList(new ArrayList<ItemSubmissionLookupDTO>());

            transformationEngine1.transform(new TransformationSpec());
            log.debug("BTE transformation finished!");
            result = outputGenerator.getDtoList();
        }
        TransformationEngine transformationEngine2 = getScopusFeedTransformationEngine();
        if (transformationEngine2 != null)
        {
            SubmissionItemDataLoader dataLoader = (SubmissionItemDataLoader) transformationEngine2
                    .getDataLoader();
            dataLoader.setDtoList(result);

            ImpRecordOutputGenerator outputGenerator = (ImpRecordOutputGenerator) transformationEngine2
                    .getOutputGenerator();
            transformationEngine2.transform(new TransformationSpec());
            pmeResult = outputGenerator.getRecordIdItems();
        }

        return pmeResult;
    }

    private static List<String> getEidList(List<Element> idList)
    {
        List<String> ids = new ArrayList<String>();
        for (Element idElement : idList)
        {
            Element eidElement = XMLUtils.getSingleElement(idElement, "eid");
            if (eidElement != null)
            {
                ids.add(eidElement.getTextContent());
            }
        }
        return ids;
    }

    private static void geteidList(Context context) throws SQLException
    {

        String query = "SELECT " + IMP_RECORD_ID + " FROM "
                + IMP_RECORDTOITEM_TABLE + " WHERE " + IMP_SOURCE_REF
                + " = 'scopus'";
        TableRowIterator tri = null;
        try
        {
            tri = DatabaseManager.query(context, query);
            while (tri.hasNext())
            {
                TableRow tr = tri.next();
                String eID = tr.getStringColumn(IMP_RECORD_ID);
                eidList.add(eID);
            }
        }
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }
    }


    public static TransformationEngine getScopusFeedTransformationEngine()
    {
        return scopusFeedTransformationEngine;
    }

    public static TransformationEngine getScopusFeedPhase1TransformationEngine()
    {
        return scopusFeedPhase1TransformationEngine;
    }

}
