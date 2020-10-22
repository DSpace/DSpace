/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.suggestion.oaire.OAIREPublicationApproverServiceImpl;
import org.dspace.app.suggestion.oaire.OAIREPublicationLoader;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

public class OAIREPublicationLoaderCli {

    private static final String SOURCE = "source";
    private static final String SUGGESTION_ID = "suggestion_id";
    private static final String TARGET_ID = "target_id";
    private static final String TITLE = "title";
    private static final String DATE = "date";
    private static final String CONTRIBUTORS = "contributors";
    private static final String ABSTRACT = "abstract";
    private static final String CATEGORY = "category";
    private static final String EXTERNAL_URI = "external-uri";
    private static final String REJECTED = "rejected";

    private static DSpace dspace = null;
    private static SolrClient solrSuggestionClient = null;
    private static OAIREPublicationLoader oairePublicationLoader = null;
    private static OAIREPublicationApproverServiceImpl oaireApprover = null;

    private OAIREPublicationLoaderCli() {
    }

    public static void main(String[] args) throws Exception {
        try (Context context = new Context()) {
            CommandLineParser parser = new PosixParser();
            Options options = createCommandLineOptions();
            CommandLine line = parser.parse(options, args);
            String profile = getProfileFromCommandLine(line);
            List<Item> researchers = null;
            if (profile == null) {
                System.out.println("No argument for -s, process all profile");
                researchers = getResearchers(context, UUID.fromString(profile));
            } else {
                System.out.println("Process eperson item with UUID " + profile);
                researchers = getResearchers(context, UUID.fromString(profile));
            }

            // load all author publication
            for (Item researcher : researchers) {
                List<ImportRecord> metadata = getOAIREPublicationLoader().getImportRecords(researcher);
                List<ImportRecord> records = getOAireApprover().approve(researcher, metadata);
                saveAuthorRecords(researcher, records);
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getProfileFromCommandLine(CommandLine line) {
        String query = line.getOptionValue("s");
        if (StringUtils.isEmpty(query)) {
            return null;
        }
        return query;
    }

    protected static Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption("s", "person", true, "UUID of the author object");
        return options;
    }

    private static void checkHelpEntered(Options options, CommandLine line) {
        if (line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Import Notification event json file", options);
            System.exit(0);
        }
    }

    private static void saveAuthorRecords(Item researcher, List<ImportRecord> records)
            throws SolrServerException, IOException {
        Collection<SolrInputDocument> docs = new ArrayList<>();
        for (ImportRecord record : records) {
            docs.add(translateImportRecordToSolrDocument(researcher, record));
        }
        getSuggestionSolr().add(docs);
        getSuggestionSolr().commit();
    }

    private static SolrInputDocument translateImportRecordToSolrDocument(Item item, ImportRecord record) {
        // FIXME: externalize this configuration
        SolrInputDocument document = new SolrInputDocument();
        document.addField(SOURCE, getFirstEntryByMetadatum(record, "dc", "source", null));
        document.addField(SUGGESTION_ID, getFirstEntryByMetadatum(record, "dc", "identifier", "other"));
        document.addField(TARGET_ID, item.getID().toString());
        document.addField(TITLE, getFirstEntryByMetadatum(record, "dc", "title", null));
        document.addField(DATE, getFirstEntryByMetadatum(record, "dc", "date", "issued"));
        document.addField(CONTRIBUTORS, getAllEntriesByMetadatum(record, "dc", "contributor", "author"));
        document.addField(ABSTRACT, getFirstEntryByMetadatum(record, "dc", "description", "abstract"));
        // TODO: which metadatum here?
        document.addField(CATEGORY, "");
        // FIXME: api call are correct?
        document.addField(EXTERNAL_URI, "");
        document.addField(REJECTED, "false");
        return document;
    }

    private static String[] getAllEntriesByMetadatum(ImportRecord record, String schema, String element,
            String qualifier) {
        Collection<MetadatumDTO> metadata = record.getValue(schema, element, qualifier);
        Iterator<MetadatumDTO> iterator = metadata.iterator();
        String[] values = new String[metadata.size()];
        int index = 0;
        while (iterator.hasNext()) {
            values[index] = iterator.next().getValue();
            index++;
        }
        return values;
    }

    private static String getFirstEntryByMetadatum(ImportRecord record, String schema, String element,
            String qualifier) {
        Collection<MetadatumDTO> metadata = record.getValue(schema, element, qualifier);
        Iterator<MetadatumDTO> iterator = metadata.iterator();
        return iterator.next().getValue();
    }

    /**
     * Return an instance of OAIREPublicationApproverServiceImpl
     * 
     * @return an instance of OAIREPublicationApproverServiceImpl
     */
    public static OAIREPublicationApproverServiceImpl getOAireApprover() {
        if (oaireApprover == null) {
            oaireApprover = getDSpace().getServiceManager().getServiceByName(
                    "org.dspace.app.suggestion.oaire.OAIREPublicationApproverService",
                    OAIREPublicationApproverServiceImpl.class);
        }
        return oaireApprover;
    }

    /**
     * return an instance of OAIREPublicationLoader
     * 
     * @return an instance of OAIREPublicationLoader
     */
    public static OAIREPublicationLoader getOAIREPublicationLoader() {
        if (oairePublicationLoader == null) {
            oairePublicationLoader = getDSpace().getServiceManager().getServiceByName(
                    "org.dspace.app.suggestion.oaire.OAIREPublicationLoader", OAIREPublicationLoader.class);
        }
        return oairePublicationLoader;
    }

    /**
     * Get the Item(s) which map a researcher from Solr. If the uuid is specified,
     * the researcher with this UUID will be chosen. If the uuid doesn't match any
     * researcher, the method returns an empty array list. If uuid is null, all
     * research will be return.
     * 
     * @param context DSpace context
     * @param uuid    uuid of the researcher. If null, all researcher will be
     *                returned.
     * @return the researcher with specified UUID or all researchers
     */
    @SuppressWarnings("rawtypes")
    private static List<Item> getResearchers(Context context, UUID uuid) {
        SearchService searchService = getDSpace().getSingletonService(SearchService.class);
        List<IndexableObject> objects = null;
        if (uuid != null) {
            // TODO: fix query here
            objects = searchService.search(context, "*:*", "lastModified", false, 0, 100, "search.resourcetype:Item",
                    "relationship.type:Person");
        } else {
            objects = searchService.search(context, "*:*", "lastModified", false, 0, 100, "search.resourcetype:Item",
                    "relationship.type:Person");
        }
        List<Item> items = new ArrayList<Item>();
        if (objects != null) {
            for (IndexableObject o : objects) {
                items.add((Item) o.getIndexedObject());
            }
        }
        return items;
    }

    /**
     * Get sorl client which use suggestion core
     * 
     * @return solr client
     */
    private static SolrClient getSuggestionSolr() {
        if (solrSuggestionClient == null) {
            String solrService = DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getProperty("suggestion.search.server");
            solrSuggestionClient = new HttpSolrClient.Builder(solrService).build();
        }
        return solrSuggestionClient;
    }

    /**
     * Get DSpace instance
     * 
     * @return Dspace instance
     */
    private static DSpace getDSpace() {
        if (dspace == null) {
            dspace = new DSpace();
        }
        return dspace;
    }
}
