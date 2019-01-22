/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
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
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * CLI tool to upgrade legacy id references in SOLR statistics to DSpace 6 UUID's.
 * 
 * This command will need to be run iteratively over each statistics shard until all legacy id values have
 * been replaced.
 * 
 * If a legacy id cannot be resolved from the database, the id will remain unchanged.
 *   "field:* AND NOT(field:*-*)" can be used to locate legacy ids
 * 
 * See DS-3602 for the origin of this issue.  This code is targeted for inclusion in the DSpace 6.1 release.
 * 
 * Recommendation: for a large repository, run this command with -Xmx2000m if possible.
 * 
 * To process 1,000,000 statistics records, it took 60 min to complete.
 * 
 * @author Terry Brady, Georgetown University Library
 */
public class SolrUpgradePre6xStatistics {
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

    private static final String INDEX_DEFAULT = "statistics";
    private static final String MIGQUERY =
        "(id:* AND -(id:*-*)) OR (scopeId:* AND -(scopeId:*-*)) OR (epersonid:* AND -(epersonid:*-*))";

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
    private enum FIELD {
        id,
        scopeId,
        owningComm,
        owningColl,
        owningItem,
        epersonid,
        owner,
        submitter,
        actor;
    }

    //Logger
    private static final Logger log = Logger.getLogger(SolrUpgradePre6xStatistics.class);

    //DSpace Servcies
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected CommunityService   communityService     = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService  collectionService    = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService        itemService          = ContentServiceFactory.getInstance().getItemService();
    protected BitstreamService   bitstreamService     = ContentServiceFactory.getInstance().getBitstreamService();
    protected EPersonService     epersonService       = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService       groupService         = EPersonServiceFactory.getInstance().getGroupService();

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
     * @throws IOException
     * @throws SolrServerException
     */
    public SolrUpgradePre6xStatistics(String indexName, int numRec, int batchSize)
            throws SolrServerException, IOException {
        String serverPath = configurationService.getProperty("solr-statistics.server");
        serverPath = serverPath.replaceAll("statistics$", indexName);
        System.out.println("Connecting to " + serverPath);
        server = new HttpSolrServer(serverPath);
        server.setMaxTotalConnections(1);
        this.numRec = numRec;
        this.batchSize = batchSize;
        refreshContext();
    }

    /*
     * Process a batch of updates to SOLR
     */
    private void batchUpdateStats() throws SolrServerException, IOException {
        if (docs.size() > 0) {
            server.add(docs);
            server.commit(true, true);
            docs.clear();
        }
    }

    /**
     * Refresh the DSpace Context object in order to periodically release objects from memory
     * @throws IOException
     * @throws SolrServerException
     */
    private void refreshContext() throws SolrServerException, IOException {
        if (context != null) {
            try {
                totalCache += numUncache + context.getCacheSize();
            } catch (SQLException e) {
                log.warn(e.getMessage());
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
     * 
     * @param fromStart
     *            if true, report on processing time since the start of the program
     * @return the time in ms since the start time
     */
    private long logTime(boolean fromStart) {
        long ret = 0;
        long cur = new Date().getTime();
        if (lastTime == -1) {
            startTime = cur;
        } else if (fromStart) {
            ret = cur - startTime;
        } else {
            ret = cur - lastTime;
        }
        lastTime = cur;
        return ret;
    }

    /*
     * Format ms count as h:mm:ss
     * 
     * @param dur Duration in ms
     * 
     * @return duration formatted as h:mm:ss
     */
    private String duration(long dur) {
        long sec = dur / 1000;
        long hh = sec / 3600;
        long mm = (sec % 3600) / 60;
        long ss = (sec % 60);
        return String.format("%d:%02d:%02d", hh, mm, ss);
    }

    /**
     * Print a status message appended with the processing time for the operation
     * 
     * @param header
     *            Message to display
     * @param fromStart
     *            if true, report on processing time since the start of the program
     */
    private void printTime(int numProcessed, boolean fromStart) {
        long dur = logTime(fromStart);
        long totalDur = logTime(true);
        String stotalDur = duration(totalDur);
        long cacheSize = 0;
        try {
            cacheSize = context.getCacheSize();
        } catch (SQLException e) {
            log.error("Cannot get cache size", e);
        }
        String label = fromStart ? "TOTAL" : "Processed";
        System.out.println(String.format("%s (%s; %s; %s)",
            String.format("\t%,12d %10s...", numProcessed, label),
            String.format("%,6d sec; %s", dur / 1000, stotalDur),
            String.format("DB cache: %,6d/%,8d", cacheSize, getCacheCounts(fromStart)),
            String.format("Docs: %,6d", docs.size())));
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
        options.addOption(BATCH_OPTION, "batch-size", true,
                "Number of records to batch update to SOLR at one time (default=10,000).");
        return options;
    }

    /**
     * A utility method to print out all available command-line options and exit
     * given the specified code.
     *
     * @param options
     *            the supported options.
     * @param exitCode
     *            the exit code to use. The method will call System#exit(int) with
     *            the given code.
     */
    private static void printHelpAndExit(Options options, int exitCode) {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp(SolrUpgradePre6xStatistics.class.getSimpleName() + "\n", options);
        System.out.println("\n\nCommand Defaults");
        System.out.println(
                "\tsolr-upgrade-statistics-6x [-i statistics] [-n num_recs_to_process] [-b num_rec_to_update_at_once]");
        System.out.println("");
        System.out.println(
                "\tAfter upgrading to DSpace 6, this process should be run iteratively over every statistics shard ");
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
        System.out.println(
                "\t\tthe size of your statistics repositories.  Consider optimizing your solr repos when complete.");

        System.exit(exitCode);
    }

    /**
     * Entry point for command-line invocation
     * 
     * @param args
     *            command-line arguments; see help for description
     * @throws ParseException
     *             if the command-line arguments cannot be parsed
     */
    public static void main(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        Options options = makeOptions();

        System.out.println(" * This process should be run iteratively over every statistics shard ");
        System.out.println(" * until there are no remaining records with legacy ids present.");
        System.out.println(" * This process can be run while the system is in use.");
        System.out.println(" * It is likely to take 1 hour/1,000,000 legacy records to be udpated.");
        System.out.println(" *");
        System.out.println(" * This process will rewrite most solr statistics records and may temporarily double ");
        System.out.println(
                " *\tthe size of your statistics repositories.  Consider optimizing your solr repos when complete.");
        System.out.println(" * -------------------------------------------------------------------");

        String indexName = INDEX_DEFAULT;
        int numrec = NUMREC_DEFAULT;
        int batchSize = BATCH_DEFAULT;
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption(HELP_OPTION)) {
                printHelpAndExit(options, 0);
            }

            if (line.hasOption(INDEX_NAME_OPTION)) {
                indexName = line.getOptionValue(INDEX_NAME_OPTION, INDEX_DEFAULT);
            } else {
                System.err.println("No index name provided, defaulting to : " + INDEX_DEFAULT);
            }

            if (line.hasOption(NUMREC_OPTION)) {
                numrec = Integer.parseInt(line.getOptionValue(NUMREC_OPTION, "" + NUMREC_DEFAULT));
            }
            if (line.hasOption(BATCH_OPTION)) {
                batchSize = Integer.parseInt(line.getOptionValue(BATCH_OPTION, "" + BATCH_DEFAULT));
            }

        } catch (ParseException e) {
            System.err.println("Cannot read command options");
            printHelpAndExit(options, 1);
        }

        try {
            SolrUpgradePre6xStatistics upgradeStats = new SolrUpgradePre6xStatistics(indexName, numrec, batchSize);
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
        StringBuilder sb = new StringBuilder(MIGQUERY);
        SolrQuery sQ = new SolrQuery();
        sQ.setQuery(sb.toString());
        sQ.setFacet(true);
        sQ.addFacetField("type");
        sQ.addFacetField("scopeType");
        QueryResponse sr = server.query(sQ);

        long total = 0;
        long unexpected = 0;
        for (FacetField ff : sr.getFacetFields()) {
            String s = ff.getName().equals("type") ? "View" : "Search";
            for (FacetField.Count count : ff.getValues()) {
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
                     * In testing, I discovered some unexpected values in the scopeType field. It
                     * looks like they may have been a result of a CSV import/export error. This
                     * will group any unexpected values into one report line.
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
        long rem = sr.getResults().getNumFound() - total;
        if (rem > 0) {
            System.out.println(String.format("\t%,12d\t%s", rem, "Other Records"));
            total += rem;
        }
        return total;
    }

    /*
     * Process records with a legacy id. From the command line, the user may specify
     * records of a specific type to update Otherwise, the following sequence will
     * be applied in order to optimize hibernate caching.
     * 
     * Communities and Collections - retain in the cache since each is likely to be
     * re-used Items - retain in the cache until a new item is processed Bitstreams
     * - retain in the cache until a new bitstream is processed
     */
    private void run() throws SolrServerException, SQLException, IOException {
        runReport();
        logTime(false);
        for (int processed = updateRecords(MIGQUERY); (processed != 0)
                && (numProcessed < numRec); processed = updateRecords(MIGQUERY)) {
            printTime(numProcessed, false);
            batchUpdateStats();
            if (context.getCacheSize() > CACHE_LIMIT) {
                refreshContext();
            }
        }
        printTime(numProcessed, true);

        if (numProcessed > 0) {
            runReport();
        }
    }

    /*
     * Update records associated with a particular object id
     * 
     * @param query Query to retrieve all of the statistics records associated with
     * a particular object
     * 
     * @param field Field to use for grouping records
     * 
     * @return number of items processed. 0 indicates that no more work is available
     * (or the max processed has been reached).
     */
    private int updateRecords(String query) throws SolrServerException, SQLException, IOException {
        int initNumProcessed = numProcessed;
        SolrQuery sQ = new SolrQuery();
        sQ.setQuery(query);
        sQ.setRows(batchSize);

        // Ensure that items are grouped by id
        // Sort by id fails due to presense of id and string fields. The ord function
        // seems to help
        sQ.addSort("type", SolrQuery.ORDER.desc);
        sQ.addSort("scopeType", SolrQuery.ORDER.desc);
        sQ.addSort("ord(owningItem)", SolrQuery.ORDER.desc);
        sQ.addSort("id", SolrQuery.ORDER.asc);
        sQ.addSort("scopeId", SolrQuery.ORDER.asc);

        QueryResponse sr = server.query(sQ);
        SolrDocumentList sdl = sr.getResults();

        for (int i = 0; i < sdl.size() && (numProcessed < numRec); i++) {
            SolrDocument sd = sdl.get(i);
            SolrInputDocument input = ClientUtils.toSolrInputDocument(sd);
            input.remove("_version_");
            for (FIELD col : FIELD.values()) {
                mapField(input, col);
            }

            docs.add(input);
            ++numProcessed;
        }
        return numProcessed - initNumProcessed;
    }

    /*
     * Map solr fields from legacy ids to UUIDs.
     * 
     * The id field is interpreted by the type field. The scopeId field is
     * interpreted by scopeType field.
     * 
     * Legacy ids will be unchanged if they cannot be mapped
     * 
     * @param input The SOLR statistics document to be updated
     * 
     * @param col The SOLR field to update (if present)
     */
    private void mapField(SolrInputDocument input, FIELD col) throws SQLException {
        SolrInputField ifield = input.get(col.name());
        if (ifield != null) {
            Collection<Object> vals = ifield.getValues();
            ArrayList<String> newvals = new ArrayList<>();
            for (Object ovalx : vals) {
                //DS-3436 documented an issue in which multi-values in shards were converted to a comma separated string
                //It also produced strings containing "\" at the end of a value
                for (String oval: ovalx.toString().split(",")) {
                    oval = oval.replace("\\","");
                    try {
                        UUID uuid = null;
                        if (col == FIELD.owner) {
                            if (oval.length() > 1) {
                                String owntype = oval.substring(0, 1);
                                int legacy = Integer.parseInt(oval.substring(1));
                                uuid = mapOwner(owntype, legacy);
                            }
                        } else {
                            int legacy = Integer.parseInt(oval);
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
                        }
                        if (uuid != null) {
                            if (!newvals.contains(uuid.toString())) {
                                newvals.add(uuid.toString());
                            }
                        } else {
                            String s = oval + "-unmigrated";
                            if (!newvals.contains(s)) {
                                newvals.add(s);
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Non numeric legacy id " + col.name() + ":" + oval);
                    }
                }
            }
            if (newvals.size() > 0) {
                input.removeField(col.name());
                for (String nv : newvals) {
                    input.addField(col.name(), nv);
                }
            }
        }
    }

    /*
     * Determine if the last processed item should be cleared from the hibernate
     * cache
     * 
     * @param item Current item being processed
     */
    private void checkLastItem(Item item) throws SQLException {
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
     * Determine if the last processed bitstream should be cleared from the
     * hibernate cache
     * 
     * @param bitstream Current bitstream being processed
     */
    private void checkLastBitstream(Bitstream bitstream) throws SQLException {
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
     * Retrieve the UUID corresponding to a legacy id found in a SOLR statistics
     * record
     * 
     * @param col Solr Statistic Field being processed
     * 
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
        if (col == FIELD.epersonid || col == FIELD.actor || col == FIELD.submitter) {
            EPerson per = epersonService.findByLegacyId(context, val);
            return per == null ? null : per.getID();
        }
        return null;
    }

    /*
     * Retrieve the UUID corresponding to a legacy id found in a SOLR statistics
     * record
     * 
     * @param type Identifying type field for id OR scopeType field for scopeId
     * 
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
            // A bitstream is unlikely to be processed more than once, to clear immediately
            checkLastBitstream(bit);
            return uuid;
        }
        return null;
    }

    /*
     * Retrieve the UUID corresponding to a legacy owner found in a SOLR statistics
     * record Legacy owner fields are prefixed in solr with "e" or "g"
     * 
     * @param owntype Identifying type field (e - eperson, g - group)
     * 
     * @param val Value to lookup as a legacy id
     */
    private UUID mapOwner(String owntype, int val) throws SQLException {
        if (owntype.equals("e")) {
            EPerson per = epersonService.findByLegacyId(context, val);
            return per == null ? null : per.getID();
        } else if (owntype.equals("g")) {
            Group perg = groupService.findByLegacyId(context, val);
            return perg == null ? null : perg.getID();
        }
        return null;
    }

}