/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.searchindex;

import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.iiif.searchindex.factory.IIIFSearchIndexServiceFactoryImpl;
import org.dspace.iiif.searchindex.service.IIIFSearchIndexService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Command line utility for search indexing.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 */
public class IIIFSearchIndexCLI {

    private static final EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
                                                                                          .getConfigurationService();

    private IIIFSearchIndexCLI() {}

    public static void main(String[] argv) throws Exception {

        boolean iiifEnabled = configurationService.getBooleanProperty("iiif.enabled");
        if (!iiifEnabled) {
            System.out.println("ERROR: IIIF is not enabled on this DSpace server.");
            System.exit(1);
        }

        String action = "";

        // default to printing messages
        boolean isQuiet = false;
        // default to no limit
        int max2Process = Integer.MAX_VALUE;

        String identifier = null;
        String eperson = null;

        Context context = new Context();
        IIIFSearchIndexService iiifSearchIndexService = IIIFSearchIndexServiceFactoryImpl.getInstance()
                                                                                         .getIiifSearchIndexService();

        if (!iiifSearchIndexService.checkStatus()) {
            String serviceLocation = configurationService.getProperty("iiif.search.index.service");
            System.out.println("\nERROR: The IIIF indexing service for OCR files was not found at the location\n" +
                "specified in iiif.cfg: " + serviceLocation);
            System.out.println("\nSee documentation for configuring and using the IIIF indexing service.");
            System.out.println("\n<Link to the documentation here>\n");
            System.exit(1);
        }


        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("i", "identifier", true,
            "index ocr files belonging to this identifier");
        options.addOption("e", "eperson", true,
            "email of eperson setting the canvas dimensions");
        options.addOption("a", "add", false,
            "add items to solr index");
        options.addOption("d", "delete", false,
            "delete items to solr index");
        options.addOption("q", "quiet", false,
            "do not print anything except in the event of errors");
        options.addOption("m", "maximum", true,
            "process no more than maximum items");
        options.addOption("h", "help", false,
            "display help");

        Option skipOption = Option.builder("s")
                                  .longOpt("skip")
                                  .hasArg()
                                  .hasArgs()
                                  .valueSeparator(',')
                                  .desc(
                                      "SKIP the bitstreams belonging to identifier\n" +
                                          "Separate multiple identifiers with a comma (,)\n" +
                                          "(e.g. -s \n 123456789/34,123456789/323)")
                                  .build();
        options.addOption(skipOption);

        CommandLine line = null;

        try {
            line = parser.parse(options, argv);
        } catch (MissingArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
            HelpFormatter help = new HelpFormatter();
            help.printHelp("CanvasDimension processor\n", options);
            System.exit(1);
        }

        if (line.hasOption('h')) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("CanvasDimension processor\n", options);
            System.out
                .println("\nUUID example:    iiif-canvas-dimensions -a -e user@email.org " +
                    "-i 1086306d-8a51-43c3-98b9-c3b00f49105f");
            System.out
                .println("\nHandle example:    iiif-canvas-dimensions -a -e user@email.org " +
                    "-i 123456789/12");
            System.exit(0);
        }

        if (line.hasOption('q')) {
            isQuiet = true;
        }
        if (line.hasOption('e')) {
            eperson = line.getOptionValue('e');
        }
        if (line.hasOption('i')) {
            identifier = line.getOptionValue('i');
        } else {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("CanvasDimension processor\n", options);
            System.out.println("An identifier for a Community, Collection, or Item must be provided.");
            System.exit(1);
        }
        if (line.hasOption('m')) {
            max2Process = Integer.parseInt(line.getOptionValue('m'));
            if (max2Process <= 1) {
                System.out.println("Invalid maximum value '" +
                    line.getOptionValue('m') + "' - ignoring");
                max2Process = Integer.MAX_VALUE;
            }
        }
        if (!(line.hasOption('a') || line.hasOption('d'))) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("IIIFSearchIndex processor\n", options);
            System.out.println("You must provide either an \"add\" or a \"delete\" option.");
            System.exit(1);
        }
        if (line.hasOption('a')) {
            if (line.hasOption('d')) {
                HelpFormatter help = new HelpFormatter();
                help.printHelp("IIIFSearchIndex processor\n", options);
                System.out.println("You must provide either an \"add\" or a \"delete\" option.");
                System.exit(1);
            }
            action = "add";
        }
        if (line.hasOption('d')) {
            if (line.hasOption('a')) {
                HelpFormatter help = new HelpFormatter();
                help.printHelp("IIIFSearchIndex processor\n", options);
                System.out.println("You must provide either an \"add\" or a \"delete\" option.");
                System.exit(1);
            }
            String s = System.console().readLine("\nThis action will delete all entries from the IIIF search " +
                "index for this DSpace \nCommunity, Collection, or Item. \n\nAre you sure you want to do this? Y/n: ");
            if (!"Y".equals(s)) {
                System.out.println("Action stopped.");
                System.exit(1);
            }
            action = "delete";
        }

        String[] skipIds;

        if (line.hasOption('s')) {
            //specified which identifiers to skip when processing
            skipIds = line.getOptionValues('s');

            if (skipIds == null || skipIds.length == 0) {   //display error, since no identifiers specified to skip
                System.err.println("\nERROR: -s (-skip) option requires at least one identifier to SKIP.\n" +
                    "Make sure to separate multiple identifiers with a comma!\n" +
                    "(e.g. -s 123456789/34,123456789/323)\n");
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("IIIF Search Index\n", options);
                System.exit(1);
            }
            iiifSearchIndexService.setSkipList(Arrays.asList(skipIds));
        }

        DSpaceObject dso = null;
        if (identifier.indexOf('/') != -1) {
            dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, identifier);
        } else {
            dso = UtilServiceFactory.getInstance().getDSpaceObjectUtils()
                                    .findDSpaceObject(context, UUID.fromString(identifier));
        }

        if (dso == null) {
            throw new IllegalArgumentException("Cannot resolve "
                + identifier + " to a DSpace object.");
        }

        EPerson user;

        if (eperson == null) {
            System.out.println("You must provide an eperson using the \"-e\" flag.");
            System.exit(1);
        }

        if (eperson.indexOf('@') != -1) {
            // @ sign, must be an email
            user = epersonService.findByEmail(context, eperson);
        } else {
            user = epersonService.find(context, UUID.fromString(eperson));
        }

        if (user == null) {
            System.out.println("Error, eperson cannot be found: " + eperson);
            System.exit(1);
        }

        context.setCurrentUser(user);

        iiifSearchIndexService.setMax2Process(max2Process);
        iiifSearchIndexService.setIsQuiet(isQuiet);

        System.out.println("Processing started.");
        int processed = 0;
        switch (dso.getType()) {
            case Constants.COMMUNITY:
                processed = iiifSearchIndexService.processCommunity(context, (Community) dso, action);
                break;
            case Constants.COLLECTION:
                processed = iiifSearchIndexService.processCollection(context, (Collection) dso, action);
                break;
            case Constants.ITEM:
                iiifSearchIndexService.processItem(context, (Item) dso, action);
                processed = 1;
                break;
            default:
                System.out.println("Unsupported object type.");
                break;
        }
        // commit changes
        if (processed >= 1) {
            context.commit();
        }

        // Always print summary to standard out.
        if (action.contentEquals("delete")) {
            System.out.println(processed + " items were removed from the IIIF search index.");
        } else {
            // When processing a single item don't bother showing the trivial item count; if the item was
            // skipped because it is in the index already the count is confusing.
            if (dso.getType() != Constants.ITEM) {
                System.out.println(processed + " items were added to the IIIF search index.");
            }
        }
        System.out.println("Processing was successfully completed.");

    }

}
