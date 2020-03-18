/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.batch;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.deduplication.service.DedupService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.utils.DSpace;

/**
 * CLI Tool used to populate the deduplication index of solr dedup core.
 * 
 * Usage: ./dspace index-deduplication [-chfueto[r <item handle/uuid>]]
 */
public class DedupClient {
    private static final Logger log = Logger.getLogger(DedupClient.class);

    private DedupClient() {
    }

    /**
     * When invoked as a command-line tool, creates, updates, removes content from
     * the whole index
     *
     * @param args the command-line arguments, none used
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @throws SolrServerException
     *
     */
    public static void main(String[] args)
            throws SQLException, IOException, SearchServiceException, SolrServerException {

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        String usage = "./dspace index-deduplication [-chfueo[r <item handle/uuid>]]"
                + " or nothing to update/clean an existing index.";
        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        options.addOption(OptionBuilder.withArgName("item handle").hasArg(true)
                .withDescription("remove an object from your handle/uuid").create("r"));

        options.addOption(OptionBuilder.isRequired(false)
                .withDescription("clean existing index removing any documents that no longer exist in the db")
                .create("c"));

        options.addOption(OptionBuilder.isRequired(false)
                .withDescription("if updating existing index, force each handle to be reindexed even if uptodate")
                .create("f"));

        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("update an entity from index based on its handle or uuid, use with -f to force clean")
                .create("u"));

        options.addOption(OptionBuilder.isRequired(false).withDescription("print this help message").create("h"));

        options.addOption(OptionBuilder.isRequired(false).withDescription("optimize search core").create("o"));

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
         * new
         * DSpace.getServiceManager().getServiceByName("org.dspace.discovery.SolrIndexer");
         */

        DSpace dspace = new DSpace();

        DedupService indexer = dspace.getServiceManager().getServiceByName(DedupService.class.getName(),
                DedupService.class);

        if (line.hasOption("r")) {
            log.info("Removing " + line.getOptionValue("r") + " from Index");
            indexer.unIndexContent(context, line.getOptionValue("r"));
        } else if (line.hasOption("c")) {
            log.info("Cleaning Index");
            indexer.cleanIndex(line.hasOption("f"));
        } else if (line.hasOption("o")) {
            log.info("Optimizing dedup core.");
            indexer.optimize();
        } else if (line.hasOption("u")) {
            String optionValue = line.getOptionValue("u");
            String[] identifiers = optionValue.split("\\s*,\\s*");
            for (String id : identifiers) {
                Item item;

                item = (Item) HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, id);
                indexer.indexContent(context, item, line.hasOption("f"));
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

                UUID item_id = null;
                List<UUID> ids = new ArrayList<UUID>();

                while ((strLine = br.readLine()) != null) {
                    item_id = UUID.fromString(strLine.trim());
                    ids.add(item_id);
                }

                in.close();
                indexer.indexContent(context, ids, line.hasOption("f"));
            } catch (Exception e) {
                log.error("Error: " + e.getMessage());
            }
        } else {
            log.info("Updating and Cleaning Index");
            indexer.cleanIndex(line.hasOption("f"));
            indexer.updateIndex(context, line.hasOption("f"));
        }

        log.info("Done with indexing");
    }

}
