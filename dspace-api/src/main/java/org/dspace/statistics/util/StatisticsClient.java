/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.Get;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.SolrLogger;

import java.io.*;
import java.net.URL;

/**
 * Class to load intermediate statistics files into solr
 *
 * @author Stuart Lewis
 */
public class StatisticsClient
{
    private static final Logger log = Logger.getLogger(StatisticsClient.class);

    /**
     * Print the help message
     *
     * @param options The command line options the user gave
     * @param exitCode the system exit code to use
     */
    private static void printHelp(Options options, int exitCode)
    {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("StatisticsClient\n", options);
        System.exit(exitCode);
    }

    /**
     * Main method to run the statistics importer.
     *
     * @param args The command line arguments
     * @throws Exception If something goes wrong
     */
	public static void main(String[] args) throws Exception
    {
		CommandLineParser parser = new PosixParser();

		Options options = new Options();

        options.addOption("u", "update-spider-files", false,
                "Update Spider IP Files from internet into " +
                        ConfigurationManager.getProperty("dspace.dir") + "/config/spiders");

        options.addOption("m", "mark-spiders", false, "Update isBot Flag in Solr");
        options.addOption("f", "delete-spiders-by-flag", false, "Delete Spiders in Solr By isBot Flag");
        options.addOption("i", "delete-spiders-by-ip", false, "Delete Spiders in Solr By IP Address");
        options.addOption("o", "optimize", false, "Run maintenance on the SOLR index");
        options.addOption("b", "reindex-bitstreams", false, "Reindex the bitstreams to ensure we have the bundle name");
        options.addOption("e", "export", false, "Export SOLR view statistics data to usage-statistics-intermediate-format");
        options.addOption("r", "remove-deleted-bitstreams", false, "While indexing the bundle names remove the statistics about deleted bitstreams");
        options.addOption("s", "shard-solr-index", false, "Split the data from the main Solr core into separate Solr cores per year");
        options.addOption("h", "help", false, "help");

		CommandLine line = parser.parse(options, args);

        // Did the user ask to see the help?
        if (line.hasOption('h'))
        {
            printHelp(options, 0);
        }

        if(line.hasOption("u"))
        {
            StatisticsClient.updateSpiderFiles();
        }
        else if (line.hasOption('m'))
        {
            SolrLogger.markRobotsByIP();
        }
        else if(line.hasOption('f'))
        {
            SolrLogger.deleteRobotsByIsBotFlag();
        }
        else if(line.hasOption('i'))
        {
            SolrLogger.deleteRobotsByIP();
        }
        else if(line.hasOption('o'))
        {
            SolrLogger.optimizeSOLR();
        }
        else if(line.hasOption('b'))
        {
            SolrLogger.reindexBitstreamHits(line.hasOption('r'));
        }
        else if(line.hasOption('e'))
        {
            SolrLogger.exportHits();
        }
        else if(line.hasOption('s'))
        {
            SolrLogger.shardSolrIndex();
        }
        else
        {
            printHelp(options, 0);
        }
    }

    /**
     * Method to update Spiders in config directory.
     */
    private static void updateSpiderFiles()
    {
	    try
        {
            System.out.println("Downloading latest spider IP addresses:");

            // Get the list URLs to download from
            String urls = ConfigurationManager.getProperty("solr-statistics", "spiderips.urls");
            if ((urls == null) || ("".equals(urls)))
            {
                System.err.println(" - Missing setting from dspace.cfg: solr.spiderips.urls");
                System.exit(0);
            }

            // Get the location of spiders directory
            File spiders = new File(ConfigurationManager.getProperty("dspace.dir"),"config/spiders");

            if (!spiders.exists() && !spiders.mkdirs())
            {
                log.error("Unable to create spiders directory");
            }

            String[] values = urls.split(",");
            for (String value : values)
            {
                value = value.trim();
                System.out.println(" Downloading: " + value);

                URL url = new URL(value);

                Get get = new Get();
                get.setDest(new File(spiders, url.getHost() + url.getPath().replace("/","-")));
                get.setSrc(url);
                get.setUseTimestamp(true);
                get.execute();

            }


        } catch (Exception e)
        {
            System.err.println(" - Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }


}
