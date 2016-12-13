/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.batch.bte.ImpRecordItem;
import org.dspace.app.cris.batch.bte.ImpRecordMetadata;
import org.dspace.app.cris.batch.bte.ImpRecordOutputGenerator;
import org.dspace.app.cris.batch.dao.ImpRecordDAO;
import org.dspace.app.cris.batch.dao.ImpRecordDAOFactory;
import org.dspace.app.cris.batch.dto.DTOImpRecord;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.submit.lookup.MultipleSubmissionLookupDataLoader;
import org.dspace.submit.lookup.SubmissionItemDataLoader;
import org.dspace.submit.lookup.SubmissionLookupOutputGenerator;
import org.dspace.submit.lookup.WOSOnlineDataLoader;
import org.dspace.submit.util.ItemSubmissionLookupDTO;
import org.dspace.utils.DSpace;

import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.core.TransformationSpec;
import gr.ekt.bte.exceptions.BadTransformationSpec;
import gr.ekt.bte.exceptions.MalformedSourceException;

public class WosFeed
{

    private static final Logger log = Logger.getLogger(WosFeed.class);

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    // p = workspace, w = workflow step 1, y = workflow step 2, x =
    // workflow step 3, z = inarchive
    private static String status = "y";

    private static TransformationEngine feedTransformationEnginePhaseOne = new DSpace()
            .getServiceManager()
            .getServiceByName("wosFeedTransformationEnginePhaseOne",
                    TransformationEngine.class);
    
    private static TransformationEngine feedTransformationEnginePhaseTwo = new DSpace()
            .getServiceManager()
            .getServiceByName("wosFeedTransformationEnginePhaseTwo",
                    TransformationEngine.class);

    private static WOSOnlineDataLoader wosOnlineDataLoader = new DSpace()
            .getServiceManager()
            .getServiceByName("wosOnlineDataLoader", WOSOnlineDataLoader.class);

    public static void main(String[] args)
            throws SQLException, BadTransformationSpec,
            MalformedSourceException, HttpException, IOException, AuthorizeException
    {

        String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
        String proxyPort = ConfigurationManager.getProperty("http.proxy.port");

        System.out.println(proxyHost + " ---- " + proxyPort);

        Context context = new Context();

        String usage = "org.dspace.app.cris.batch.WosFeed -q queryPMC -p submitter -s start_date(YYYY-MM-DD) -e end_date(YYYY-MM-DD) -c collectionID";

        HelpFormatter formatter = new HelpFormatter();

        Options options = new Options();
        CommandLine line = null;

        options.addOption(OptionBuilder.withArgName("UserQuery").hasArg(true)
                .withDescription(
                        "UserQuery, default query setup in the wosfeed.cfg")
                .create("q"));

        options.addOption(
                OptionBuilder.withArgName("query SymbolicTimeSpan").hasArg(true)
                        .withDescription(
                                "This element defines a range of load dates. The load date is the date when a record was added to a database. If symbolicTimeSpan is specified,"
                                + " the timeSpan parameter must be omitted. If timeSpan and symbolicTimeSpan are both omitted, then the maximum publication date time span will"
                                + " be inferred from the editions data. Values 1week, 2week or 3week")
                .create("t"));
        
        options.addOption(
                OptionBuilder.withArgName("beginTimeSpan").hasArg(true)
                        .withDescription(
                                "Query start date to retrieve data publications from Wos, default start date is yesterday (begin timeSpan)")
                .create("s"));

        options.addOption(
                OptionBuilder.withArgName("endTimeSpan").hasArg(true)
                        .withDescription(
                                "Query End date to retrieve data publications from Wos, default is today (end timeSpan)")
                .create("e"));

        options.addOption(OptionBuilder.withArgName("forceCollectionID")
                .hasArg(false).withDescription("force use the collectionID")
                .create("f"));

        options.addOption(OptionBuilder.isRequired(true)
                .withArgName("collectionID").hasArg(true)
                .withDescription("Collection for item submission").create("c"));

        options.addOption(OptionBuilder.isRequired(true).withArgName("Eperson")
                .hasArg(true).withDescription("Submitter of the records")
                .create("p"));

        options.addOption(OptionBuilder.withArgName("status").hasArg(true)
                .withDescription(
                        "Status of new item p = workspace, w = workflow step 1, y = workflow step 2, x = workflow step 3, z = inarchive")
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
        boolean forceCollectionId = line.hasOption("f");

        String symbolicTimeSpan = null;
        String startDate = null;
        String endDate = null;
        
        if (line.hasOption("t"))
        {
            symbolicTimeSpan = line.getOptionValue("t");
        }
        
        if (!line.hasOption("s") && !line.hasOption("e"))
        {
            symbolicTimeSpan = "1week";
        }
        else {
            if (line.hasOption("s"))
            {
                startDate = line.getOptionValue("s");
            }
            else
            {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -1);
                startDate = df.format(cal.getTime());
            }
            
            if (line.hasOption("e"))
            {
                endDate = line.getOptionValue("e");
            }
            else
            {
                Calendar cal = Calendar.getInstance();
                endDate = df.format(cal.getTime());
            }
        }
        
        String userQuery = ConfigurationManager.getProperty("wosfeed",
                "query.param.default");
        if (line.hasOption("q"))
        {
            userQuery = line.getOptionValue("q");
        }

        if (line.hasOption("o"))
        {
            status = line.getOptionValue("o");
        }

        int total = 0;
        int deleted = 0;

        List<ImpRecordItem> pmeItemList = new ArrayList<ImpRecordItem>();

        ImpRecordDAO dao = ImpRecordDAOFactory.getInstance(context);
        pmeItemList.addAll(
                convertToImpRecordItem(userQuery, "WOK", symbolicTimeSpan, startDate, endDate));

        for (ImpRecordItem pmeItem : pmeItemList)
        {
            try
            {
                int tmpCollectionID = collection_id;
                if (!forceCollectionId)
                {
                    Set<ImpRecordMetadata> t = pmeItem.getMetadata().get("dc.source.type");
                    if (t != null && !t.isEmpty())
                    {
                        String stringTmpCollectionID = "";
                        Iterator<ImpRecordMetadata> iterator = t.iterator();
                        while (iterator.hasNext())
                        {
                            String stringTrimTmpCollectionID = iterator.next().getValue();
                            stringTmpCollectionID += stringTrimTmpCollectionID
                                    .trim();
                        }
                        tmpCollectionID = ConfigurationManager
                                .getIntProperty("wosfeed",
                                        "wos.type." + stringTmpCollectionID
                                                + ".collectionid",
                                        collection_id);
                    }
                }

                total++;
                String action = "insert";
                DTOImpRecord impRecord = writeImpRecord(context, dao,
                        tmpCollectionID, pmeItem, action, eperson.getID());

                dao.write(impRecord, true);
            }
            catch (Exception ex)
            {
                deleted++;
            }
        }
      
        System.out.println("Imported " + (total - deleted) + " record; "
                + deleted + " marked as removed");
        pmeItemList.clear();

        context.complete();

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

    private static List<ImpRecordItem> convertToImpRecordItem(String userQuery,
            String databaseID, String symbolicTimeSpan, String start, String end)
                    throws BadTransformationSpec, MalformedSourceException,
                    HttpException, IOException
    {
        List<ImpRecordItem> pmeResult = new ArrayList<ImpRecordItem>();
        List<Record> wosResult = wosOnlineDataLoader
                .searchByAffiliation(userQuery, databaseID, symbolicTimeSpan, start, end);
        List<ItemSubmissionLookupDTO> results = new ArrayList<ItemSubmissionLookupDTO>();
        if (wosResult != null && !wosResult.isEmpty())
        {

            TransformationEngine transformationEngine1 = getFeedTransformationEnginePhaseOne();
            if (transformationEngine1 != null)
            {
                for (Record record : wosResult)
                {
                    HashMap<String, Set<String>> map = new HashMap<String, Set<String>>();
                    HashSet<String> set = new HashSet<String>();
                    set.add(record.getValues("isiId").get(0).getAsString());
                    map.put("isiid", set);

                    MultipleSubmissionLookupDataLoader mdataLoader = (MultipleSubmissionLookupDataLoader) transformationEngine1
                            .getDataLoader();
                    mdataLoader.setIdentifiers(map);
                    SubmissionLookupOutputGenerator outputGenerator = (SubmissionLookupOutputGenerator) transformationEngine1
                            .getOutputGenerator();
                    outputGenerator
                            .setDtoList(new ArrayList<ItemSubmissionLookupDTO>());

                    transformationEngine1.transform(new TransformationSpec());
                    log.debug("BTE transformation finished!");
                    results.addAll(outputGenerator.getDtoList());
                }                
                
            }

            TransformationEngine transformationEngine2 = getFeedTransformationEnginePhaseTwo();
            if (transformationEngine2 != null && results!=null)
            {
                SubmissionItemDataLoader dataLoader = (SubmissionItemDataLoader) transformationEngine2
                        .getDataLoader();
                dataLoader.setDtoList(results);

                ImpRecordOutputGenerator outputGenerator = (ImpRecordOutputGenerator) transformationEngine2
                        .getOutputGenerator();
                transformationEngine2.transform(new TransformationSpec());
                pmeResult = outputGenerator.getRecordIdItems();
            }
        }
        return pmeResult;
    }

    public static TransformationEngine getFeedTransformationEnginePhaseOne()
    {
        return feedTransformationEnginePhaseOne;
    }
    
    public static TransformationEngine getFeedTransformationEnginePhaseTwo()
    {
        return feedTransformationEnginePhaseTwo;
    }

}
