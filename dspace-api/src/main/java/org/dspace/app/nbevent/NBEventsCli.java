/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.nbevent.service.impl.NBEventServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactoryImpl;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.SolrImportExportException;
import org.dspace.utils.DSpace;



public class NBEventsCli {

    public static final String ORIGINAL_ID = "original_id";
    public static final String TITLE = "title";
    public static final String TOPIC = "topic";
    public static final String TRUST = "trust";
    public static final String MESSAGE = "message";
    public static final String EVENT_ID = "event_id";
    public static final String RESOURCE_UUID = "resource_uuid";
    public static final String LAST_UPDATE = "last_update";

    public static void main(String[] args) throws Exception {
        DSpace dspace = new DSpace();
        NBEventServiceImpl nbEventService = dspace.getServiceManager().getServiceByName("nbEventService",
                NBEventServiceImpl.class);
        if (nbEventService == null) {
            System.err.println("nbEventService is NULL. Error in spring configuration");
        } else {
            System.out.println("nbEventService is correctly loaded");
        }
        Context context = new Context();
        CommandLineParser parser = new PosixParser();
        Options options = createCommandLineOptions();
        CommandLine line = parser.parse(options, args);
        String fileLocation = getFileLocationFromCommandLine(line);
        checkHelpEntered(options, line);
        List<NBEvent> entries;
        try {
            entries = getEntriesFromFile(fileLocation);
            try {
                for (NBEvent entry : entries) {
                    nbEventService.store(context, entry);
                }
                System.exit(0);
            } catch (SolrImportExportException | SolrServerException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } catch (JsonParseException | JsonMappingException e) {
            System.err.println("Unable to parse the file content.");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("File is not found or not readable");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<NBEvent> getEntriesFromFile(String fileLocation)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return jsonMapper.readValue(new File(fileLocation), new TypeReference<List<NBEvent>>() {
        });
    }

    protected static Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption("f", "file", true, "the location for the file containing the json data");
        return options;
    }

    private static void checkHelpEntered(Options options, CommandLine line) {
        if (line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Import Notification event json file", options);
            System.exit(0);
        }
    }

    private static String getFileLocationFromCommandLine(CommandLine line) {
        String query = line.getOptionValue("f");
        if (StringUtils.isEmpty(query)) {
            System.out.println("No file location was entered");
            System.exit(1);
        }
        return query;
    }

    private SolrClient getSolr() {

        String solrService = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("openstar.search.server");
        return new HttpSolrClient.Builder(solrService).build();
    }

    public void store(Context context, List<NBEvent> entries)
            throws SolrImportExportException, SolrServerException, IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        for (NBEvent dto : entries) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField(EVENT_ID, dto.getEventId());
            doc.addField(ORIGINAL_ID, dto.getOriginalId());
            doc.addField(TITLE, dto.getTitle());
            doc.addField(TOPIC, dto.getTopic());
            doc.addField(TRUST, dto.getTrust());
            doc.addField(MESSAGE, dto.getMessage());
            doc.addField(LAST_UPDATE, System.currentTimeMillis());
            doc.addField(RESOURCE_UUID, getResourceUUID(context, dto.getOriginalId()));
            updateRequest.add(doc);
        }
        updateRequest.process(getSolr());
        getSolr().commit();
    }

    private String getResourceUUID(Context context, String originalId) throws SolrImportExportException {
        try {
            String id = getIdFromOriginalId(originalId);
            if (id != null) {
                Item item = (Item) HandleServiceFactoryImpl.getInstance().getHandleService().resolveToObject(context,
                        id);
                if (item != null) {
                    return item.getID().toString();
                } else {
                    System.err.println("OriginalID not found");
                    throw new RuntimeException();
                }
            } else {
                System.err.println("OriginalID not found");
                throw new RuntimeException();
            }
        } catch (RuntimeException | SQLException e) {
            System.err.println("OriginalID not found");
            throw new SolrImportExportException("Original ID is not valid: ");
        }
    }

    // oai:www.openstarts.units.it:10077/21486
    private String getIdFromOriginalId(String originalId) {
        Integer startPosition = originalId.lastIndexOf(':');
        if (startPosition != -1) {
            return originalId.substring(startPosition, originalId.length());
        } else {
            return null;
        }
    }
}
