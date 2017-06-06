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
        private static final String HELP_OPTION = "h";
        private static final int NUMREC_DEFAULT = 1000;
        private static final String INDEX_DEFAULT = "statistics";
	private static final Logger log = Logger.getLogger(SolrUpgradeStatistics6.class);
	private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
	private HttpSolrServer server;
	private int numRec = NUMREC_DEFAULT;
	private int numProcessed = 0;
	private enum FIELD{owningColl,owningComm,id,owningItem;}
        private Context context;

        protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
        protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
        protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

	protected SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

	public SolrUpgradeStatistics6(String indexName, int numRec) {
                String serverPath = configurationService.getProperty("solr-statistics.server");
                serverPath = serverPath.replaceAll("statistics$", indexName);
                System.out.println("Connecting to " + serverPath);
                server = new HttpSolrServer(serverPath);
                this.numRec = numRec;
                this.context = new Context();
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

		}
		catch (ParseException e)
		{
			System.err.println("Cannot read command options");
			printHelpAndExit(options, 1);
		}
		
		SolrUpgradeStatistics6 upgradeStats = new SolrUpgradeStatistics6(indexName, numrec);
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
                String query = "NOT(id:*-*) AND type:2";
	        SolrQuery sQ = new SolrQuery();
	        sQ.setQuery(query);
	        sQ.setFacet(true);
	        sQ.addFacetField("id");
	        sQ.setFacetMinCount(1);
                System.out.println(server.getBaseURL() + sQ.toString() + "\n");
	        QueryResponse sr = server.query(sQ);
	        
                for(FacetField ff: sr.getFacetFields()) {
                        for(FacetField.Count count: ff.getValues()) {
                                System.out.println(String.format("\t%s: %d", count.getName(), count.getCount()));
                                updateItem(count.getName());
                        }
                }
                System.out.println("Done!");
        }
	
	private void updateItem(String id) throws SolrServerException, SQLException, IOException {
	        final List<SolrDocument> docsToUpdate = new ArrayList<SolrDocument>();

	        SolrQuery sQ = new SolrQuery();
	        sQ.setQuery(String.format("id:%s OR owningItem:%s", id, id));
	        sQ.setRows(numRec - numProcessed);
	        
	        System.out.println(server.getBaseURL() + sQ.toString() + "\n");
	        
	        QueryResponse sr = server.query(sQ);
	        SolrDocumentList sdl = sr.getResults();
	        for(int i=0; i<sdl.size(); i++) {
	                SolrDocument sd = sdl.get(i);
	                System.out.println(sd.get("uid") + "  " + sd.get("type") + " " + sd.get("id"));
	                SolrInputDocument input = ClientUtils.toSolrInputDocument(sd);
	                for(FIELD col: FIELD.values()) {
	                        mapField(input, col);
	                }
	                server.add(input);
	                System.out.println(++numProcessed + " Prepared doc for update: "+sd.get("uid"));
	        }
	        UpdateResponse ur = server.commit();
	}
	

        private static Options makeOptions() {
		Options options = new Options();
		options.addOption(HELP_OPTION, "help", false, "Get help on options for this command.");
		options.addOption(INDEX_NAME_OPTION, "index-name", true,
				                 "The names of the indexes to process. At least one is required. Available indexes are: authority, statistics.");
		options.addOption(NUMREC_OPTION, "num-rec", true, "Number of records to update.");
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
