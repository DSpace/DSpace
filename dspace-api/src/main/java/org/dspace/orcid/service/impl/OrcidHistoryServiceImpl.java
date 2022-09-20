/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service.impl;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.orcid.OrcidHistory;
import org.dspace.orcid.OrcidOperation;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidResponse;
import org.dspace.orcid.dao.OrcidHistoryDAO;
import org.dspace.orcid.dao.OrcidQueueDAO;
import org.dspace.orcid.exception.OrcidClientException;
import org.dspace.orcid.exception.OrcidValidationException;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.model.OrcidProfileSectionType;
import org.dspace.orcid.model.validator.OrcidValidationError;
import org.dspace.orcid.model.validator.OrcidValidator;
import org.dspace.orcid.service.MetadataSignatureGenerator;
import org.dspace.orcid.service.OrcidEntityFactoryService;
import org.dspace.orcid.service.OrcidHistoryService;
import org.dspace.orcid.service.OrcidProfileSectionFactoryService;
import org.dspace.orcid.service.OrcidTokenService;
import org.orcid.jaxb.model.v3.release.record.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidHistoryService}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidHistoryServiceImpl implements OrcidHistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidHistoryServiceImpl.class);

    @Autowired
    private OrcidHistoryDAO orcidHistoryDAO;

    @Autowired
    private OrcidQueueDAO orcidQueueDAO;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidProfileSectionFactoryService profileFactoryService;

    @Autowired
    private OrcidEntityFactoryService activityFactoryService;

    @Autowired
    private MetadataSignatureGenerator metadataSignatureGenerator;

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private OrcidValidator orcidValidator;

    @Autowired
    private OrcidTokenService orcidTokenService;

    @Override
    public OrcidHistory find(Context context, int id) throws SQLException {
        return orcidHistoryDAO.findByID(context, OrcidHistory.class, id);
    }

    @Override
    public List<OrcidHistory> findAll(Context context) throws SQLException {
        return orcidHistoryDAO.findAll(context, OrcidHistory.class);
    }

    @Override
    public List<OrcidHistory> findByProfileItemOrEntity(Context context, Item profileItem) throws SQLException {
        return orcidHistoryDAO.findByProfileItemOrEntity(context, profileItem);
    }

    @Override
    public OrcidHistory create(Context context, Item profileItem, Item entity) throws SQLException {
        OrcidHistory orcidHistory = new OrcidHistory();
        orcidHistory.setEntity(entity);
        orcidHistory.setProfileItem(profileItem);
        return orcidHistoryDAO.create(context, orcidHistory);
    }

    @Override
    public void delete(Context context, OrcidHistory orcidHistory) throws SQLException {
        orcidHistoryDAO.delete(context, orcidHistory);
    }

    @Override
    public void update(Context context, OrcidHistory orcidHistory) throws SQLException {
        if (orcidHistory != null) {
            orcidHistoryDAO.save(context, orcidHistory);
        }
    }

    @Override
    public Optional<String> findLastPutCode(Context context, Item profileItem, Item entity) throws SQLException {
        List<OrcidHistory> records = orcidHistoryDAO.findByProfileItemAndEntity(context, profileItem.getID(),
            entity.getID());
        return findLastPutCode(records, profileItem);
    }

    @Override
    public Map<Item, String> findLastPutCodes(Context context, Item entity) throws SQLException {
        Map<Item, String> profileItemAndPutCodeMap = new HashMap<Item, String>();

        List<OrcidHistory> orcidHistoryRecords = findByEntity(context, entity);
        for (OrcidHistory orcidHistoryRecord : orcidHistoryRecords) {
            Item profileItem = orcidHistoryRecord.getProfileItem();
            if (profileItemAndPutCodeMap.containsKey(profileItem)) {
                continue;
            }

            findLastPutCode(orcidHistoryRecords, profileItem)
                .ifPresent(putCode -> profileItemAndPutCodeMap.put(profileItem, putCode));
        }

        return profileItemAndPutCodeMap;
    }

    @Override
    public List<OrcidHistory> findByEntity(Context context, Item entity) throws SQLException {
        return orcidHistoryDAO.findByEntity(context, entity);
    }

    @Override
    public List<OrcidHistory> findSuccessfullyRecordsByEntityAndType(Context context,
        Item entity, String recordType) throws SQLException {
        return orcidHistoryDAO.findSuccessfullyRecordsByEntityAndType(context, entity, recordType);
    }

    @Override
    public OrcidHistory synchronizeWithOrcid(Context context, OrcidQueue orcidQueue, boolean forceAddition)
        throws SQLException {

        Item profileItem = orcidQueue.getProfileItem();

        String orcid = getMetadataValue(profileItem, "person.identifier.orcid")
            .orElseThrow(() -> new IllegalArgumentException(
                format("The related profileItem item (id = %s) does not have an orcid", profileItem.getID())));

        String token = getAccessToken(context, profileItem)
            .orElseThrow(() -> new IllegalArgumentException(
                format("The related profileItem item (id = %s) does not have an access token", profileItem.getID())));

        OrcidOperation operation = calculateOperation(orcidQueue, forceAddition);

        try {

            OrcidResponse response = synchronizeWithOrcid(context, orcidQueue, orcid, token, operation);
            OrcidHistory orcidHistory = createHistoryRecordFromOrcidResponse(context, orcidQueue, operation, response);
            orcidQueueDAO.delete(context, orcidQueue);
            return orcidHistory;

        } catch (OrcidValidationException ex) {
            throw ex;
        } catch (OrcidClientException ex) {
            LOGGER.error("An error occurs during the orcid synchronization of ORCID queue " + orcidQueue, ex);
            return createHistoryRecordFromOrcidError(context, orcidQueue, operation, ex);
        } catch (RuntimeException ex) {
            LOGGER.warn("An unexpected error occurs during the orcid synchronization of ORCID queue " + orcidQueue, ex);
            return createHistoryRecordFromGenericError(context, orcidQueue, operation, ex);
        }

    }

    private OrcidResponse synchronizeWithOrcid(Context context, OrcidQueue orcidQueue, String orcid, String token,
        OrcidOperation operation) throws SQLException {
        if (isProfileSectionType(orcidQueue)) {
            return synchronizeProfileDataWithOrcid(context, orcidQueue, orcid, token, operation);
        } else if (isEntityType(orcidQueue)) {
            return synchronizeEntityWithOrcid(context, orcidQueue, orcid, token, operation);
        } else {
            throw new IllegalArgumentException("The type of the given queue record could not be determined");
        }
    }

    private OrcidOperation calculateOperation(OrcidQueue orcidQueue, boolean forceAddition) {
        OrcidOperation operation = orcidQueue.getOperation();
        if (operation == null) {
            throw new IllegalArgumentException("The orcid queue record with id " + orcidQueue.getID()
                + "  has no operation defined");
        }
        return operation != OrcidOperation.DELETE && forceAddition ? OrcidOperation.INSERT : operation;
    }

    private OrcidResponse synchronizeEntityWithOrcid(Context context, OrcidQueue orcidQueue,
        String orcid, String token, OrcidOperation operation) throws SQLException {
        if (operation == OrcidOperation.DELETE) {
            return deleteEntityOnOrcid(context, orcid, token, orcidQueue);
        } else {
            return sendEntityToOrcid(context, orcid, token, orcidQueue, operation == OrcidOperation.UPDATE);
        }
    }

    private OrcidResponse synchronizeProfileDataWithOrcid(Context context, OrcidQueue orcidQueue,
        String orcid, String token, OrcidOperation operation) throws SQLException {

        if (operation == OrcidOperation.INSERT) {
            return sendProfileDataToOrcid(context, orcid, token, orcidQueue);
        } else {
            return deleteProfileDataOnOrcid(context, orcid, token, orcidQueue);
        }

    }

    private OrcidResponse sendEntityToOrcid(Context context, String orcid, String token, OrcidQueue orcidQueue,
        boolean toUpdate) {

        Activity activity = activityFactoryService.createOrcidObject(context, orcidQueue.getEntity());

        List<OrcidValidationError> validationErrors = orcidValidator.validate(activity);
        if (CollectionUtils.isNotEmpty(validationErrors)) {
            throw new OrcidValidationException(validationErrors);
        }

        if (toUpdate) {
            activity.setPutCode(getPutCode(orcidQueue));
            return orcidClient.update(token, orcid, activity, orcidQueue.getPutCode());
        } else {
            return orcidClient.push(token, orcid, activity);
        }

    }

    private OrcidResponse sendProfileDataToOrcid(Context context, String orcid, String token, OrcidQueue orcidQueue) {

        OrcidProfileSectionType recordType = OrcidProfileSectionType.fromString(orcidQueue.getRecordType());
        String signature = orcidQueue.getMetadata();
        Item person = orcidQueue.getEntity();

        List<MetadataValue> metadataValues = metadataSignatureGenerator.findBySignature(context, person, signature);
        Object orcidObject = profileFactoryService.createOrcidObject(context, metadataValues, recordType);

        List<OrcidValidationError> validationErrors = orcidValidator.validate(orcidObject);
        if (CollectionUtils.isNotEmpty(validationErrors)) {
            throw new OrcidValidationException(validationErrors);
        }

        return orcidClient.push(token, orcid, orcidObject);
    }

    private OrcidResponse deleteProfileDataOnOrcid(Context context, String orcid, String token, OrcidQueue orcidQueue) {
        OrcidProfileSectionType recordType = OrcidProfileSectionType.fromString(orcidQueue.getRecordType());
        return orcidClient.deleteByPutCode(token, orcid, orcidQueue.getPutCode(), recordType.getPath());
    }

    private OrcidResponse deleteEntityOnOrcid(Context context, String orcid, String token, OrcidQueue orcidQueue) {
        OrcidEntityType recordType = OrcidEntityType.fromEntityType(orcidQueue.getRecordType());
        return orcidClient.deleteByPutCode(token, orcid, orcidQueue.getPutCode(), recordType.getPath());
    }

    private OrcidHistory createHistoryRecordFromGenericError(Context context, OrcidQueue orcidQueue,
        OrcidOperation operation, RuntimeException ex) throws SQLException {
        return create(context, orcidQueue, ex.getMessage(), operation, 500, null);
    }

    private OrcidHistory createHistoryRecordFromOrcidError(Context context, OrcidQueue orcidQueue,
        OrcidOperation operation, OrcidClientException ex) throws SQLException {
        return create(context, orcidQueue, ex.getMessage(), operation, ex.getStatus(), null);
    }

    private OrcidHistory createHistoryRecordFromOrcidResponse(Context context, OrcidQueue orcidQueue,
        OrcidOperation operation, OrcidResponse orcidResponse) throws SQLException {

        int status = orcidResponse.getStatus();
        if (operation == OrcidOperation.DELETE && orcidResponse.isNotFoundStatus()) {
            status = HttpStatus.SC_NO_CONTENT;
        }

        return create(context, orcidQueue, orcidResponse.getContent(), operation, status, orcidResponse.getPutCode());
    }

    private OrcidHistory create(Context context, OrcidQueue orcidQueue, String responseMessage,
        OrcidOperation operation, int status, String putCode) throws SQLException {
        OrcidHistory history = new OrcidHistory();
        history.setEntity(orcidQueue.getEntity());
        history.setProfileItem(orcidQueue.getProfileItem());
        history.setResponseMessage(responseMessage);
        history.setStatus(status);
        history.setPutCode(putCode);
        history.setRecordType(orcidQueue.getRecordType());
        history.setMetadata(orcidQueue.getMetadata());
        history.setOperation(operation);
        history.setDescription(orcidQueue.getDescription());
        return orcidHistoryDAO.create(context, history);
    }

    private Optional<String> getMetadataValue(Item item, String metadataField) {
        return ofNullable(itemService.getMetadataFirstValue(item, new MetadataFieldName(metadataField), Item.ANY))
            .filter(StringUtils::isNotBlank);
    }

    private Optional<String> getAccessToken(Context context, Item item) {
        return ofNullable(orcidTokenService.findByProfileItem(context, item))
            .map(orcidToken -> orcidToken.getAccessToken());
    }

    private boolean isProfileSectionType(OrcidQueue orcidQueue) {
        return OrcidProfileSectionType.isValid(orcidQueue.getRecordType());
    }

    private boolean isEntityType(OrcidQueue orcidQueue) {
        return OrcidEntityType.isValidEntityType(orcidQueue.getRecordType());
    }

    private Optional<String> findLastPutCode(List<OrcidHistory> orcidHistoryRecords, Item profileItem) {
        return orcidHistoryRecords.stream()
            .filter(orcidHistoryRecord -> profileItem.equals(orcidHistoryRecord.getProfileItem()))
            .sorted(comparing(OrcidHistory::getTimestamp, nullsFirst(naturalOrder())).reversed())
            .map(history -> history.getPutCode())
            .filter(putCode -> isNotBlank(putCode))
            .findFirst();
    }

    private Long getPutCode(OrcidQueue orcidQueue) {
        return isCreatable(orcidQueue.getPutCode()) ? Long.valueOf(orcidQueue.getPutCode()) : null;
    }

    public OrcidClient getOrcidClient() {
        return orcidClient;
    }

    public void setOrcidClient(OrcidClient orcidClient) {
        this.orcidClient = orcidClient;
    }

}
