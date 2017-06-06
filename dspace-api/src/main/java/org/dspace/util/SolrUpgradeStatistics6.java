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
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.luke.FieldFlag;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.FacetParams;
import org.dspace.core.ConfigurationManager;

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
        private static final int numrec_default = 1000;
	private static final Logger log = Logger.getLogger(SolrUpgradeStatistics6.class);

	/**
	 * Entry point for command-line invocation
	 * @param args command-line arguments; see help for description
	 * @throws ParseException if the command-line arguments cannot be parsed
	 */
	public static void main(String[] args) throws ParseException
	{
		CommandLineParser parser = new PosixParser();
		Options options = makeOptions();

                String indexName = "statistics";
                int numrec = numrec_default;
		try
		{
			CommandLine line = parser.parse(options, args);
			if (line.hasOption(HELP_OPTION))
			{
				printHelpAndExit(options, 0);
			}

			if (line.hasOption(INDEX_NAME_OPTION))
			{
			    indexName = line.getOptionValue(INDEX_NAME_OPTION, "statistics");
			}
			else
			{
			    System.err.println("No index name provided, defaulting to \"statistics\".");
			}
			
			if (line.hasOption(NUMREC_OPTION)) {
			        numrec = Integer.parseInt(line.getOptionValue(NUMREC_OPTION,""+numrec_default));
			}

		}
		catch (ParseException e)
		{
			System.err.println("Cannot read command options");
			printHelpAndExit(options, 1);
		}
		
		System.out.println(String.format("==>%s: %d", indexName, numrec));
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
