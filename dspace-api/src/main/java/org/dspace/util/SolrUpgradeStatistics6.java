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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.luke.FieldFlag;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.FacetParams;
import org.dspace.content.Bitstream;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLoggerServiceImpl.ResultProcessor;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.*;
import java.util.*;

/**
 * Utility class to export, clear and import Solr indexes.
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 */
public class SolrUpgradeStatistics6
{
        private static final String INDEX_NAME_OPTION = "i";
        private static final String NUMREC_OPTION = "n";
        private static final String TYPE_OPTION = "t";
        private static final String HELP_OPTION = "h";
        private static final int NUMREC_DEFAULT = 10000;
        private static final String INDEX_DEFAULT = "statistics";
        private static final Logger log = Logger.getLogger(SolrUpgradeStatistics6.class);
        private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        private HttpSolrServer server;
        private int numRec = NUMREC_DEFAULT;
        private int numProcessed = 0;
        private enum FIELD{owningColl,owningComm,id,owningItem,scopeId;}
        private Context context;
        private Integer type;
        
        protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
        protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
        protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

        protected SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

        public SolrUpgradeStatistics6(String indexName, int numRec, Integer type) {
                String serverPath = configurationService.getProperty("solr-statistics.server");
                serverPath = serverPath.replaceAll("statistics$", indexName);
                System.out.println("Connecting to " + serverPath);
                server = new HttpSolrServer(serverPath);
                this.numRec = numRec;
                this.context = new Context();
                this.context.setMode(Context.Mode.READ_ONLY);
                this.type = type;
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

                String indexName = INDEX_DEFAULT;
                int numrec = NUMREC_DEFAULT;
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
                
                SolrUpgradeStatistics6 upgradeStats = new SolrUpgradeStatistics6(indexName, numrec, type);
                try {
                        upgradeStats.run();
                } catch (SolrServerException e) {
                        log.error("Error querying stats", e);
                } catch (SQLException e) {
                        log.error("Error querying stats", e);
                } catch (IOException e) {
                        log.error("Error querying stats", e);
                }
        }

        private void run() throws SolrServerException, SQLException, IOException {
                runReport();
                if (type != null) {
                        run(type);
                } else {
                        //process items first minimize the number of objects that will be loaded from hibernate at one time
                        run(Constants.ITEM);
                        //process any bitstrem objects that did not have a match on onwingItem
                        run(Constants.BITSTREAM);
                        //process collections
                        run(Constants.COLLECTION);
                        //process collections
                        run(Constants.COMMUNITY);
                }
                if (numProcessed > 0) {
                        System.out.println("\n\t\t *** Num Processed: "+numProcessed+"\n");
                        runReport();
                }
        }

        private void runReport() throws SolrServerException {
                System.out.println("=================================================================");
                System.out.println("\t*** Statistics Records with Legacy Id ***");
                runReport(false);
                runReport(true);
                System.out.println("=================================================================");
        }
        private void runReport(boolean search) throws SolrServerException {
                String field = search ? "scopeId" : "id";
                String query = String.format("NOT(%s:*-*)", field); 
                SolrQuery sQ = new SolrQuery();
                sQ.setQuery(query);
                sQ.setFacet(true);
                sQ.addFacetField(search? "scopeType" : "type");
                QueryResponse sr = server.query(sQ);
                
                for(FacetField ff: sr.getFacetFields()) {
                        for(FacetField.Count count: ff.getValues()) {
                                String name = count.getName();
                                int id = Integer.parseInt(name);
                                String s = search ? "Search" : "View";
                                if (id == Constants.COMMUNITY) {
                                        name = "Community " + s;
                                } else if (id == Constants.COLLECTION) {
                                        name = "Collection " + s;
                                } else if (id == Constants.ITEM) {
                                        name = "Item " + s;
                                } else if (id == Constants.BITSTREAM) {
                                        name = "Bistream " + s;
                                } else if (id == Constants.SITE) {
                                        //I have inconsistent values for this field
                                        name = "Site "+ s + "(TBD)";
                                } 
                                System.out.println(String.format("\t%s: %d", name, count.getCount()));
                        }
                }
        }
        
        
        private void run(int ptype) throws SolrServerException, SQLException, IOException {
                String query = String.format("NOT(id:*-*) AND type:%d", ptype);
                SolrQuery sQ = new SolrQuery();
                sQ.setQuery(query);
                sQ.setFacet(true);
                sQ.addFacetField("id");
                sQ.setFacetMinCount(1);
                QueryResponse sr = server.query(sQ);
                
                for(FacetField ff: sr.getFacetFields()) {
                        for(FacetField.Count count: ff.getValues()) {
                                String id = count.getName();
                                if (ptype == Constants.COMMUNITY) {
                                        updateRecords(String.format("id:%s OR owningComm:%s OR scopeId:%s", id, id, id));
                                } else if (ptype == Constants.COLLECTION) {
                                        updateRecords(String.format("id:%s OR owningColl:%s OR scopeId:%s", id, id, id));
                                } else if (ptype == Constants.ITEM) {
                                        updateRecords(String.format("id:%s OR owningItem:%s OR scopeId:%s", id, id, id));
                                } else if (ptype == Constants.BITSTREAM) {
                                        updateRecords(String.format("id:%s OR scopeId:%s", id, id));
                                }
                        }
                }
        }
        
        private void updateRecords(String query) throws SolrServerException, SQLException, IOException {
                SolrQuery sQ = new SolrQuery();
                sQ.setQuery(query);
                sQ.setRows(numRec - numProcessed);
                
                QueryResponse sr = server.query(sQ);
                SolrDocumentList sdl = sr.getResults();
                for(int i=0; i<sdl.size() && (numProcessed < numRec); i++) {
                        SolrDocument sd = sdl.get(i);
                        SolrInputDocument input = ClientUtils.toSolrInputDocument(sd);
                        for(FIELD col: FIELD.values()) {
                                mapField(input, col);
                        }
                        server.add(input);
                        ++numProcessed;
                }
                server.commit();
        }
        

        private static Options makeOptions() {
                Options options = new Options();
                options.addOption(HELP_OPTION, "help", false, "Get help on options for this command.");
                options.addOption(INDEX_NAME_OPTION, "index-name", true,
                                                 "The names of the indexes to process. At least one is required. Available indexes are: authority, statistics.");
                options.addOption(NUMREC_OPTION, "num-rec", true, "Number of records to update.");
                options.addOption(TYPE_OPTION, "type", true, "(4) Communities, (3) Collections, (2) Items (0) Bitstreams");
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
                System.out.println("\tsolr-upgradeD6-statistics [-i statistics] [-n num_recs_to_process]");
                System.exit(exitCode);
        }
        
        private void mapField(SolrInputDocument input, FIELD col) throws SQLException {
                SolrInputField ifield = input.get(col.name());
                if (ifield != null) {
                        Collection<Object> vals = ifield.getValues();
                        ArrayList<UUID> newvals = new ArrayList<>();
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
                                                newvals.add(uuid);
                                        }
                                } catch (NumberFormatException e) {
                                        log.warn("Non numeric legacy id "+ col.name() +":" + oval.toString());
                                }
                        }
                        if (newvals.size() > 0) {
                                input.removeField(col.name());
                                for(UUID uuid: newvals) {
                                        input.addField(col.name(), uuid.toString());
                                }
                                
                        }
                }
        }
        
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
                        return item == null ? null : item.getID();
                }
                return null;
        }
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
                        return item == null ? null : item.getID();
                }
                if (type == Constants.BITSTREAM) {
                        Bitstream bit = bitstreamService.findByLegacyId(context, val);
                        return bit == null ? null : bit.getID();
                }
                return null;
        }

}
