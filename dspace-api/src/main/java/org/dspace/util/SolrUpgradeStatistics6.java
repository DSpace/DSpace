/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.dspace.content.Bitstream;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * CLI tool to upgrade legacy id references in SOLR statistics to DSpace 6 UUID's.
 * 
 * This command will need to be run iteratively over each statistics shard until all legacy id values have been replaced.
 * 
 * If a legacy id cannot be resolved from the database, the suffix "-legacy" will be added to the id field to prevent the attempt to re-process that record.
 * 
 * See DS-3602 for the origin of this issue.  This code is targeted for inclusion in the DSpace 6.1 release.
 * 
 * Recommendation: for a large repository, run this command with -Xmx2000m if possible.
 * 
 * To process 1,000,000 statistics records, it took 60 min to complete.
 * 
 * @author Terry Brady, Georgetown University Library
 */
public class SolrUpgradeStatistics6
{
        //Command line parameter constants
        private static final String INDEX_NAME_OPTION = "i";
        private static final String NUMREC_OPTION = "n";
        private static final String BATCH_OPTION = "b";
        private static final String TYPE_OPTION = "t";
        private static final String HELP_OPTION = "h";
        private static final int    NUMREC_DEFAULT = 100000;
        private static final int    BATCH_DEFAULT = 10000;
        
        //After processing each batch of updates to SOLR, evaulate if the hibernate cache needs to be cleared
        private static final int    CACHE_LIMIT = 20000;
        
        //Maximum number of solr ids to pull in a facet query... when processing infrequently accessed ids, the
        //facet size may grow.
        private static final int    FACET_LIMIT = 1000;
        
        private static final String INDEX_DEFAULT = "statistics";
        private Integer type;

        //Counters to determine the number of items to process
        private int numRec = NUMREC_DEFAULT;
        private int batchSize = BATCH_DEFAULT;
        
        //Cache management
        private int numProcessed = 0;
        private long totalCache = 0;
        private long numUncache = 0;
        private List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        private Context context;

        
        //Enum to identify the named SOLR statistics fields to update
        private enum FIELD{
                id, 
                scopeId, 
                owningComm, 
                owningColl, 
                owningItem, 
                epersonid;
        }
        
        //Logger
        private static final Logger log = Logger.getLogger(SolrUpgradeStatistics6.class);
        
        //DSpace Servcies
        private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        protected CommunityService   communityService     = ContentServiceFactory.getInstance().getCommunityService();
        protected CollectionService  collectionService    = ContentServiceFactory.getInstance().getCollectionService();
        protected ItemService        itemService          = ContentServiceFactory.getInstance().getItemService();
        protected BitstreamService   bitstreamService     = ContentServiceFactory.getInstance().getBitstreamService();
        protected EPersonService     epersonService       = EPersonServiceFactory.getInstance().getEPersonService();

        // This code will operate on one shard at a time, therefore the SOLR web service will be accessed directly rather
        // than make use of the DSpace Solr Logger which only writes to the current shard
        private HttpSolrServer server;
        
        //Allows for smart use of hibernate cache
        private Item lastItem = null;
        private Bitstream lastBitstream = null;
        
        //Report on process times
        private long startTime = -1;
        private long lastTime = -1;
        

        /**
         * Construct the utility class from the command line options
         * @param indexName name of the statistics shard to update
         * @param numRec    maximum number of records to process
         * @param type      object type to process (community, collection, item, bitstream)
         * @throws IOException 
         * @throws SolrServerException 
         */
        public SolrUpgradeStatistics6(String indexName, int numRec, int batchSize, Integer type) throws SolrServerException, IOException {
                String serverPath = configurationService.getProperty("solr-statistics.server");
                serverPath = serverPath.replaceAll("statistics$", indexName);
                System.out.println("Connecting to " + serverPath);
                server = new HttpSolrServer(serverPath);
                //ensure that all queries wait for commits to complete
                server.setMaxTotalConnections(1);
                this.numRec = numRec;
                this.batchSize = batchSize;
                this.type = type;
                refreshContext();
        }

        /*
         * Process a batch of updates to SOLR
         */
        private void batchUpdateStats() throws SolrServerException, IOException {
                if (docs.size() > 0) {
                        server.add(docs);
                        server.commit(true, true, true);
                        docs.clear();
                }
                
        }
        
        /**
         * Refresh the DSpace Context object in order to periodically release objects from memory
         * @throws IOException 
         * @throws SolrServerException 
         */
        public void refreshContext() throws SolrServerException, IOException {
                if (context != null) {
                        try {
                                totalCache += numUncache + context.getCacheSize();
                        } catch (SQLException e) {
                        }
                }
                this.context = new Context(Context.Mode.READ_ONLY);
                lastItem = null;
                lastBitstream = null;
                numUncache = 0;
        }
        
        /*
         * Compute the number of items that were cached by hibernate since the context was cleared.
         */
        private long getCacheCounts(boolean fromStart) {
                long count = 0;
                try {
                        count = context.getCacheSize();
                } catch (Exception e) {
                        //no action
                }
                count += this.numUncache;
                if (fromStart) {
                        count += totalCache;
                }
                return count;
        }
        
        /**
         * Compute the time since the last batch was processed
         * @param fromStart if true, report on processing time since the start of the program
         * @return the time in ms since the start time
         */
        public long logTime(boolean fromStart) {
                long ret = 0;
                long cur = new Date().getTime();
                if (lastTime == -1) {
                        startTime = cur;
                } else if (fromStart){
                        ret = cur - startTime;
                } else {
                        ret = cur - lastTime;
                }
                lastTime = cur;
                return ret;
        }

        /* 
         * Format ms count as h:mm:ss
         * @param dur Duration in ms
         * @return duration formatted as h:mm:ss
         */
        private String duration(long dur) {
                long sec = dur / 1000;
                long hh = sec / 3600;
                long mm = (sec % 3600) / 60;
                long ss = (sec % 60);
                return String.format("%d:%02d:%02d", hh,mm,ss);
        }
        
        /**
         * Print a status message appended with the processing time for the operation
         * @param header Message to display
         * @param fromStart if true, report on processing time since the start of the program
         */
        public void printTime(String header, boolean fromStart) {
                long dur = logTime(fromStart);
                long totalDur = logTime(true);
                String stotalDur = duration(totalDur);
                System.out.println(String.format("%s (%,6d sec; %s; DB cache: %,6d; Docs: %,6d)", header, dur/1000, stotalDur, getCacheCounts(fromStart), docs.size()));
        }

        /*
         * Create command line option processor
         */
        private static Options makeOptions() {
                Options options = new Options();
                options.addOption(HELP_OPTION, "help", false, "Get help on options for this command.");
                options.addOption(INDEX_NAME_OPTION, "index-name", true,
                                                 "The names of the indexes to process. At least one is required (default=statistics)");
                options.addOption(NUMREC_OPTION, "num-rec", true, "Total number of records to update (defaut=100,000).");
                options.addOption(BATCH_OPTION, "batch-size", true, "Number of records to batch update to SOLR at one time (default=10,000).");
                options.addOption(TYPE_OPTION, "type", true, "(4) Communities, (3) Collections, (2) Items (0) Bitstreams (default=all)");
                return options;
        }

        /**
         * A utility method to print out all available command-line options and exit given the specified code.
         *
         * @param options the supported options.
         * @param exitCode the exit code to use. The method will call System#exit(int) with the given code.
         */
        private static void printHelpAndExit(Options options, int exitCode)
        {
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp(SolrUpgradeStatistics6.class.getSimpleName() + "\n", options);
                System.out.println("\n\nCommand Defaults");
                System.out.println("\tsolr-upgradeD6-statistics [-i statistics] [-n num_recs_to_process] [-b num_rec_to_update_at_once]");
                System.out.println("");
                System.out.println("\tAfter upgrading to DSpace 6, this process should be run iteratively over every statistics shard ");
                System.out.println("\t\tuntil there are no remaining records with legacy ids present.");
                System.out.println("\t\tThis process can be run while the system is in use.");
                System.out.println("");
                System.out.println("\tIt will take 20-30 min to process 1,000,000 legacy records. ");
                System.out.println("");
                System.out.println("\tUse the -n option to manage the workload on your server. ");
                System.out.println("\t\tTo process all records, set -n to 10000000 or to 100000000 (10M or 100M)");
                System.out.println("\tIf possible, please allocate 2GB of memory to this process (e.g. -Xmx2000m)");
                System.out.println("");
                System.out.println("\tThis process will rewrite most solr statistics records and may temporarily double ");
                System.out.println("\t\tthe size of your statistics repositories.  Consider optimizing your solr repos when complete.");

                System.exit(exitCode);
        }

        /**
         * Entry point for command-line invocation
         * @param args command-line arguments; see help for description
         * @throws ParseException if the command-line arguments cannot be parsed
         */
        public static void main(String[] args) throws ParseException
        {
                CommandLineParser parser = new PosixParser();
                Options options = makeOptions();
                
                System.out.println(" * This process should be run iteratively over every statistics shard ");
                System.out.println(" * until there are no remaining records with legacy ids present.");
                System.out.println(" * This process can be run while the system is in use.");
                System.out.println(" * It is likely to take 1 hour/1,000,000 legacy records to be udpated.");
                System.out.println(" *");
                System.out.println(" * This process will rewrite most solr statistics records and may temporarily double ");
                System.out.println(" *\tthe size of your statistics repositories.  Consider optimizing your solr repos when complete.");
                System.out.println(" * -------------------------------------------------------------------");

                String indexName = INDEX_DEFAULT;
                int numrec = NUMREC_DEFAULT;
                int batchSize = BATCH_DEFAULT;
                Integer type = null;
                try
                {
                        CommandLine line = parser.parse(options, args);
                        if (line.hasOption(HELP_OPTION))
                        {
                                printHelpAndExit(options, 0);
                        }

                        if (line.hasOption(INDEX_NAME_OPTION))
                        {
                            indexName = line.getOptionValue(INDEX_NAME_OPTION, INDEX_DEFAULT);
                        }
                        else
                        {
                            System.err.println("No index name provided, defaulting to : "+ INDEX_DEFAULT);
                        }
                        
                        if (line.hasOption(NUMREC_OPTION)) {
                                numrec = Integer.parseInt(line.getOptionValue(NUMREC_OPTION,""+NUMREC_DEFAULT));
                        }
                        if (line.hasOption(BATCH_OPTION)) {
                                batchSize = Integer.parseInt(line.getOptionValue(BATCH_OPTION,""+BATCH_DEFAULT));
                        }
                        if (line.hasOption(TYPE_OPTION)) {
                                String s = null;
                                try {
                                        s = line.getOptionValue(TYPE_OPTION);
                                        type = Integer.parseInt(s);
                                } catch (NumberFormatException e) {
                                        log.warn("Non integer type parameter "+s);
                                }
                        }

                }
                catch (ParseException e)
                {
                        System.err.println("Cannot read command options");
                        printHelpAndExit(options, 1);
                }
                
                try {
                        SolrUpgradeStatistics6 upgradeStats = new SolrUpgradeStatistics6(indexName, numrec, batchSize, type);
                        upgradeStats.run();
                } catch (SolrServerException e) {
                        log.error("Error querying stats", e);
                } catch (SQLException e) {
                        log.error("Error querying stats", e);
                } catch (IOException e) {
                        log.error("Error querying stats", e);
                }
        }

        
        /*
         * Process records with a legacy id.
         * From the command line, the user may specify records of a specific type to update
         * Otherwise, the following sequence will be applied in order to optimize hibernate caching.
         * 
         * Communities and Collections - retain in the cache since each is likely to be re-used
         * Items - retain in the cache until a new item is processed
         * Bitstreams - retain in the cache until a new bitstream is processed
         * 
         * 1. Find the most viewed item records
         * 2. Find bitstreams owned by item records
         * 3. Find all remaining bitstream records
         * 4. Find all remaining collection records
         * 5. Find all remaining community records
         * 
         */
        private void run() throws SolrServerException, SQLException, IOException {
                runReport();
                logTime(false);
                if (type != null) {
                        for(FIELD field: FIELD.values()){                                
                                while(run(field, type) > 0){};                                
                        }
                } else {
                        //process items first minimize the number of objects that will be loaded from hibernate at one time
                        while(run(FIELD.id, Constants.ITEM) > 0){};
                        while(run(FIELD.scopeId, Constants.ITEM) > 0){};
                        while(run(FIELD.owningItem, Constants.ITEM) > 0){};
                        
                        //process any bitstrem objects that did not have a match on onwingItem
                        while(run(FIELD.id, Constants.BITSTREAM) > 0){};
                        while(run(FIELD.scopeId, Constants.BITSTREAM) > 0){};
                        
                        //collections
                        while(run(FIELD.id, Constants.COLLECTION) > 0){};
                        while(run(FIELD.scopeId, Constants.COLLECTION) > 0){};
                        while(run(FIELD.owningColl, Constants.COLLECTION) > 0){};
                        
                        //communities
                        while(run(FIELD.id, Constants.COMMUNITY) > 0){};
                        while(run(FIELD.scopeId, Constants.COMMUNITY) > 0){};
                        while(run(FIELD.owningComm, Constants.COMMUNITY) > 0){};                        

                        //Handle irregular type/scopetype values
                        while(run(FIELD.id, -1) > 0){};                        
                        while(run(FIELD.scopeId, -1) > 0){};                        
                        while(run(FIELD.epersonid, -1) > 0){};                        
                }
                
                if (numProcessed > 0) {
                        printTime(String.format("\t%,12d Processed...", numProcessed), false);
                        printTime(String.format("\t%,12d TOTAL...    ", numProcessed), true);
                        batchUpdateStats();
                }
                
                if (numProcessed > 0) {
                        runReport();
                }
        }

        /*
         * Report on the existence of legacy id records within a shard
         */
        private void runReport() throws SolrServerException {
                System.out.println();
                System.out.println("=================================================================");
                System.out.println("\t*** Statistics Records with Legacy Id ***\n");
                long total = runReportQuery();
                System.out.println("\t--------------------------------------");
                System.out.println(String.format("\t%,12d\t%s", total, "TOTAL"));
                System.out.println("=================================================================");
                System.out.println();
        }
        /*
         * Report on the existence of specific legacy id records within a shard
         */
        private long runReportQuery() throws SolrServerException {
                StringBuilder sb = new StringBuilder();
                for(FIELD field: FIELD.values()) {
                        if (sb.length() > 0) {
                                sb.append(" OR ");
                        }
                        if (field == FIELD.epersonid) {
                                sb.append(String.format("(%s:* AND NOT(%s:*-*) AND NOT(epersonid:0))", field.name(), field.name()));
                        } else {
                                sb.append(String.format("(%s:* AND NOT(%s:*-*))", field.name(), field.name()));
                        }
                }
                SolrQuery sQ = new SolrQuery();
                sQ.setQuery(sb.toString());
                sQ.setFacet(true);
                sQ.addFacetField("type");
                sQ.addFacetField("scopeType");
                QueryResponse sr = server.query(sQ);
                
                long total = 0;
                long unexpected = 0;
                for(FacetField ff: sr.getFacetFields()) {
                        String s = ff.getName().equals("type") ? "View" : "Search";
                        for(FacetField.Count count: ff.getValues()) {
                                String name = count.getName();
                                int id = Integer.parseInt(name);
                                if (id == Constants.COMMUNITY) {
                                        name = "Community " + s;
                                } else if (id == Constants.COLLECTION) {
                                        name = "Collection " + s;
                                } else if (id == Constants.ITEM) {
                                        name = "Item " + s;
                                } else if (id == Constants.BITSTREAM) {
                                        name = "Bistream " + s;
                                } else {
                                        /*
                                         * In testing, I discovered some unexpected values in the scopeType field.  
                                         * It looks like they may have been a result of a CSV import/export error.
                                         * This will group any unexpected values into one report line.
                                         */
                                        unexpected += count.getCount();
                                        continue;
                                }
                                System.out.println(String.format("\t%,12d\t%s", count.getCount(), name));
                                total += count.getCount();
                        }
                }
                if (unexpected > 0) {
                        System.out.println(String.format("\t%,12d\t%s", unexpected, "Unexpected Type & Full Site"));
                        total += unexpected;
                }
                return total;
        }
        
        /*
         * Run queries for records of a specific type faceting by id to retrieve the most viewed records first.
         * @param field Field to use for grouping records
         * @param ptype The type of object to query:Community, Collection, Item, Bitstream; -1 is used for irregular type values
         */
        private int run(FIELD field, int ptype) throws SolrServerException, SQLException, IOException {
                if (numProcessed >= numRec) {
                        return 0;
                }
                int initNumProcessed = numProcessed;
                String query = "";
                
                if (ptype == -1){
                        query = String.format("%s:* AND NOT(%s:*-*)", field.name(), field.name());
                        if (field == FIELD.epersonid) {
                                query += " AND NOT(epersonid:0)";
                        }
                } else {
                        query = String.format("%s:* AND NOT(%s:*-*) AND (type:%d OR scopeType:%d)", field.name(), field.name(), ptype, ptype);
                }
                SolrQuery sQ = new SolrQuery();
                sQ.setQuery(query);
                sQ.setFacet(true);
                sQ.addFacetField(field.name());
                sQ.setFacetMinCount(1);
                sQ.setFacetLimit(Math.min(FACET_LIMIT,batchSize));
                QueryResponse sr = server.query(sQ);
                
                for(FacetField ff: sr.getFacetFields()) {
                        for(FacetField.Count count: ff.getValues()) {
                                String id = count.getName();
                                
                                //When querying on id or scopeId, find as many objects as possible that will re-use objects in cache
                                if (field == FIELD.id || field == FIELD.scopeId) {
                                        if (ptype == Constants.COMMUNITY) {
                                                updateRecords(field, String.format("id:%s OR owningComm:%s OR scopeId:%s", id, id, id));
                                        } else if (ptype == Constants.COLLECTION) {
                                                updateRecords(field, String.format("id:%s OR owningColl:%s OR scopeId:%s", id, id, id));
                                        } else if (ptype == Constants.ITEM) {
                                                updateRecords(field, String.format("id:%s OR owningItem:%s OR scopeId:%s", id, id, id));
                                        } else  {
                                                updateRecords(field, String.format("id:%s OR scopeId:%s", id, id));
                                        }
                                } else {
                                        updateRecords(field, String.format("%s:%s", field.name(), id));
                                }
                                if (numProcessed >= numRec) {
                                        break;
                                }
                        }
                        if (numProcessed >= numRec) {
                                break;
                        }
                }
                if ((numProcessed - initNumProcessed) > 0){
                        printTime(String.format("\t%,12d Processed...", numProcessed), false);
                        batchUpdateStats();
                        refreshContext();
                }
                return numProcessed - initNumProcessed;
        }
        
        /*
         * Update records associated with a particular object id
         * @param query Query to retrieve all of the statistics records associated with a particular object
         * @param field Field to use for grouping records
         * @return number of items processed.  0 indicates that no more work is available (or the max processed has been reached).
         */
        private int updateRecords(FIELD field, String query) throws SolrServerException, SQLException, IOException {
                int initNumProcessed = numProcessed;
                SolrQuery sQ = new SolrQuery();
                sQ.setQuery(query);
                sQ.setRows(Math.min(numRec - numProcessed, batchSize));
                
                //Ensure that items are grouped by id
                //Sort by id fails due to presense of id and string fields. The ord function seems to help
                sQ.setSort(String.format("ord(%s)", field.name()), SolrQuery.ORDER.asc); 
                
                QueryResponse sr = server.query(sQ);
                SolrDocumentList sdl = sr.getResults();
                
                for(int i=0; i<sdl.size() && (numProcessed < numRec); i++) {
                        SolrDocument sd = sdl.get(i);
                        SolrInputDocument input = ClientUtils.toSolrInputDocument(sd);
                        input.remove("_version_");
                        for(FIELD col: FIELD.values()) {
                                mapField(input, col);
                        }
                        
                        docs.add(input);
                        ++numProcessed;
                        if (numProcessed % batchSize == 0) {
                                printTime(String.format("\t%,12d Processed...", numProcessed), false);
                                batchUpdateStats();
                                if (context.getCacheSize() > CACHE_LIMIT) {
                                        refreshContext();
                                }
                        }
                }
                return numProcessed - initNumProcessed;
        }
        
        
        /*
         * Map solr fields from legacy ids to UUIDs.
         * 
         * The id field is interpreted by the type field.
         * The scopeId field is interpreted by scopeType field.
         * 
         * In order to prevent the re-processing of items with an id that cannot be resolved, the suffix "-legacy" will be appended to any id fields that cannot be resolved.  
         * 
         * @param input The SOLR statistics document to be updated
         * @param col The SOLR field to update (if present)
         */
        private void mapField(SolrInputDocument input, FIELD col) throws SQLException {
                SolrInputField ifield = input.get(col.name());
                if (ifield != null) {
                        Collection<Object> vals = ifield.getValues();
                        ArrayList<String> newvals = new ArrayList<>();
                        for(Object oval: vals) {
                                try {
                                        int legacy = Integer.parseInt(oval.toString());
                                        UUID uuid = null;
                                        if (col == FIELD.id) {
                                                Object otype = input.getFieldValue("type");
                                                if (otype != null) {
                                                        int type = Integer.parseInt(otype.toString());
                                                        uuid = mapType(type, legacy);
                                                }
                                        } else if (col == FIELD.scopeId) {
                                                Object otype = input.getFieldValue("scopeType");
                                                if (otype != null) {
                                                        int type = Integer.parseInt(otype.toString());
                                                        uuid = mapType(type, legacy);
                                                }
                                        } else {
                                                uuid = mapId(col, legacy);
                                        }
                                        if (uuid != null) {
                                                newvals.add(uuid.toString());
                                        } else if (col == FIELD.epersonid) {
                                                //Prevent re-processing of a legacy id value if a database match cannot be found.
                                                newvals.add(String.format("%d-legacy", legacy));
                                        } else {
                                                //reformatting of the epersonid seemed to trigger silent update failures (with log warnings)
                                                newvals.add(String.format("%d", -legacy));
                                        }
                                } catch (NumberFormatException e) {
                                        log.warn("Non numeric legacy id "+ col.name() +":" + oval.toString());
                                }
                        }
                        if (newvals.size() > 0) {
                                input.removeField(col.name());
                                for(String nv: newvals) {
                                        input.addField(col.name(), nv);
                                }
                                
                        }
                }
        }
        
        /*
         * Determine if the last processed item should be cleared from the hibernate cache
         * @param item Current item being processed
         */
        public void checkLastItem(Item item) throws SQLException {
                if (item != null) {
                        if (lastItem == null) {
                                lastItem = item;
                        } else if (!lastItem.getID().equals(item.getID())) {
                                numUncache++;
                                context.uncacheEntity(lastItem);
                                lastItem = item;
                        }                        
                }
        }

        /*
         * Determine if the last processed bitstream should be cleared from the hibernate cache
         * @param bitstream Current bitstream being processed
         */
        public void checkLastBitstream(Bitstream bitstream) throws SQLException {
                if (bitstream != null) {
                        if (lastBitstream == null) {
                                lastBitstream = bitstream;
                        } else if (!lastBitstream.getID().equals(bitstream.getID())) {
                                numUncache++;
                                context.uncacheEntity(lastBitstream);
                                lastBitstream = bitstream;
                        }                        
                }
        }

        /*
         * Retrieve the UUID corresponding to a legacy id found in a SOLR statistics record
         * @param col Solr Statistic Field being processed
         * @param val Value to lookup as a legacy id
         */
        private UUID mapId(FIELD col, int val) throws SQLException {
                
                if (col == FIELD.owningComm) {
                        Community comm = communityService.findByLegacyId(context, val);
                        return comm == null ? null : comm.getID();
                }
                if (col == FIELD.owningColl) {
                        org.dspace.content.Collection coll = collectionService.findByLegacyId(context, val);
                        return coll == null ? null : coll.getID();
                }
                if (col == FIELD.owningItem) {
                        Item item = itemService.findByLegacyId(context, val);
                        checkLastItem(item);
                        return item == null ? null : item.getID();
                }
                if (col == FIELD.epersonid) {
                        EPerson per = epersonService.findByLegacyId(context, val);
                        return per == null ? null : per.getID();
                }
                return null;
        }

        /*
         * Retrieve the UUID corresponding to a legacy id found in a SOLR statistics record
         * @param type Identifying type field for id OR scopeType field for scopeId
         * @param val Value to lookup as a legacy id
         */
        private UUID mapType(int type, int val) throws SQLException {
                if (type == Constants.COMMUNITY) {
                        Community comm = communityService.findByLegacyId(context, val);
                        return comm == null ? null : comm.getID();
                }
                if (type == Constants.COLLECTION) {
                        org.dspace.content.Collection coll = collectionService.findByLegacyId(context, val);
                        return coll == null ? null : coll.getID();
                }
                if (type == Constants.ITEM) {
                        Item item = itemService.findByLegacyId(context, val);
                        checkLastItem(item);
                        return item == null ? null : item.getID();
                }
                if (type == Constants.BITSTREAM) {
                        Bitstream bit = bitstreamService.findByLegacyId(context, val);
                        UUID uuid = bit == null ? null : bit.getID();
                        //A bitstream is unlikely to be processed more than once, to clear immediately
                        checkLastBitstream(bit);
                        return uuid;
                }
                return null;
        }

}
