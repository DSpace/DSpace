package org.dspace.app.cris.batch;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
import org.dspace.app.cris.batch.bte.ImpRecordOutputGenerator;
import org.dspace.app.cris.batch.dao.ImpRecordDAO;
import org.dspace.app.cris.batch.dao.ImpRecordDAOFactory;
import org.dspace.app.cris.batch.dto.DTOImpRecord;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.submit.lookup.SubmissionItemDataLoader;
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

    private static TransformationEngine feedTransformationEngine = new DSpace()
            .getServiceManager().getServiceByName("wosFeedTransformationEngine",
                    TransformationEngine.class);

    private static WOSOnlineDataLoader wosOnlineDataLoader = new DSpace()
            .getServiceManager()
            .getServiceByName("wosOnlineDataLoader", WOSOnlineDataLoader.class);

    public static void main(String[] args)
            throws SQLException, BadTransformationSpec,
            MalformedSourceException, HttpException, IOException
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
                OptionBuilder.withArgName("query Start Date").hasArg(true)
                        .withDescription(
                                "Query start date to retrieve data publications from Wos, default start date is yesterday")
                .create("s"));

        options.addOption(
                OptionBuilder.withArgName("query End Date").hasArg(true)
                        .withDescription(
                                "Query End date to retrieve data publications from Wos, default is today")
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

        EPerson eperson = EPerson.find(context,
                Integer.parseInt(line.getOptionValue("p")));
        context.setCurrentUser(eperson);

        int collection_id = Integer.parseInt(line.getOptionValue("c"));
        boolean forceCollectionId = line.hasOption("f");

        String startDate = "";
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

        String endDate = "";
        if (line.hasOption("e"))
        {
            endDate = line.getOptionValue("e");
        }
        else
        {
            Calendar cal = Calendar.getInstance();
            endDate = df.format(cal.getTime());
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
                convertToImpRecordItem(userQuery, "WOK", startDate, endDate));

        try
        {
            for (ImpRecordItem pmeItem : pmeItemList)
            {

                int tmpCollectionID = collection_id;
                if (!forceCollectionId)
                {
                    Set<String> t = pmeItem.getMetadata().get("dc.type");                 
                    if (t != null && !t.isEmpty())
                    {
                        String stringTmpCollectionID = t.iterator().next();
                        tmpCollectionID = ConfigurationManager.getIntProperty("wosfeed", "wos.type." + stringTmpCollectionID + ".collectionid", collection_id);
                    }
                }

                total++;                
                String action = "insert";
                DTOImpRecord impRecord = writeImpRecord(context, dao,
                        tmpCollectionID, pmeItem, action, eperson.getID());

                dao.write(impRecord, true);

            }
        }
        catch (Exception ex)
        {
            deleted++;
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

    private static List<ImpRecordItem> convertToImpRecordItem(String userQuery,
            String databaseID, String start, String end)
                    throws BadTransformationSpec, MalformedSourceException,
                    HttpException, IOException
    {
        List<ImpRecordItem> pmeResult = new ArrayList<ImpRecordItem>();
        List<Record> wosResult = wosOnlineDataLoader
                .searchByAffiliation(userQuery, databaseID, start, end);
        List<ItemSubmissionLookupDTO> results = new ArrayList<ItemSubmissionLookupDTO>();
        if (wosResult != null && !wosResult.isEmpty())
        {
            for (Record record : wosResult)
            {
                List<Record> rr = new ArrayList<Record>();
                rr.add(record);
                ItemSubmissionLookupDTO result = new ItemSubmissionLookupDTO(
                        rr);
                results.add(result);
            }

            TransformationEngine transformationEngine2 = getFeedTransformationEngine();
            if (transformationEngine2 != null)
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

    public static TransformationEngine getFeedTransformationEngine()
    {
        return feedTransformationEngine;
    }

}
