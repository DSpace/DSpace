/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.ocrcanvas;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
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
import org.dspace.iiif.ocrcanvas.factory.AnnotationLinkServiceFactory;
import org.dspace.iiif.ocrcanvas.service.AnnotationLinkService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class AnnotationLinkCLI {

    private static final EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
                                                                                          .getConfigurationService();

    private AnnotationLinkCLI() {}

    public static void main(String[] argv) throws Exception {

        Date startTime = new Date();

        boolean iiifEnabled = configurationService.getBooleanProperty("iiif.enabled");
        if (!iiifEnabled) {
            System.out.println("WARNING: IIIF is not enabled on this DSpace server.");
        }
        AnnotationLinkService annotationLinkService = AnnotationLinkServiceFactory.getInstance()
                                                                                  .getAnnotationLinkService();

        annotationLinkService.setDeleteAction(false);
        annotationLinkService.setReplaceAction(false);

        String identifier = null;
        String eperson = null;

        Context context = new Context(Context.Mode.BATCH_EDIT);

        CommandLineParser parser = new DefaultParser();


        Options options = new Options();
        options.addOption("i", "identifier", true,
            "link OCR files and image bitstreams belonging to this identifier");
        options.addOption("e", "eperson", true,
            "email of eperson setting the canvas dimensions");
        options.addOption("r", "replace", false,
            "replace existing canvasid metadata for ocr bitstreams");
        options.addOption("d", "delete", false,
            "delete existing canvasid metadata from ocr bitstreams");
        options.addOption("h", "help", false,
            "display help");

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
        if (line.hasOption('e')) {
            eperson = line.getOptionValue('e');
        }
        if (line.hasOption('r')) {
            System.out.println("\nReplace the existing canvasid metadata for ocr files.\n");
            annotationLinkService.setReplaceAction(true);
        }
        if (line.hasOption('d')) {
            System.out.println("\nDelete any existing canvasid metadata from ocr files.\n");
            annotationLinkService.setDeleteAction(true);
        }
        if (line.hasOption('i')) {
            identifier = line.getOptionValue('i');
        } else {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("CanvasDimension processor\n", options);
            System.out.println("An identifier for a Community, Collection, or Item must be provided.");
            System.exit(1);
        }

        DSpaceObject dso = null;
        if (identifier.indexOf('/') != -1) {
            dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, identifier);
        } else {
            dso = UtilServiceFactory.getInstance().getDSpaceObjectUtils()
                                    .findDSpaceObject(context, UUID.fromString(identifier));
        }

        if (dso == null) {
            throw new IllegalArgumentException("Cannot resolve " + identifier + " to a DSpace object.");
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

        System.out.println("\nProcessing OCR files.\n");

        int processed = 0;
        switch (dso.getType()) {
            case Constants.COMMUNITY:
                processed = annotationLinkService.processCommunity(context, (Community) dso);
                break;
            case Constants.COLLECTION:
                processed = annotationLinkService.processCollection(context, (Collection) dso);
                break;
            case Constants.ITEM:
                annotationLinkService.processItem(context, (Item) dso);
                processed = 1;
                break;
            default:
                System.out.println("Unsupported object type.");
                break;
        }
        if (processed >= 1) {
            context.commit();
        }


        Date endTime = new Date();
        System.out.println("Started: " + startTime.getTime());
        System.out.println("Ended: " + endTime.getTime());
        System.out.println(
            "Elapsed time: " + ((endTime.getTime() - startTime.getTime()) / 1000) + " secs (" + (endTime
                .getTime() - startTime.getTime()) + " msecs)");

        // Always print summary to standard out.
        System.out.println(processed + " IIIF items were processed.");
    }

}
