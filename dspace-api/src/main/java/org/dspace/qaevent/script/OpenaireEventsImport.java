/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.script;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to perfom a QAEvents import from a
 * json file. The JSON file contains an array of JSON Events, where each event
 * has the following structure
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
 *
 */
public class OpenaireEventsImport
    extends DSpaceRunnable<OpenaireEventsImportScriptConfiguration<OpenaireEventsImport>> {

    private QAEventService qaEventService;

    private String[] topicsToImport;

    private ConfigurationService configurationService;

    private String fileLocation;

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
        DSpace dspace = new DSpace();

        qaEventService =  dspace.getSingletonService(QAEventService.class);
        if (qaEventService == null) {
            throw new IllegalStateException("qaEventService is NULL. Error in spring configuration");
        }

        configurationService = dspace.getConfigurationService();

        topicsToImport = configurationService.getArrayProperty("qaevents.openaire.import.topic");

        fileLocation = commandLine.getOptionValue("f");

    }

    @Override
    public void internalRun() throws Exception {

        if (StringUtils.isEmpty(fileLocation)) {
            throw new IllegalArgumentException("No file location was entered");
        }

        context = new Context();
        assignCurrentUserInContext();

        try {
            runOpenaireEventsImport();
        } catch (Exception ex) {
            handler.logError("A not recoverable error occurs during OPENAIRE events import. "
                + ExceptionUtils.getRootCauseMessage(ex), ex);
            throw ex;
        }

    }

    /**
     * Read the OPENAIRE events from the given JSON file and try to store them.
     */
    private void runOpenaireEventsImport() {

        QAEvent[] qaEvents = readOpenaireQAEventsFromJsonFile();
        handler.logInfo("Found " + qaEvents.length + " events to store");

        for (QAEvent event : qaEvents) {
            try {
                storeOpenaireQAEvent(event);
            } catch (RuntimeException e) {
                handler.logWarning(getRootCauseMessage(e));
            }
        }

    }

    /**
     * Read all the QAEvent present in the given file.
     *
     * @return           the QA events to be imported
     * @throws Exception if an oerror occurs during the file reading
     */
    private QAEvent[] readOpenaireQAEventsFromJsonFile() {
        try {

            ObjectMapper jsonMapper = new JsonMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return jsonMapper.readValue(getQAEventsFileInputStream(), QAEvent[].class);

        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occurs parsing the OPENAIRE QA events json", ex);
        }
    }

    /**
     * Obtain an InputStream from the runnable instance.
     * @return
     * @throws Exception
     */
    private InputStream getQAEventsFileInputStream() throws Exception {
        return handler.getFileStream(context, fileLocation)
            .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                + "found for filename: " + fileLocation));
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

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }
}
