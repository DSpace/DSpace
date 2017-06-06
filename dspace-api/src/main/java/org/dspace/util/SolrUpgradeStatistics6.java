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
import org.apache.solr.common.luke.FieldFlag;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.FacetParams;
import org.dspace.core.ConfigurationManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
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
	private int numRec;

	protected SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

	public SolrUpgradeStatistics6(String indexName, int numRec) {
                String serverPath = configurationService.getProperty("solr-statistics.server");
                serverPath = serverPath.replaceAll("statistics$", indexName);
                System.out.println("Connecting to " + serverPath);
                server = new HttpSolrServer(serverPath);
                this.numRec = numRec;
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
                }
	}

	private void run() throws SolrServerException {
                String query = "NOT(id:*-*) AND type:2";
	        SolrQuery sQ = new SolrQuery();
	        sQ.setQuery(query);
	        sQ.setFacet(true);
	        sQ.addFacetField("id");
	        sQ.setFacetMinCount(1);
	        QueryResponse sr = server.query(sQ);
	        
                for(FacetField ff: sr.getFacetFields()) {
                        System.out.println(ff);
                        for(FacetField.Count count: ff.getValues()) {
                                System.out.println(String.format("\t%s: %d", count.getName(), count.getCount()));
                        }
                }
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
}
