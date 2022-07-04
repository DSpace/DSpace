/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class OpenaireEventsRunnable extends DSpaceRunnable<OpenaireEventsScriptConfiguration<OpenaireEventsRunnable>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenaireEventsRunnable.class);

    protected QAEventService qaEventService;

    protected String[] topicsToImport;

    protected ConfigurationService configurationService;

    protected String fileLocation;

    protected List<QAEvent> entries;

    protected Context context;

    @Override
    @SuppressWarnings({ "rawtypes" })
    public OpenaireEventsScriptConfiguration getScriptConfiguration() {
        OpenaireEventsScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("import-openaire-events", OpenaireEventsScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {
        DSpace dspace = new DSpace();

        qaEventService =  dspace.getSingletonService(QAEventService.class);
        if (qaEventService == null) {
            LOGGER.error("qaEventService is NULL. Error in spring configuration");
            throw new IllegalStateException();
        } else {
            LOGGER.debug("qaEventService correctly loaded");
        }

        configurationService = dspace.getConfigurationService();

        topicsToImport = configurationService.getArrayProperty("qaevents.openaire.import.topic");

        fileLocation = commandLine.getOptionValue("f");

    }

    @Override
    public void internalRun() throws Exception {

        if (StringUtils.isEmpty(fileLocation)) {
            LOGGER.info("No file location was entered");
            System.exit(1);
        }

        context = new Context();

        ObjectMapper jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            this.entries = jsonMapper.readValue(getQAEventsInputStream(), new TypeReference<List<QAEvent>>() {
            });
        } catch (IOException e) {
            LOGGER.error("File is not found or not readable: " + fileLocation, e);
            System.exit(1);
        }

        for (QAEvent entry : entries) {
            entry.setSource(QAEvent.OPENAIRE_SOURCE);
            if (!StringUtils.equalsAny(entry.getTopic(), topicsToImport)) {
                LOGGER.info("Skip event for topic " + entry.getTopic() + " is not allowed in the oaire-qaevents.cfg");
                continue;
            }
            try {
                qaEventService.store(context, entry);
            } catch (RuntimeException e) {
                handler.logWarning(getRootCauseMessage(e));
            }
        }

    }

    /**
     * Obtain an InputStream from the runnable instance.
     * @return
     * @throws Exception
     */
    protected InputStream getQAEventsInputStream() throws Exception {

        this.assignCurrentUserInContext();

        InputStream inputStream = handler.getFileStream(context, fileLocation)
                .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                    + "found for filename: " + fileLocation));

        return inputStream;
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }
}
