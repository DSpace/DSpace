/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

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
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link DSpaceRunnable} to perfom a NBEvents import from file.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class NBEventsRunnable extends DSpaceRunnable<NBEventsScriptConfiguration<NBEventsRunnable>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NBEventsRunnable.class);

    protected NBEventService nbEventService;

    protected String[] topicsToImport;

    protected ConfigurationService configurationService;

    protected String fileLocation;

    protected List<NBEvent> entries;

    protected Context context;

    @Override
    @SuppressWarnings({ "rawtypes" })
    public NBEventsScriptConfiguration getScriptConfiguration() {
        NBEventsScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("import-nbevents", NBEventsScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {
        DSpace dspace = new DSpace();

        nbEventService =  dspace.getSingletonService(NBEventService.class);
        if (nbEventService == null) {
            LOGGER.error("nbEventService is NULL. Error in spring configuration");
            throw new IllegalStateException();
        } else {
            LOGGER.debug("nbEventService correctly loaded");
        }

        configurationService = dspace.getConfigurationService();

        topicsToImport = configurationService.getArrayProperty("oaire-nbevents.import.topic");

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
            this.entries = jsonMapper.readValue(getNBEventsInputStream(), new TypeReference<List<NBEvent>>() {
            });
        } catch (IOException e) {
            LOGGER.error("File is not found or not readable: " + fileLocation);
            e.printStackTrace();
            System.exit(1);
        }

        for (NBEvent entry : entries) {
            if (!StringUtils.equalsAny(entry.getTopic(), topicsToImport)) {
                LOGGER.info("Skip event for topic " + entry.getTopic() + " is not allowed in the oaire-nbevents.cfg");
                continue;
            }
            try {
                nbEventService.store(context, entry);
            } catch (RuntimeException e) {
                System.out.println("Skip event for originalId " + entry.getOriginalId() + ": " + e.getMessage());
            }
        }

    }

    /**
     * Obtain an InputStream from the runnable instance.
     * @return
     * @throws Exception
     */
    protected InputStream getNBEventsInputStream() throws Exception {

        this.assignCurrentUserInContext();
        this.assignSpecialGroupsInContext();

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

    private void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

}
