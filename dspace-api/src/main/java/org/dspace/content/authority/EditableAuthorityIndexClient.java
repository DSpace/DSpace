/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

/**
 * Class used to reindex DSpace Authority Concepts into Solr
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Lantian Gai (lantian at atmire dot com)
 */
public class EditableAuthorityIndexClient {


    private static final Logger log = Logger.getLogger(EditableAuthorityIndexClient.class);

    /**
     * When invoked as a command-line tool, creates, updates, removes content
     * from the whole index
     *
     * @param args the command-line arguments, none used
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @throws org.apache.solr.client.solrj.SolrServerException
     *
     */
    public static void main(String[] args) throws Exception {

        Context context = new Context();
        context.setIgnoreAuthorization(true);

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
                "(re)build index, wiping out current one if it exists").create(
                "b"));

        options
                .addOption(OptionBuilder
                        .isRequired(false)
                        .withDescription(
                                "if updating existing index, force each handle to be reindexed even if uptodate")
                        .create("f"));

        options.addOption(OptionBuilder.isRequired(false).withDescription(
                "print this help message").create("h"));

        options.addOption(OptionBuilder.isRequired(false).withDescription(
                "optimize search solr core").create("o"));

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

        EditableAuthorityIndexingService indexer = dspace.getServiceManager().getServiceByName(EditableAuthorityIndexingService.class.getName(),EditableAuthorityIndexingService.class);

        if (line.hasOption("r")) {
            log.info("Removing " + line.getOptionValue("r") + " from Index");
            indexer.unIndexContent(context, line.getOptionValue("r"), true);
        } else if (line.hasOption("c")) {
            log.info("Cleaning Index");
            indexer.cleanIndex();
        } else if (line.hasOption("b")) {
            log.info("(Re)building index from scratch.");
            indexer.cleanIndex();
            indexer.updateIndex(context,true);
        } else if (line.hasOption("o")) {
            log.info("Optimizing search core.");
            indexer.optimize();
        } else {
            log.info("Updating Index, force reindex of Concepts:" + line.hasOption("f"));
            indexer.updateIndex(context, line.hasOption("f"));
        }

        log.info("Done with indexing");
	}
}
