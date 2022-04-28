/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.canvasdimension;

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
import org.dspace.iiif.canvasdimension.factory.IIIFCanvasDimensionServiceFactory;
import org.dspace.iiif.canvasdimension.service.IIIFCanvasDimensionService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Sets IIIF canvas metadata on bitstreams based on image size.
 *
 * @author Michael Spalti mspalti@willamette.edu
 */
public class CanvasDimensionCLI {

    private static final EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
                                                                                          .getConfigurationService();

    private CanvasDimensionCLI() {}

    public static void main(String[] argv) throws Exception {


        boolean iiifEnabled = configurationService.getBooleanProperty("iiif.enabled");
        if (!iiifEnabled) {
            System.out.println("WARNING: IIIF is not enabled on this DSpace server.");
        }

        // default to not updating existing dimensions
        boolean force = false;
        // default to printing messages
        boolean isQuiet = false;
        // default to no limit
        int max2Process = Integer.MAX_VALUE;

        String identifier = null;
        String eperson = null;

        Context context = new Context();
        IIIFCanvasDimensionService canvasProcessor = IIIFCanvasDimensionServiceFactory.getInstance()
                                                                                      .getIiifCanvasDimensionService();

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("i", "identifier", true,
            "process IIIF canvas dimensions for images belonging to this identifier");
        options.addOption("e", "eperson", true,
            "email of eperson setting the canvas dimensions");
        options.addOption("f", "force", false,
            "force update of all IIIF canvas height and width dimensions");
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
                .println("\nUUID example:    iiif-canvas-dimensions -e user@email.org " +
                    "-i 1086306d-8a51-43c3-98b9-c3b00f49105f");
            System.out
                .println("\nHandle example:    iiif-canvas-dimensions -e user@email.org " +
                        "-i 123456789/12");
            System.exit(0);
        }

        if (line.hasOption('f')) {
            force = true;
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
        String[] skipIds;

        if (line.hasOption('s')) {
            //specified which identifiers to skip when processing
            skipIds = line.getOptionValues('s');

            if (skipIds == null || skipIds.length == 0) {   //display error, since no identifiers specified to skip
                System.err.println("\nERROR: -s (-skip) option requires at least one identifier to SKIP.\n" +
                    "Make sure to separate multiple identifiers with a comma!\n" +
                    "(e.g. -s 123456789/34,123456789/323)\n");
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("Canvas Dimensions\n", options);
                System.exit(1);
            }
            canvasProcessor.setSkipList(Arrays.asList(skipIds));
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

        canvasProcessor.setForceProcessing(force);
        canvasProcessor.setMax2Process(max2Process);
        canvasProcessor.setIsQuiet(isQuiet);

        int processed = 0;
        switch (dso.getType()) {
            case Constants.COMMUNITY:
                processed = canvasProcessor.processCommunity(context, (Community) dso);
                break;
            case Constants.COLLECTION:
                processed = canvasProcessor.processCollection(context, (Collection) dso);
                break;
            case Constants.ITEM:
                canvasProcessor.processItem(context, (Item) dso);
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
        System.out.println(processed + " IIIF items were processed.");

    }

}
