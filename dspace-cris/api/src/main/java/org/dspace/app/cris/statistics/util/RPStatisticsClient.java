/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
/*
 * StatisticsImporter.java
 *
 * Version: $Revision: 4754 $
 *
 * Date: $Date: 2010-02-09 08:36:43 +0100 (mar, 09 feb 2010) $
 *
 * Copyright (c) 2002-2010, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.cris.statistics.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.SolrLogger;
import org.dspace.utils.DSpace;

/**
 * Class to load intermediate statistics files into solr
 * 
 * @author Stuart Lewis
 */
public class RPStatisticsClient
{

    /**
     * Print the help message
     * 
     * @param options
     *            The command line options the user gave
     * @param exitCode
     *            the system exit code to use
     */
    private static void printHelp(Options options, int exitCode)
    {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("RPStatisticsClient\n", options);
        System.exit(exitCode);
    }

    /**
     * Main method to run the statistics importer.
     * 
     * @param args
     *            The command line arguments
     * @throws Exception
     *             If something goes wrong
     */
    public static void main(String[] args) throws Exception
    {

        DSpace dspace = new DSpace();

        SolrLogger indexer = dspace.getServiceManager().getServiceByName(
                SolrLogger.class.getName(), SolrLogger.class);

        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("u", "update-spider-files", false,
                "Update Spider IP Files from internet into "
                        + ConfigurationManager.getProperty("dspace.dir")
                        + "/config/spiders");

        options.addOption("m", "mark-spiders", false,
                "Update isBog Flag in Solr");
        options.addOption("f", "delete-spiders-by-flag", false,
                "Delete Spiders in Solr By isBot Flag");
        options.addOption("i", "delete-spiders-by-ip", false,
                "Delete Spiders in Solr By IP Address");
        options.addOption("h", "help", false, "help");
        options.addOption("b", "mark-spiders-by-useragent", true,
                "Update isBot Flag in Solr By User Agent");
        CommandLine line = parser.parse(options, args);

        // Did the user ask to see the help?
        if (line.hasOption('h'))
        {
            printHelp(options, 0);
        }

        if (line.hasOption("u"))
        {
            // StatisticsClient.updateSpiderFiles();
        }
        else if (line.hasOption('m'))
        {
            indexer.markRobotsByIP();
        }
        else if (line.hasOption('f'))
        {
            indexer.deleteRobotsByIsBotFlag();
        }
        else if (line.hasOption('i'))
        {
            indexer.deleteRobotsByIP();
        }
        else if (line.hasOption('b'))
        {
            if (line.getOptionValue('b') != null)
                indexer.markRobotByUserAgent(line.getOptionValue('b'));
        }
        else
        {
            printHelp(options, 0);
        }
    }

}
