/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.script;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.dspace.app.profile.OrcidSynchronizationMode.BATCH;
import static org.dspace.app.profile.OrcidSynchronizationMode.MANUAL;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.exception.OrcidValidationException;
import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.app.profile.OrcidSynchronizationMode;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Script that perform the bulk synchronization with ORCID registry of all the
 * ORCID queue records that has an owner that configure the synchronization mode
 * equals to BATCH.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidBulkPush extends DSpaceRunnable<OrcidBulkPushScriptConfiguration<OrcidBulkPush>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidBulkPush.class);

    private OrcidQueueService orcidQueueService;

    private OrcidHistoryService orcidHistoryService;

    private OrcidSynchronizationService orcidSynchronizationService;

    private ConfigurationService configurationService;

    private Context context;

    private Map<Item, OrcidSynchronizationMode> synchronizationModeByOwner = new HashMap<>();

    private boolean ignoreMaxAttempts = false;

    @Override
    public void setup() throws ParseException {
        OrcidServiceFactory orcidServiceFactory = OrcidServiceFactory.getInstance();
        this.orcidQueueService = orcidServiceFactory.getOrcidQueueService();
        this.orcidHistoryService = orcidServiceFactory.getOrcidHistoryService();
        this.orcidSynchronizationService = orcidServiceFactory.getOrcidSynchronizationService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        if (commandLine.hasOption('f')) {
            ignoreMaxAttempts = true;
        }

    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        assignCurrentUserInContext();

        try {
            context.turnOffAuthorisationSystem();
            performBulkSynchronization();
            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void performBulkSynchronization() throws SQLException {

        List<OrcidQueue> queueRecords = findQueueRecordsToSynchronize();
        handler.logInfo("Found " + queueRecords.size() + " queue records to synchronize with ORCID");

        for (OrcidQueue queueRecord : queueRecords) {
            performSynchronization(queueRecord);
        }

    }

    private List<OrcidQueue> findQueueRecordsToSynchronize() throws SQLException {
        return findQueueRecords().stream()
            .filter(record -> getOwnerSynchronizationMode(record.getOwner()) == BATCH)
            .collect(Collectors.toList());
    }

    private List<OrcidQueue> findQueueRecords() throws SQLException {
        if (ignoreMaxAttempts) {
            return orcidQueueService.findAll(context);
        } else {
            int attempts = configurationService.getIntProperty("orcid.bulk-synchronization.max-attempts");
            return orcidQueueService.findByAttemptsLessThan(context, attempts);
        }
    }

    private void performSynchronization(OrcidQueue queueRecord) {

        try {

            queueRecord = reload(queueRecord);

            handler.logInfo(getOperationInfoMessage(queueRecord));

            OrcidHistory orcidHistory = orcidHistoryService.synchronizeWithOrcid(context, queueRecord, false);

            handler.logInfo(getSynchronizationResultMessage(orcidHistory));

            commitTransaction();

        } catch (OrcidValidationException ex) {
            rollbackTransaction();
            handler.logError(getValidationErrorMessage(ex));
        } catch (Exception ex) {
            rollbackTransaction();
            String errorMessage = getUnexpectedErrorMessage(ex);
            LOGGER.error(errorMessage, ex);
            handler.logError(errorMessage);
        } finally {
            incrementAttempts(queueRecord);
        }

    }

    private OrcidSynchronizationMode getOwnerSynchronizationMode(Item owner) {
        OrcidSynchronizationMode synchronizationMode = synchronizationModeByOwner.get(owner);
        if (synchronizationMode == null) {
            synchronizationMode = orcidSynchronizationService.getSynchronizationMode(owner).orElse(MANUAL);
            synchronizationModeByOwner.put(owner, synchronizationMode);
        }
        return synchronizationMode;
    }

    private String getOperationInfoMessage(OrcidQueue record) {

        UUID ownerId = record.getOwner().getID();
        String putCode = record.getPutCode();
        String type = record.getRecordType();

        if (record.getOperation() == null) {
            return "Synchronization of " + type + " data for profile with ID: " + ownerId;
        }

        switch (record.getOperation()) {
            case INSERT:
                return "Addition of " + type + " for profile with ID: " + ownerId;
            case UPDATE:
                return "Update of " + type + " for profile with ID: " + ownerId + " by put code " + putCode;
            case DELETE:
                return "Deletion of " + type + " for profile with ID: " + ownerId + " by put code " + putCode;
            default:
                return "Synchronization of " + type + " data for profile with ID: " + ownerId;
        }

    }

    private String getSynchronizationResultMessage(OrcidHistory orcidHistory) {

        String message = "History record created with status " + orcidHistory.getStatus();

        switch (orcidHistory.getStatus()) {
            case 201:
            case 200:
            case 204:
                message += ". The operation was completed successfully";
                break;
            case 400:
                message += ". The resource sent to ORCID registry is not valid";
                break;
            case 404:
                message += ". The resource does not exists anymore on the ORCID registry";
                break;
            case 409:
                message += ". The resource is already present on the ORCID registry";
                break;
            default:
                break;
        }

        return message;

    }

    private String getValidationErrorMessage(OrcidValidationException ex) {
        return ex.getMessage();
    }

    private String getUnexpectedErrorMessage(Exception ex) {
        return "An unexpected error occurs during the synchronization: " + getRootMessage(ex);
    }

    private void incrementAttempts(OrcidQueue queueRecord) {
        queueRecord = reload(queueRecord);
        if (queueRecord == null) {
            return;
        }

        try {
            queueRecord.setAttempts(queueRecord.getAttempts() != null ? queueRecord.getAttempts() + 1 : 1);
            orcidQueueService.update(context, queueRecord);
            commitTransaction();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private OrcidQueue reload(OrcidQueue queueRecord) {
        try {
            return context.reloadEntity(queueRecord);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void commitTransaction() {
        try {
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void rollbackTransaction() {
        try {
            context.rollback();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getRootMessage(Exception ex) {
        String message = ExceptionUtils.getRootCauseMessage(ex);
        return isNotEmpty(message) ? message.substring(message.indexOf(":") + 1).trim() : "Generic error";
    }

    @Override
    @SuppressWarnings("unchecked")
    public OrcidBulkPushScriptConfiguration<OrcidBulkPush> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("orcid-bulk-push",
            OrcidBulkPushScriptConfiguration.class);
    }

}
