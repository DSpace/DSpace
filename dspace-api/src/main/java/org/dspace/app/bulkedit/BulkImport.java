/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.dspace.app.bulkimport.utils.WorkbookUtils.getCellValue;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.app.bulkimport.exception.BulkImportException;
import org.dspace.app.bulkimport.model.ImportAction;
import org.dspace.app.bulkimport.model.MainEntity;
import org.dspace.app.bulkimport.model.MetadataGroup;
import org.dspace.app.bulkimport.service.ItemSearcherMapper;
import org.dspace.app.bulkimport.utils.WorkbookUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkImport extends DSpaceRunnable<BulkImportScriptConfiguration<BulkImport>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkImport.class);


    private static final int ID_CELL_INDEX = 0;

    private static final int ACTION_CELL_INDEX = 1;

    private static final String UUID_PREFIX = "UUID";

    private static final String ID_SEPARATOR = "::";

    private static final String METADATA_VALUES_SEPARATOR = "\\|\\|";

    private static final String LANGUAGE_SEPARATOR = "/";

    private static final String ROW_ID = "ROW-ID";


    private CollectionService collectionService;

    private ItemService itemService;

    private MetadataFieldService metadataFieldService;

    private WorkspaceItemService workspaceItemService;

    private InstallItemService installItemService;

    private WorkflowService<XmlWorkflowItem> workflowService;

    private ItemSearcherMapper itemSearcherMapper;

    private AuthorizeService authorizeService;


    private String collectionId;

    private String filename;

    private Collection collection;

    private boolean submissionEnabled;

    private boolean endOnError;

    private Workbook workbook;


    @Override
    @SuppressWarnings("unchecked")
    public void setup() throws ParseException {

        this.collectionService = ContentServiceFactory.getInstance().getCollectionService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        this.workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        this.installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        this.workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
        this.authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        this.itemSearcherMapper = new DSpace().getServiceManager().getServiceByName("itemSearcherMapper",
            ItemSearcherMapper.class);

        collectionId = commandLine.getOptionValue('c');
        filename = commandLine.getOptionValue('f');
    }

    @Override
    public void internalRun() throws Exception {
        Context context = new Context();
        assignCurrentUserInContext(context);

        InputStream inputStream = handler.getFileStream(context, filename)
            .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                + "found for filename: " + filename));

        collection = collectionService.find(context, UUID.fromString(collectionId));
        if (collection == null) {
            throw new IllegalArgumentException("No collection found with id " + collectionId);
        }

        if (!this.authorizeService.isAdmin(context, collection)) {
            throw new IllegalArgumentException("The user is not an admin of the given collection");
        }

        try {
            performImport(context, inputStream);
            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }
    }

    public void performImport(Context context, InputStream is) {
        this.workbook = createWorkbook(is);
        validateWorkbook(context);
        List<MainEntity> mainEntities = readMainEntities();
        performImport(context, mainEntities);
    }

    private Workbook createWorkbook(InputStream is) {
        try {
            return WorkbookFactory.create(is);
        } catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
            throw new BulkImportException("An error occurs during the workbook creation", e);
        }
    }

    private void validateWorkbook(Context context) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new BulkImportException("The Workbook should have at least one sheet");
        }

        for (Sheet sheet : workbook) {
            String name = sheet.getSheetName();
            if (WorkbookUtils.isSheetEmpty(sheet)) {
                throw new BulkImportException("The sheet " + name + " of the Workbook is empty");
            }
            if (WorkbookUtils.isRowEmpty(sheet.getRow(0))) {
                throw new BulkImportException("The header of sheet " + name + " of the Workbook is empty");
            }

            validateHeaders(context, sheet);
        }
    }

    private void validateHeaders(Context context, Sheet sheet) {

        List<String> headers = getAllHeaders(sheet);
        String sheetName = sheet.getSheetName();
        boolean isMainEntitySheet = workbook.getSheetIndex(sheet) == 0;

        if (isMainEntitySheet && headers.size() < 2) {
            throw new BulkImportException("At least the columns ID and ACTION are required for the Main Entity sheet");
        }

        List<String> metadataFields = headers.subList(getFirstMetadataIndex(sheet), headers.size());
        List<String> invalidMetadataFields = new ArrayList<>();

        for (String metadata : metadataFields) {

            String metadataWithoutLanguage = getMetadataField(metadata);
            try {
                if (metadataFieldService.findByString(context, metadataWithoutLanguage, '.') == null) {
                    invalidMetadataFields.add(metadataWithoutLanguage);
                }
            } catch (SQLException e) {
                handler.logError(ExceptionUtils.getRootCauseMessage(e));
                invalidMetadataFields.add(metadataWithoutLanguage);
            }

        }

        if (CollectionUtils.isNotEmpty(invalidMetadataFields)) {
            throw new BulkImportException("The following metadata fields of the sheet named " + sheetName
                + " are invalid:" + invalidMetadataFields);
        }

    }

    private List<MainEntity> readMainEntities() {
        Sheet mainEntitySheet = workbook.getSheetAt(0);
        Map<String, Integer> headers = getHeaderMap(mainEntitySheet);

        List<Sheet> metadataGroupSheets = getAllMetadataGroupSheets();
        List<MetadataGroup> metadataGroups = readMetadataGroups(metadataGroupSheets);

        return WorkbookUtils.getRows(mainEntitySheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .filter(this::isMainEntityRowValid)
            .map(row -> buildMainEntity(row, headers, metadataGroups))
            .collect(Collectors.toList());
    }

    private List<Sheet> getAllMetadataGroupSheets() {
        return StreamSupport.stream(workbook.spliterator(), false).skip(1)
            .collect(Collectors.toList());
    }

    private List<MetadataGroup> readMetadataGroups(List<Sheet> metadataGroupSheets) {
        return metadataGroupSheets.stream()
            .flatMap(this::readMetadataGroups)
            .collect(Collectors.toList());
    }

    private Stream<MetadataGroup> readMetadataGroups(Sheet metadataGroupSheet) {
        Map<String, Integer> headers = getHeaderMap(metadataGroupSheet);
        return WorkbookUtils.getRows(metadataGroupSheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .filter(this::isMetadataGroupRowValid)
            .map(row -> buildMetadataGroup(row, headers));
    }

    private List<String> getAllHeaders(Sheet sheet) {
        return WorkbookUtils.getCells(sheet.getRow(0))
            .map(cell -> getCellValue(cell))
            .filter(header -> StringUtils.isNotBlank(header))
            .collect(Collectors.toList());
    }

    private Map<String, Integer> getHeaderMap(Sheet sheet) {
        return WorkbookUtils.getCells(sheet.getRow(0))
            .filter(cell -> StringUtils.isNotBlank(getCellValue(cell)))
            .collect(Collectors.toMap(cell -> getCellValue(cell), cell -> cell.getColumnIndex(), handleDuplication()));
    }

    private BinaryOperator<Integer> handleDuplication() {
        return (i1, i2) -> {
            throw new BulkImportException("Duplicated headers found on cells " + (i1 + 1) + " and " + (i2 + 1));
        };
    }

    private MetadataGroup buildMetadataGroup(Row row, Map<String, Integer> headers) {
        String parentId = getIdFromRow(row);
        MultiValuedMap<String, String> metadata = getMetadataFromRow(row, headers);
        return new MetadataGroup(parentId, row.getSheet().getSheetName(), metadata);
    }

    private MainEntity buildMainEntity(Row row, Map<String, Integer> headers, List<MetadataGroup> metadataGroups) {
        String id = getIdFromRow(row);
        String action = getActionFromRow(row);
        MultiValuedMap<String, String> metadata = getMetadataFromRow(row, headers);
        List<MetadataGroup> ownMetadataGroup = getOwnMetadataGroups(row, metadataGroups);
        return new MainEntity(id, action, row.getRowNum(), metadata, ownMetadataGroup);
    }

    private void performImport(Context context, List<MainEntity> mainEntities) {
        mainEntities.forEach(mainEntity -> performImport(context, mainEntity));
    }

    private void performImport(Context context, MainEntity mainEntity) {

        try {

            switch (mainEntity.getAction()) {
                case ADD:
                    addItem(context, mainEntity);
                    break;
                case UPDATE:
                    updateItem(context, mainEntity);
                    break;
                case DELETE:
                    deleteItem(context, mainEntity);
                    break;
                case NOT_SPECIFIED:
                default:
                    addOrUpdateItem(context, mainEntity);
                    break;
            }

        } catch (BulkImportException bie) {
            handleException(mainEntity, bie);
        } catch (Exception e) {
            LOGGER.error("An error occurs during the import", e);
            handleException(mainEntity, new BulkImportException(e));
        }

    }

    private void addItem(Context context, MainEntity mainEntity)
        throws AuthorizeException, SQLException, IOException, WorkflowException {

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);

        if (submissionEnabled) {
            installItemService.installItem(context, workspaceItem);
        } else {
            workflowService.start(context, workspaceItem);
        }

        Item item = workspaceItem.getItem();
        addMetadata(context, item, mainEntity, false);

        handler.logInfo("Row " + mainEntity.getRow() + " - Item created successfully - ID: " + item.getID());

    }

    private void updateItem(Context context, MainEntity mainEntity) throws Exception {
        Item item = findItem(context, mainEntity);
        if (item == null) {
            throw new BulkImportException("No item to update found for entity with id " + mainEntity.getId());
        }

        updateItem(context, mainEntity, item);
    }

    private void updateItem(Context context, MainEntity mainEntity, Item item) throws SQLException {

        if (!collection.equals(item.getOwningCollection())) {
            throw new BulkImportException("The item related to the entity with id " + mainEntity.getId()
                + " have a different own collection");
        }

        addMetadata(context, item, mainEntity, true);

        handler.logInfo("Row " + mainEntity.getRow() + " - Item updated successfully - ID: " + item.getID());

    }

    private void deleteItem(Context context, MainEntity mainEntity) throws Exception {

        Item item = findItem(context, mainEntity);
        if (item == null) {
            throw new BulkImportException("No item to delete found for entity with id " + mainEntity.getId());
        }

        itemService.delete(context, item);
        handler.logInfo("Row " + mainEntity.getRow() + " - Item deleted successfully");
    }

    private void addOrUpdateItem(Context context, MainEntity mainEntity) throws Exception {

        Item item = findItem(context, mainEntity);
        if (item == null) {
            addItem(context, mainEntity);
        } else {
            updateItem(context, mainEntity, item);
        }

    }

    private Item findItem(Context context, MainEntity mainEntity) throws Exception {

        String id = mainEntity.getId();
        if (id == null) {
            return null;
        }

        if (UUIDUtils.fromString(id) != null) {
            return itemSearcherMapper.search(context, UUID_PREFIX, id);
        }

        String[] idSections = mainEntity.getId().split(ID_SEPARATOR);
        return itemSearcherMapper.search(context, idSections[0].toUpperCase(), idSections[1]);
    }

    private void addMetadata(Context context, Item item, MainEntity mainEntity, boolean replace) throws SQLException {

        if (replace) {
            removeMetadata(context, item, mainEntity);
        }

        addMetadata(context, item, mainEntity.getMetadata(), false);

        List<MetadataGroup> metadataGroups = mainEntity.getMetadataGroups();
        for (MetadataGroup metadataGroup : metadataGroups) {
            addMetadata(context, item, metadataGroup.getMetadata(), true);
        }

    }

    private void addMetadata(Context context, Item item, MultiValuedMap<String, String> metadata,
        boolean isMetadataGroup) throws SQLException {

        Iterable<String> metadataFields = metadata.keySet();
        for (String field : metadataFields) {
            String language = getMetadataLanguage(field);
            MetadataField metadataField = metadataFieldService.findByString(context, getMetadataField(field), '.');
            for (String value : metadata.get(field)) {
                value = isMetadataGroup && StringUtils.isBlank(value) ? PLACEHOLDER_PARENT_METADATA_VALUE : value;
                itemService.addMetadata(context, item, metadataField, language, value, null, -1);
            }
        }

    }

    private void removeMetadata(Context context, Item item, MainEntity mainEntity) throws SQLException {

        removeMetadata(context, item, mainEntity.getMetadata());

        List<MetadataGroup> metadataGroups = mainEntity.getMetadataGroups();
        for (MetadataGroup metadataGroup : metadataGroups) {
            removeMetadata(context, item, metadataGroup.getMetadata());
        }
    }

    private void removeMetadata(Context context, Item item, MultiValuedMap<String, String> metadata)
        throws SQLException {

        Iterable<String> fields = metadata.keySet();
        for (String field : fields) {
            String language = getMetadataLanguage(field);
            MetadataField metadataField = metadataFieldService.findByString(context, getMetadataField(field), '.');
            removeSingleMetadata(context, item, metadataField, language);
        }

    }

    private void removeSingleMetadata(Context context, Item item, MetadataField metadataField, String language)
        throws SQLException {
        List<MetadataValue> metadata = itemService.getMetadata(item, metadataField.getMetadataSchema().getName(),
            metadataField.getElement(), metadataField.getQualifier(), language);
        itemService.removeMetadataValues(context, item, metadata);
    }

    private String getMetadataField(String field) {
        return field.split(LANGUAGE_SEPARATOR)[0];
    }

    private String getMetadataLanguage(String field) {
        return field.contains(LANGUAGE_SEPARATOR) ? field.split(LANGUAGE_SEPARATOR)[1] : null;
    }

    private String getIdFromRow(Row row) {
        return WorkbookUtils.getCellValue(row, ID_CELL_INDEX);
    }

    private String getActionFromRow(Row row) {
        return WorkbookUtils.getCellValue(row, ACTION_CELL_INDEX);
    }

    private MultiValuedMap<String, String> getMetadataFromRow(Row row, Map<String, Integer> headers) {

        MultiValuedMap<String, String> metadata = new ArrayListValuedHashMap<String, String>();

        int firstMetadataIndex = getFirstMetadataIndex(row.getSheet());

        for (String header : headers.keySet()) {
            int index = headers.get(header);
            if (index >= firstMetadataIndex) {
                String cellValue = WorkbookUtils.getCellValue(row, index);
                String[] values = cellValue != null ? cellValue.split(METADATA_VALUES_SEPARATOR) : new String[] { "" };
                metadata.putAll(header, Arrays.asList(values));
            }
        }

        return metadata;
    }

    private int getFirstMetadataIndex(Sheet sheet) {
        boolean isMainEntitySheet = workbook.getSheetIndex(sheet) == 0;
        return isMainEntitySheet ? 2 : 1;
    }

    private List<MetadataGroup> getOwnMetadataGroups(Row row, List<MetadataGroup> metadataGroups) {
        String id = getIdFromRow(row);
        int rowIndex = row.getRowNum() + 1;
        return metadataGroups.stream()
            .filter(g -> g.getParentId().equals(id) || g.getParentId().equals(ROW_ID + ID_SEPARATOR + rowIndex))
            .collect(Collectors.toList());
    }

    private boolean isMainEntityRowValid(Row row) {
        String id = getIdFromRow(row);
        String action = getActionFromRow(row);

        if (!isValidId(id, false)) {
            handleError(row, "Invalid ID " + id);
            return false;
        }

        return isNotBlank(action) ? isValidAction(id, action, row) : true;
    }

    private boolean isValidAction(String id, String action, Row row) {

        ImportAction[] actions = ImportAction.values();
        if (!ImportAction.isValid(action)) {
            handleError(row, "Invalid action " + action + ": allowed values are " + Arrays.toString(actions));
            return false;
        }

        if (isBlank(id) && ImportAction.valueOf(action) != ImportAction.ADD) {
            handleError(row, "Only ADD action can have an empty ID");
            return false;
        }

        if (isNotBlank(id) && ImportAction.valueOf(action) == ImportAction.ADD) {
            handleError(row, "ADD action can not have an ID set");
            return false;
        }

        return true;
    }

    private boolean isMetadataGroupRowValid(Row row) {
        String parentId = getIdFromRow(row);

        if (StringUtils.isBlank(parentId)) {
            handleError(row, "Nested Entities - No PARENT-ID set");
            return false;
        }

        if (!isValidId(parentId, true)) {
            handleError(row, "Nested Entities - Invalid PARENT-ID " + parentId);
            return false;
        }

        return true;
    }

    private boolean isValidId(String id, boolean isMetadataGroup) {

        if (StringUtils.isBlank(id)) {
            return true;
        }

        if (UUIDUtils.fromString(id) != null) {
            return true;
        }

        String[] idSections = id.split(ID_SEPARATOR);
        if (idSections.length != 2) {
            return false;
        }

        java.util.Collection<String> validPrefixes = itemSearcherMapper.getAllowedSearchType();
        if (isMetadataGroup) {
            validPrefixes = new ArrayList<String>(validPrefixes);
            validPrefixes.add(ROW_ID);
        }
        return validPrefixes.contains(idSections[0]);
    }

    private void handleException(MainEntity mainEntity, BulkImportException bie) {
        if (endOnError) {
            throw bie;
        }
        String message = "Row " + mainEntity.getRow() + " - " + getRootCauseMessage(bie);
        handler.logError(message);
    }

    private void handleError(Row row, String message) {
        String errorMessage = "Row " + (row.getRowNum() + 1) + " - " + message;
        if (endOnError) {
            throw new BulkImportException(errorMessage);
        }
        handler.logError(errorMessage);
    }

    private void assignCurrentUserInContext(Context context) throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public BulkImportScriptConfiguration<BulkImport> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("bulk-import", BulkImportScriptConfiguration.class);
    }

}
