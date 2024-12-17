/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.script;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.dspace.core.Constants.ITEM;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import eu.dnetlib.broker.BrokerClient;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.HandleServiceImpl;
import org.dspace.handle.service.HandleService;
import org.dspace.qaevent.service.OpenaireClientFactory;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to perfom a QAEvents import from a
 * json file. The JSON file contains an array of JSON Events, where each event
 * has the following structure. The message attribute follows the structure
 * documented at
 * @see <a href="https://graph.openaire.eu/docs/category/entities" target="_blank"> see </a>
 * 
 * <code> <br/>
 * { <br/>
 * "originalId": "oai:www.openstarts.units.it:10077/21838",<br/>
 * "title": "Egypt, crossroad of translations and literary interweavings", <br/>
 * "topic": "ENRICH/MORE/PROJECT", <br/>
 * "trust": 1.0, <br/>
 * "message": { <br/>
 * "projects[0].acronym": "PAThs", <br/>
 * "projects[0].code": "687567", <br/>
 * "projects[0].funder": "EC",<br/>
 * "projects[0].fundingProgram": "H2020", <br/>
 * "projects[0].jurisdiction": "EU",<br/>
 * "projects[0].openaireId": "40|corda__h2020::6e32f5eb912688f2424c68b851483ea4", <br/>
 * "projects[0].title": "Tracking Papyrus and Parchment Paths" <br/>
 * } <br/>
 * }
 * </code>
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4Science.it)
 *
 */
public class OpenaireEventsImport
    extends DSpaceRunnable<OpenaireEventsImportScriptConfiguration<OpenaireEventsImport>> {

    private HandleService handleService;

    private QAEventService qaEventService;

    private String[] topicsToImport;

    private ConfigurationService configurationService;

    private BrokerClient brokerClient;

    private ObjectMapper jsonMapper;

    private URL openaireBrokerURL;

    private String fileLocation;

    private String email;

    private Context context;

    @Override
    @SuppressWarnings({ "rawtypes" })
    public OpenaireEventsImportScriptConfiguration getScriptConfiguration() {
        OpenaireEventsImportScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("import-openaire-events", OpenaireEventsImportScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {

        jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        DSpace dspace = new DSpace();
        handleService = dspace.getSingletonService(HandleServiceImpl.class);
        qaEventService = dspace.getSingletonService(QAEventService.class);
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        brokerClient = OpenaireClientFactory.getInstance().getBrokerClient();

        topicsToImport = configurationService.getArrayProperty("qaevents.openaire.import.topic");
        openaireBrokerURL = getOpenaireBrokerUri();

        fileLocation = commandLine.getOptionValue("f");
        email = commandLine.getOptionValue("e");

    }

    @Override
    public void internalRun() throws Exception {

        if (StringUtils.isAllBlank(fileLocation, email)) {
            throw new IllegalArgumentException("One parameter between the location of the file and the email "
                + "must be entered to proceed with the import.");
        }

        if (StringUtils.isNoneBlank(fileLocation, email)) {
            throw new IllegalArgumentException("Only one parameter between the location of the file and the email "
                + "must be entered to proceed with the import.");
        }

        context = new Context();
        assignCurrentUserInContext();

        try {
            importOpenaireEvents();
        } catch (Exception ex) {
            handler.logError("A not recoverable error occurs during OPENAIRE events import: " + getMessage(ex), ex);
            throw ex;
        }

    }

    /**
     * Read the OPENAIRE events from the given JSON file or directly from the
     * OPENAIRE broker and try to store them.
     */
    private void importOpenaireEvents() throws Exception {

        if (StringUtils.isNotBlank(fileLocation)) {
            handler.logInfo("Trying to read the QA events from the provided file");
            importOpenaireEventsFromFile();
        } else {
            handler.logInfo("Trying to read the QA events from the OPENAIRE broker");
            importOpenaireEventsFromBroker();
        }

    }

    /**
     * Read the OPENAIRE events from the given file location and try to store them.
     */
    private void importOpenaireEventsFromFile() throws Exception {

        InputStream eventsFileInputStream = getQAEventsFileInputStream();
        List<QAEvent> qaEvents = readOpenaireQAEventsFromJson(eventsFileInputStream);

        handler.logInfo("Found " + qaEvents.size() + " events in the given file");

        storeOpenaireQAEvents(qaEvents);

    }

    /**
     * Import the OPENAIRE events from the Broker using the subscription related to
     * the given email and try to store them.
     */
    private void importOpenaireEventsFromBroker() {

        List<String> subscriptionIds = listEmailSubscriptions();

        handler.logInfo("Found " + subscriptionIds.size() + " subscriptions related to the given email");

        for (String subscriptionId : subscriptionIds) {

            List<QAEvent> events = readOpenaireQAEventsFromBroker(subscriptionId);

            handler.logInfo("Found " + events.size() + " events from the subscription " + subscriptionId);

            storeOpenaireQAEvents(events);

        }
    }

    /**
     * Obtain an InputStream from the runnable instance.
     */
    private InputStream getQAEventsFileInputStream() throws Exception {
        return handler.getFileStream(context, fileLocation)
            .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                + "found for filename: " + fileLocation));
    }

    /**
     * Read all the QAEvent from the OPENAIRE Broker related to the subscription
     * with the given id.
     */
    private List<QAEvent> readOpenaireQAEventsFromBroker(String subscriptionId) {

        try {
            InputStream eventsInputStream = getEventsBySubscriptions(subscriptionId);
            return readOpenaireQAEventsFromJson(eventsInputStream);
        } catch (Exception ex) {
            handler.logError("An error occurs downloading the events related to the subscription "
                + subscriptionId + ": " + getMessage(ex), ex);
        }

        return List.of();

    }

    /**
     * Read all the QAEvent present in the given input stream.
     *
     * @return the QA events to be imported
     */
    private List<QAEvent> readOpenaireQAEventsFromJson(InputStream inputStream) throws Exception {
        return jsonMapper.readValue(inputStream, new TypeReference<List<QAEvent>>() {
        });
    }

    /**
     * Store the given QAEvents.
     *
     * @param events the event to be stored
     */
    private void storeOpenaireQAEvents(List<QAEvent> events) {
        for (QAEvent event : events) {
            try {
                final String resourceUUID = getResourceUUID(context, event.getOriginalId());
                if (resourceUUID == null) {
                    throw new IllegalArgumentException("Skipped event " + event.getEventId() +
                        " related to the oai record " + event.getOriginalId() + " as the record was not found");
                }
                event.setTarget(resourceUUID);
                storeOpenaireQAEvent(event);
            } catch (RuntimeException | SQLException e) {
                handler.logWarning("An error occurs storing the event with id "
                    + event.getEventId() + ": " + getMessage(e));
            }
        }
    }

    private String getResourceUUID(Context context, String originalId) throws IllegalStateException, SQLException {
        String id = getHandleFromOriginalId(originalId);
        if (StringUtils.isNotBlank(id)) {
            DSpaceObject dso = handleService.resolveToObject(context, id);
            if (dso != null && dso.getType() == ITEM) {
                Item item = (Item) dso;
                final String itemUuid = item.getID().toString();
                context.uncacheEntity(item);
                return itemUuid;
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Malformed originalId " + originalId);
        }
    }

    // oai:www.openstarts.units.it:10077/21486
    private String getHandleFromOriginalId(String originalId) {
        int startPosition = originalId.lastIndexOf(':');
        if (startPosition != -1) {
            return originalId.substring(startPosition + 1, originalId.length());
        } else {
            return originalId;
        }
    }

    /**
     * Store the given QAEvent, skipping it if it is not supported.
     *
     * @param event the event to be stored
     */
    private void storeOpenaireQAEvent(QAEvent event) {

        if (!StringUtils.equalsAny(event.getTopic(), topicsToImport)) {
            handler.logWarning("Event for topic " + event.getTopic() + " is not allowed in the qaevents.cfg");
            return;
        }

        event.setSource(QAEvent.OPENAIRE_SOURCE);

        qaEventService.store(context, event);

    }

    /**
     * Download the events related to the given subscription from the OPENAIRE broker.
     *
     * @param subscriptionId the subscription id
     * @return an input stream from which to read the events in json format
     */
    private InputStream getEventsBySubscriptions(String subscriptionId) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        brokerClient.downloadEvents(openaireBrokerURL, subscriptionId, outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * Takes all the subscription related to the given email from the OPENAIRE
     * broker.
     */
    private List<String> listEmailSubscriptions() {
        try {
            return brokerClient.listSubscriptions(openaireBrokerURL, email);
        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occurs retriving the subscriptions "
                + "from the OPENAIRE broker: " + getMessage(ex), ex);
        }
    }

    private URL getOpenaireBrokerUri() {
        try {
            return new URL(configurationService.getProperty("qaevents.openaire.broker-url", "http://api.openaire.eu/broker"));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("The configured OPENAIRE broker URL is not valid.", e);
        }
    }

    /**
     * Get the root exception message from the given exception.
     */
    private String getMessage(Exception ex) {
        String message = ExceptionUtils.getRootCauseMessage(ex);
        // Remove the Exception name from the message
        return isNotBlank(message) ? substringAfter(message, ":").trim() : "";
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }
}
