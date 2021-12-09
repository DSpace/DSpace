/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.canvasdimension;

import java.util.Arrays;

import org.apache.commons.cli.*;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;

public class CanvasDimensionCLI {

    private CanvasDimensionCLI() {}

    public static void main(String[] argv) throws Exception {

        boolean force = false;
        boolean isQuiet = false;
        String identifier = null;
        int max2Process = Integer.MAX_VALUE;

        Context context = new Context();
        IIIFCanvasDimensionProcessor canvasProcessor = new IIIFCanvasDimensionProcessor();

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("i", "identifier", true,
            "process IIIF canvas dimensions for images belonging to identifier");
        options.addOption("f", "force", false,
            "force update of all IIIF canvas height and width dimensions");
        options.addOption("q", "quiet", false,
            "do not print anything except in the event of errors.");
        options.addOption("s", "skipList", false,
            "force update of all IIIF canvas height and width dimensions");
        options.addOption("m", "maximum", true,
            "process no more than maximum items");
        //create a "skip" option (to specify communities/collections/items to skip)
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
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("CanvasDimension processor\n", options);
            System.exit(1);
        }

        if (line.hasOption('f')) {
            force = true;
        }
        if (line.hasOption('q')) {
            isQuiet = true;
        }
        if (line.hasOption('i')) {
            identifier = line.getOptionValue('i');
        } else {
            System.out.println("An identifier for a Community, Collection, or Item must be provided.");
        }
        if (line.hasOption('m')) {
            max2Process = Integer.parseInt(line.getOptionValue('m'));
            if (max2Process <= 1) {
                System.out.println("Invalid maximum value '" +
                    line.getOptionValue('m') + "' - ignoring");
                max2Process = Integer.MAX_VALUE;
            }
        }
        String skipIds[] = null;

        if (line.hasOption('s')) {
            //specified which identifiers to skip when processing
            skipIds = line.getOptionValues('s');

            if (skipIds == null || skipIds.length == 0) {   //display error, since no identifiers specified to skip
                System.err.println("\nERROR: -s (-skip) option requires at least one identifier to SKIP.\n" +
                    "Make sure to separate multiple identifiers with a comma!\n" +
                    "(e.g. -s 123456789/34,123456789/323)\n");
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("MediaFilterManager\n", options);
                System.exit(0);
            }
            canvasProcessor.setSkipList(Arrays.asList(skipIds));
        }


        DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, identifier);
        if (dso == null) {
            throw new IllegalArgumentException("Cannot resolve "
                + identifier + " to a DSpace object");
        }


        canvasProcessor.setForceProcessing(force);
        canvasProcessor.setMax2Process(max2Process);
        canvasProcessor.setIsQuiet(isQuiet);

        switch (dso.getType()) {
            case Constants.SITE:
                canvasProcessor.processSite(context);
                break;
            case Constants.COMMUNITY:
                canvasProcessor.processCommunity(context, (Community) dso);
                break;
            case Constants.COLLECTION:
                canvasProcessor.processCollection(context, (Collection) dso);
                break;
            case Constants.ITEM:
                canvasProcessor.processItem(context, (Item) dso);
                break;
            default:
                break;
        }
    }

}
