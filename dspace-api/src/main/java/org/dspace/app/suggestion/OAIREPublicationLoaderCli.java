/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.app.suggestion.oaire.OAIREPublicationLoader;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runner responsible to import metadata about authors from OpenAIRE to Solr.
 * This runner works in two ways:
 * If -s parameter with a valid UUID is received, then the specific researcher
 * with this UUID will be used.
 * Invocation without any parameter results in massive import, processing all
 * authors registered in DSpace.
 */

public class OAIREPublicationLoaderCli
    extends DSpaceRunnable<OAIREPublicationLoaderScriptConfiguration<OAIREPublicationLoaderCli>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAIREPublicationLoaderCli.class);

    private OAIREPublicationLoader oairePublicationLoader = null;

    protected Context context;

    protected String profile;

//    /**
//     * Import record from OpenAIRE to Solr.
//     * This method works in two ways. If -s parameter with a valid UUID is received,
//     * then the specific researcher with this UUID will be used. Otherwise, information
//     * about all researcher will be loaded.
//     * 
//     * @param args
//     * @throws Exception
//     */
//    public static void main(String[] args) throws Exception {
//        try (Context context = new Context()) {
//            CommandLineParser parser = new PosixParser();
//            Options options = createCommandLineOptions();
//            CommandLine line = parser.parse(options, args);
//            checkHelpEntered(options, line);
//            String profile = getProfileFromCommandLine(line);
//            List<Item> researchers = null;
//            if (profile == null) {
//                System.out.println("No argument for -s, process all profile");
//                researchers = getResearchers(context, null);
//            } else {
//                System.out.println("Process eperson item with UUID " + profile);
//                researchers = getResearchers(context, UUID.fromString(profile));
//            }
//
//            // load all author publication
//            for (Item researcher : researchers) {
//                getOAIREPublicationLoader().importAuthorRecords(researcher);
//            }
//            System.out.println("Process complete");
//            System.exit(0);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    private static String getProfileFromCommandLine(CommandLine line) {
//        String query = line.getOptionValue("s");
//        if (StringUtils.isEmpty(query)) {
//            return null;
//        }
//        return query;
//    }
//
//    protected static Options createCommandLineOptions() {
//        Options options = new Options();
//        options.addOption("s", "person", true, "UUID of the author object");
//        return options;
//    }

    private static void checkHelpEntered(Options options, CommandLine line) {
        if (line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Import Notification event json file", options);
            System.exit(0);
        }
    }

//    /**
//     * return an instance of OAIREPublicationLoader
//     * 
//     * @return an instance of OAIREPublicationLoader
//     */
//    public static OAIREPublicationLoader getOAIREPublicationLoader() {
//        if (oairePublicationLoader == null) {
//            oairePublicationLoader = getDSpace().getServiceManager().getServiceByName(
//                    "OAIREPublicationLoader", OAIREPublicationLoader.class);
//        }
//        return oairePublicationLoader;
//    }




//    /**
//     * Get DSpace instance
//     * 
//     * @return Dspace instance
//     */
//    private static DSpace getDSpace() {
//        if (dspace == null) {
//            dspace = new DSpace();
//        }
//        return dspace;
//    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public OAIREPublicationLoaderScriptConfiguration<OAIREPublicationLoaderCli> getScriptConfiguration() {
        OAIREPublicationLoaderScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("import-oaire-suggestions", OAIREPublicationLoaderScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {

        oairePublicationLoader = new DSpace().getServiceManager().getServiceByName(
                "OAIREPublicationLoader", OAIREPublicationLoader.class);

        profile = commandLine.getOptionValue("s");
        if (profile == null) {
            LOGGER.info("No argument for -s, process all profile");
        } else {
            LOGGER.info("Process eperson item with UUID " + profile);
        }


    }

    @Override
    public void internalRun() throws Exception {

        context = new Context();

        List<Item> researchers = getResearchers(profile);

        for (Item researcher : researchers) {

            oairePublicationLoader.importAuthorRecords(researcher);

        }

    }

    /**
     * Get the Item(s) which map a researcher from Solr. If the uuid is specified,
     * the researcher with this UUID will be chosen. If the uuid doesn't match any
     * researcher, the method returns an empty array list. If uuid is null, all
     * research will be return.
     * 
     * @param profile uuid of the researcher. If null, all researcher will be
     *                returned.
     * @return the researcher with specified UUID or all researchers
     */
    @SuppressWarnings("rawtypes")
    private List<Item> getResearchers(String profileUUID) {
        final UUID uuid = profileUUID != null ? UUID.fromString(profileUUID) : null;
        SearchService searchService = new DSpace().getSingletonService(SearchService.class);
        List<IndexableObject> objects = null;
        if (uuid != null) {
            objects = searchService.search(context, "search.resourceid:" + uuid.toString(),
                "lastModified", false, 0, 1000, "search.resourcetype:Item", "relationship.type:Person");
        } else {
            objects = searchService.search(context, "*:*", "lastModified", false, 0, 1000, "search.resourcetype:Item",
                    "relationship.type:Person");
        }
        List<Item> items = new ArrayList<Item>();
        if (objects != null) {
            for (IndexableObject o : objects) {
                items.add((Item) o.getIndexedObject());
            }
        }
        LOGGER.info("Found " + items.size() + " researcher(s)");
        return items;
    }
}
