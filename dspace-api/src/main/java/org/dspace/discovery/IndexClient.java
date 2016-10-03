/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.ExternalService;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;

/**
 * Class used to reindex dspace communities/collections/items into discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class IndexClient {


    private static final Logger log = Logger.getLogger(IndexClient.class);

    /**
     * When invoked as a command-line tool, creates, updates, removes content
     * from the whole index
     *
     * @param args the command-line arguments, none used
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     *
     */
    public static void main(String[] args) throws SQLException, IOException, SearchServiceException {

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        String usage = "org.dspace.discovery.IndexClient [-cbhf[r <item handle>]] or nothing to update/clean an existing index.";
        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        options
                .addOption(OptionBuilder
                        .withArgName("item handle")
                        .hasArg(true)
                        .withDescription(
                                "remove an Item, Collection or Community from index based on its handle")
                        .create("r"));


        options
                .addOption(OptionBuilder
                        .isRequired(false)
                        .withDescription(
                                "clean existing index removing any documents that no longer exist in the db")
                        .create("c"));

        options.addOption(OptionBuilder.isRequired(false).withDescription(
                "(re)build index [incremental mode]").create(
                "b"));

        options.addOption(OptionBuilder.isRequired(false).withDescription(
                "Rebuild the spellchecker, can be combined with -b and -f.").create(
                "s"));

        options
                .addOption(OptionBuilder
                        .isRequired(false)
                        .withDescription(
                                "if updating existing index, force each handle to be reindexed even if uptodate")
                        .create("f"));

        options
        .addOption(OptionBuilder
                .isRequired(false)
                .hasArg(true)
                .withDescription(
                        "update a specific class of objects based on its type")
                .create("t"));

        options
        .addOption(OptionBuilder
                .isRequired(false)
                .hasArg(true)
                .withDescription(
                "update an Item, Collection or Community from index based on its handle, use with -f to force clean")
                .create("u"));
        
        options.addOption(OptionBuilder.isRequired(false).withDescription(
                "print this help message").create("h"));

        options.addOption(OptionBuilder.isRequired(false).withDescription(
                "optimize search core").create("o"));
        
        options.addOption("e", "readfile", true, "Read the identifier from a file");

        try {
            line = new PosixParser().parse(options, args);
        } catch (Exception e) {
            // automatically generate the help statement
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }

        if (line.hasOption("h")) {
            // automatically generate the help statement
            formatter.printHelp(usage, options);
            System.exit(1);
        }

        /** Acquire from dspace-services in future */
        /**
         * new DSpace.getServiceManager().getServiceByName("org.dspace.discovery.SolrIndexer");
         */

        DSpace dspace = new DSpace();

        IndexingService indexer = dspace.getServiceManager().getServiceByName(IndexingService.class.getName(),IndexingService.class);

        if (line.hasOption("r")) {
            log.info("Removing " + line.getOptionValue("r") + " from Index");
            indexer.unIndexContent(context, line.getOptionValue("r"));
        } else if (line.hasOption("c")) {
            log.info("Cleaning Index");
            indexer.cleanIndex(line.hasOption("f"));
        } else if (line.hasOption("b")) {
            log.info("(Re)building index from scratch.");
            indexer.createIndex(context);
            checkRebuildSpellCheck(line, indexer);
        } else if (line.hasOption("o")) {
            log.info("Optimizing search core.");
            indexer.optimize();
 		} else if(line.hasOption('s')) {
            checkRebuildSpellCheck(line, indexer);           
        } else if (line.hasOption("t")) {
        	log.info("Updating and Cleaning a specific Index");
            String optionValue = line.getOptionValue("t");			
            indexer.updateIndex(context, true, Integer.valueOf(optionValue));
        } else if (line.hasOption("u")) {         	
        	String optionValue = line.getOptionValue("u");
			String[] identifiers = optionValue.split("\\s*,\\s*");
			for (String id : identifiers) {
				DSpaceObject dso;
				if (id.startsWith(ConfigurationManager.getProperty("handle.prefix")) || id.startsWith("123456789/")) {
					dso = HandleManager.resolveToObject(context, id);
				} else {

					dso = dspace.getSingletonService(ExternalService.class).getObject(id);
				}
				indexer.indexContent(context, dso, line.hasOption("f"));
			}
        } else if (line.hasOption('e')) {
            try {
                String filename = line.getOptionValue('e');
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object of DataInputStream
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                // Read File Line By Line

                int item_id = 0;
                List<Integer> ids = new ArrayList<Integer>();

                while ((strLine = br.readLine()) != null) {
                    item_id = Integer.parseInt(strLine.trim());
                    ids.add(item_id);
                }

                in.close();

                int type = -1;
                if (line.hasOption('t')) {
                    type = Integer.parseInt(line.getOptionValue("t"));
                } else {
                    // force to item
                    type = Constants.ITEM;
                }
                indexer.updateIndex(context, ids, line.hasOption("f"), type);
            } catch (Exception e) {
                log.error("Error: " + e.getMessage());
            }
        } else {
            log.info("Updating and Cleaning Index");
            indexer.cleanIndex(line.hasOption("f"));
            indexer.updateIndex(context, line.hasOption("f"));
            checkRebuildSpellCheck(line, indexer);
        }

        log.info("Done with indexing");
	}

    /**
     * Check the command line options and rebuild the spell check if active.
     * @param line the command line options
     * @param indexer the solr indexer
     * @throws SearchServiceException in case of a solr exception
     */
    protected static void checkRebuildSpellCheck(CommandLine line, IndexingService indexer) throws SearchServiceException {
        if (line.hasOption("s")) {
            log.info("Rebuilding spell checker.");
            indexer.buildSpellCheck();
        }
    }
}
