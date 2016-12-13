/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
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
import org.dspace.app.cris.batch.bte.ImpRecordMetadata;
import org.dspace.app.cris.batch.bte.ImpRecordOutputGenerator;
import org.dspace.app.cris.batch.dao.ImpRecordDAO;
import org.dspace.app.cris.batch.dao.ImpRecordDAOFactory;
import org.dspace.app.cris.batch.dto.DTOImpRecord;
import org.dspace.app.util.XMLUtils;
import org.dspace.authorize.AuthorizeException;
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

public class PMCEuropeFeed
{

    private static final String LAST_MODIFIED = "last_modified";

    private static final String IMP_SOURCE_REF = "imp_sourceref";

    private static final String IMP_RECORD_ID = "imp_record_id";

    private static final String IMP_ITEM_ID = "imp_item_id";

    private static final Logger log = Logger.getLogger(PMCEuropeFeed.class);

    private static final String PMCEUROPE_ENDPOINT_SEARCH = "http://www.ebi.ac.uk/europepmc/webservices/rest/search";

    private static final String PMCEUROPE_ENDPOINT_PDF = "http://europepmc.org/articles/";

    private static final String QUERY_FIXED_PARAM = "format=xml&resulttype=idlist";

    private static final String QUERY_END_DATE = "2999-01-01";

    private static final String QUERY_PAGESIZE = "200";

    private static final String PMCEUROPE_TABLE = "IMP_RECORD_TO_ITEM";

    private static int timeout = 1000;

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    private static HashMap<String, Integer> pmid2item = new HashMap<String, Integer>();

    private static List<String> pmidDeleted = new ArrayList<String>();

    private static String pmidMetadata = "dc.identifier.pmid";

    private static String pmcidMetadata = "dc.identifier.pmcid";
    
    // p = workspace, w = workflow step 1, y = workflow step 2, x =
    // workflow step 3, z = inarchive
    private static String status = "p";

    private static TransformationEngine pubmedFeedTransformationEngine = new DSpace()
            .getServiceManager()
            .getServiceByName("pubmedFeedTransformationEngine",
                    TransformationEngine.class);

    private static TransformationEngine pubmedFeedPhase1TransformationEngine = new DSpace()
            .getServiceManager()
            .getServiceByName("pubmedFeedPhase1TransformationEngine",
                    TransformationEngine.class);

    public static void main(String[] args)
            throws SQLException, BadTransformationSpec, MalformedSourceException, AuthorizeException
    {

        String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
        String proxyPort = ConfigurationManager.getProperty("http.proxy.port");

        System.out.println(proxyHost + " ---- " + proxyPort);

        Context context = new Context();
        getPmid2item(context);

        String endpoint = ConfigurationManager.getProperty("pmceuropefeed",
                "rest.endpoint");
        if (!StringUtils.isNotBlank(endpoint))
        {
            endpoint = PMCEUROPE_ENDPOINT_SEARCH;
        }

        String queryParam = ConfigurationManager.getProperty("pmceuropefeed",
                "query.param.default");
        String usage = "org.dspace.app.cris.batch.PMCEuropeFeed -q queryPMC -p submitter -s start_date(YYYY-MM-DD) -e end_date(YYYY-MM-DD) -c collectionID";

        HelpFormatter formatter = new HelpFormatter();

        Options options = new Options();
        CommandLine line = null;

        options.addOption(
                OptionBuilder.withArgName("query Parameters").hasArg(true)
                        .withDescription(
                                "Query to retrieve data publications from PMC Europe, default query in pmceuropefeed.cfg")
                .create("q"));

        options.addOption(
                OptionBuilder.withArgName("query Start Date").hasArg(true)
                        .withDescription(
                                "Query start date to retrieve data publications from PMC Europe, default start date is the last run of the script")
                .create("s"));

        options.addOption(
                OptionBuilder.withArgName("query End Date").hasArg(true)
                        .withDescription(
                                "Query End date to retrieve data publications from PMC Europe, default is 2999-12-31")
                .create("e"));

        options.addOption(OptionBuilder.isRequired(true)
                .withArgName("collectionID").hasArg(true)
                .withDescription("Collection for item submision").create("c"));

        options.addOption(OptionBuilder.withArgName("test").hasArg(false)
                .withDescription("dry run, do not save any change")
                .create("t"));

        options.addOption(OptionBuilder.isRequired(true).withArgName("Eperson")
                .hasArg(true).withDescription("Submitter of the records")
                .create("p"));
        
        options.addOption(OptionBuilder.withArgName("identifierpmid")
                .hasArg(true).withDescription("Name of the metadata for pubmed id")
                .create("m"));
        
        options.addOption(OptionBuilder.withArgName("identifierpmcid")
                .hasArg(true).withDescription("Name of the metadata for pubmed central id")
                .create("n"));

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

        String person = line.getOptionValue("p");
    	EPerson eperson = null;
        if(StringUtils.isNumeric(person)){
        	eperson = EPerson.find(context,
                    Integer.parseInt(person));
        	
        }else {
        	eperson = EPerson.findByEmail(context, person);
        }
        
        
        if(eperson != null){
        	context.setCurrentUser(eperson);
        }else{
            formatter.printHelp(usage, "No user found", options, "");
            System.exit(1);
        }
       
        int collection_id = Integer.parseInt(line.getOptionValue("c"));

        String startDate = "";
        if (line.hasOption("s"))
        {
            startDate = line.getOptionValue("s");
        }
        else
        {
            TableRow row = DatabaseManager.querySingle(context,
                    "SELECT max(impr.last_modified) as LAST_MODIFIED from IMP_RECORD_TO_ITEM imprti join IMP_RECORD impr on "
                    + "imprti.imp_record_id = impr.imp_record_id and imprti."+IMP_SOURCE_REF+" like 'pubmedEurope'");
            Date date = row.getDateColumn("LAST_MODIFIED");
            if (date == null)
            {
                date = new Date();
            }
            startDate = df.format(date);
        }

        String endDate = QUERY_END_DATE;
        if (line.hasOption("e"))
        {
            endDate = line.getOptionValue("e");
        }
        String queryParamDate = " UPDATE_DATE:[" + startDate + " TO " + endDate
                + "]";

        if (line.hasOption("q"))
        {
            queryParam = line.getOptionValue("q");
            queryParamDate = "";
        }

        String query = queryParam + queryParamDate;

        String pagesize = ConfigurationManager.getProperty("pmceuropefeed",
                "query.pagesize");
        if (!StringUtils.isNotBlank(pagesize))
        {
            pagesize = QUERY_PAGESIZE;
        }

        if (line.hasOption("m"))
        {
            pmidMetadata = line.getOptionValue("m");
        }
        if (line.hasOption("n"))
        {
            pmcidMetadata = line.getOptionValue("n");
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

        try
        {
            int page = 1;
            List<String> pmidList = new ArrayList<String>();

            HttpClient client = new DefaultHttpClient();
            client.getParams().setIntParameter(
                    CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
            URIBuilder uriBuilder = new URIBuilder(endpoint);
            uriBuilder.addParameter("format", "xml");
            uriBuilder.addParameter("resulttype", "idlist");
            uriBuilder.addParameter("query", query);
            uriBuilder.addParameter("pageSize", pagesize);
            uriBuilder.addParameter("page", Integer.toString(page));
            boolean lastPage = false;

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

            while (!lastPage)
            {
                uriBuilder.setParameter("page", Integer.toString(page));
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
                    Element resList = XMLUtils.getSingleElement(xmlRoot,
                            "resultList");
                    List<Element> res = XMLUtils.getElementList(resList,
                            "result");
                    if (!res.isEmpty())
                    {
                        pmidList.addAll(getPmidList(res));
                        page++;
                    }
                    else
                    {
                        lastPage = true;
                    }
                }
                catch (ParserConfigurationException e1)
                {
                    log.error(e1.getMessage(), e1);
                }
                catch (SAXException e1)
                {
                    log.error(e1.getMessage(), e1);
                    lastPage = true;
                }
                finally
                {

                }
            }

            System.out.println("Retrieved " + pmidList.size() + " record");
            List<List<String>> chunks = new ArrayList<List<String>>();

            for (int i = 0; i < pmidList.size(); i += 50)
            {
                List<String> chunk = pmidList.subList(i,
                        Math.min(pmidList.size(), i + 50));
                chunks.add(chunk);
            }

            int total = 0;

            int deleted = 0;
            int imported = 0;
            List<ImpRecordItem> pmeItemList = new ArrayList<ImpRecordItem>();

            ImpRecordDAO dao = ImpRecordDAOFactory.getInstance(context);
            for (List<String> ch : chunks)
            {
                int size = ch.size();

                for (String pubmedID : ch)
                {
                    if (pmidDeleted.contains(pubmedID))
                    {
                        deleted++;
                        continue;
                    }
                    pmeItemList.addAll(convertToPMEItem(pubmedID));
                }
                for (ImpRecordItem pmeItem : pmeItemList)
                {
                    String sourceId = pmeItem.getSourceId();
                    String action = "insert";
                    if (pmid2item.containsKey(sourceId))
                    {
                        action = "update";
                    }
                    DTOImpRecord impRecord = writeImpRecord(context, dao,
                            collection_id, pmeItem, action, eperson.getID());
                    Set<ImpRecordMetadata> metadata = pmeItem.getMetadata()
                            .get(pmcidMetadata);
                    if (metadata != null && !metadata.isEmpty())
                    {
                        retrieveAndUploadPdf(context, impRecord,
                                metadata.iterator().next().getValue());
                    }

                    dao.write(impRecord, false);
                }
                context.commit();
                total += size;
                System.out.println("Imported " + (total - deleted) + " record; "
                        + deleted + " marked as removed");
                pmeItemList.clear();
                context.clearCache();
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

        HashMap<String, Set<ImpRecordMetadata>> meta = pmeItem.getMetadata();
        for (String md : meta.keySet())
        {
            Set<ImpRecordMetadata> values = meta.get(md);
            String[] splitMd = StringUtils.split(md, "\\.");
            int metadata_order = 0;
            for (ImpRecordMetadata value : values)
            {
                metadata_order++;
                if (splitMd.length > 2)
                {
                    dto.addMetadata(splitMd[0], splitMd[1], splitMd[2], value.getValue(),
                            value.getAuthority(), value.getConfidence(), metadata_order, -1);
                }
                else
                {
                    dto.addMetadata(splitMd[0], splitMd[1], null, value.getValue(), value.getAuthority(),
                            value.getConfidence(), metadata_order, value.getShare());
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

    private static List<ImpRecordItem> convertToPMEItem(String pmid)
            throws BadTransformationSpec, MalformedSourceException
    {
        List<ImpRecordItem> pmeResult = new ArrayList<ImpRecordItem>();

        TransformationEngine transformationEngine1 = getPubmedFeedPhase1TransformationEngine();
        List<ItemSubmissionLookupDTO> result = new ArrayList<ItemSubmissionLookupDTO>();
        if (transformationEngine1 != null)
        {
            HashMap<String, Set<String>> map = new HashMap<String, Set<String>>();
            HashSet<String> set = new HashSet<String>();
            set.add(pmid);
            map.put("pubmed", set);

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
        TransformationEngine transformationEngine2 = getPubmedFeedTransformationEngine();
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

    private static List<String> getPmidList(List<Element> idList)
    {
        List<String> ids = new ArrayList<String>();
        for (Element idElement : idList)
        {
            Element pmidElement = XMLUtils.getSingleElement(idElement, "pmid");
            if (pmidElement != null)
            {
                ids.add(pmidElement.getTextContent());
            }
        }
        return ids;
    }

    private static void getPmid2item(Context context) throws SQLException
    {

        String query = "SELECT " + IMP_RECORD_ID + ", " + IMP_ITEM_ID + " FROM "
                + PMCEUROPE_TABLE + " WHERE " + IMP_SOURCE_REF
                + " = 'pubmedEurope'";
        TableRowIterator tri = null;
        try
        {
            tri = DatabaseManager.query(context, query);
            while (tri.hasNext())
            {
                TableRow tr = tri.next();
                String pmID = tr.getStringColumn(IMP_RECORD_ID);
                int itemID = tr.getIntColumn(IMP_ITEM_ID);
                Item item = Item.find(context, itemID);
                if (item != null)
                {
                    pmid2item.put(pmID, itemID);
                }
                else
                {
                    pmidDeleted.add(pmID);
                }
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

    private static void retrieveAndUploadPdf(Context context,
            DTOImpRecord impRecord, String pmcID) throws SQLException
    {

        URL url;
        try
        {
            url = new URL(PMCEUROPE_ENDPOINT_PDF + pmcID + "?pdf=render");
            InputStream is = url.openStream();

            impRecord.addBitstream(context, is, null, true, 1, 0, "ORIGINAL",
                    -1, null, "pubmedEurope-" + pmcID, "application/pdf");
        }
        catch (MalformedURLException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }
    }

    public static TransformationEngine getPubmedFeedTransformationEngine()
    {
        return pubmedFeedTransformationEngine;
    }

    public static TransformationEngine getPubmedFeedPhase1TransformationEngine()
    {
        return pubmedFeedPhase1TransformationEngine;
    }

}
