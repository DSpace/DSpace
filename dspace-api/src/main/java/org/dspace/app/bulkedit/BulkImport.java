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

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.app.bulkimport.exception.BulkImportException;
import org.dspace.app.bulkimport.model.ImportAction;
import org.dspace.app.bulkimport.model.MainEntity;
import org.dspace.app.bulkimport.model.NestedEntity;
import org.dspace.app.bulkimport.service.ItemSearcherMapper;
import org.dspace.app.bulkimport.utils.WorkbookUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
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

    private WorkspaceItemService workspaceItemService;

    private InstallItemService installItemService;

    private WorkflowService<XmlWorkflowItem> workflowService;

    private ItemSearcherMapper itemSearcherMapper;

    private String collectionId;

    private String filename;

    private Collection collection;

    private boolean submissionEnabled;

    private boolean endOnError;


    @Override
    @SuppressWarnings("unchecked")
    public void setup() throws ParseException {

        this.collectionService = ContentServiceFactory.getInstance().getCollectionService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        this.installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        this.workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
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

        try {
            performImport(context, inputStream);
            context.complete();
        } catch (Exception e) {
            e.printStackTrace(); // TODO REMOVE
            handler.handleException(e);
            context.abort();
        }
    }

    public void performImport(Context context, InputStream is) {
        Workbook workbook = createWorkbook(is);
        validateWorkbook(workbook);
        List<MainEntity> mainEntities = readMainEntities(workbook);
        performImport(context, mainEntities);
    }

    private Workbook createWorkbook(InputStream is) {
        try {
            return WorkbookFactory.create(is);
        } catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
            throw new BulkImportException("An error occurs during the workbook creation", e);
        }
    }

    private void validateWorkbook(Workbook workbook) {
        if (workbook.getNumberOfSheets() != 2) {
            throw new BulkImportException("The input Workbook must have 2 sheets (main entity and nested entity)");
        }

        for (Sheet sheet : workbook) {
            String name = sheet.getSheetName();
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new BulkImportException("The sheet " + name + " of the Workbook is empty");
            }
            if (WorkbookUtils.isRowEmpty(sheet.getRow(0))) {
                throw new BulkImportException("The header of sheet " + name + " of the Workbook is empty");
            }
        }
    }

    private List<MainEntity> readMainEntities(Workbook workbook) {
        Sheet mainEntitySheet = workbook.getSheetAt(0);
        List<String> headers = getHeaders(mainEntitySheet);

        Sheet nestedEntitySheet = workbook.getSheetAt(1);
        List<NestedEntity> nestedEntities = readNestedEntities(nestedEntitySheet);

        return WorkbookUtils.getRows(mainEntitySheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .filter(this::isMainEntityRowValid)
            .map(row -> buildMainEntity(row, headers, nestedEntities))
            .collect(Collectors.toList());
    }

    private List<NestedEntity> readNestedEntities(Sheet nestedEntitySheet) {
        List<String> headers = getHeaders(nestedEntitySheet);
        return WorkbookUtils.getRows(nestedEntitySheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .filter(this::isNestedEntityRowValid)
            .map(row -> buildNestedEntity(row, headers))
            .collect(Collectors.toList());
    }

    private List<String> getHeaders(Sheet sheet) {
        return WorkbookUtils.getCells(sheet.getRow(0)).map(Cell::getStringCellValue).collect(Collectors.toList());
    }

    private NestedEntity buildNestedEntity(Row row, List<String> headers) {
        String parentId = getIdFromRow(row);
        MultiValuedMap<String, String> metadata = getMetadataFromRow(row, headers);
        return new NestedEntity(parentId, metadata);
    }

    private MainEntity buildMainEntity(Row row, List<String> headers, List<NestedEntity> nestedEntities) {
        String id = getIdFromRow(row);
        String action = getActionFromRow(row);
        MultiValuedMap<String, String> metadata = getMetadataFromRow(row, headers);
        List<NestedEntity> ownNestedEntity = getOwnNestedEntities(row, nestedEntities);
        return new MainEntity(id, action, row.getRowNum(), metadata, ownNestedEntity);
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

        handler.logInfo("Row " + mainEntity.getRow() + " - Item created successfully");

    }

    private void updateItem(Context context, MainEntity mainEntity) throws Exception {
        Item item = findItem(context, mainEntity);
        if (item == null) {
            throw new BulkImportException("No item to update found for entity with id " + mainEntity.getId());
        }

        updateItem(context, mainEntity, item);
    }

    private void updateItem(Context context, MainEntity mainEntity, Item item) {

        if (!collection.equals(item.getOwningCollection())) {
            throw new BulkImportException("The item related to the entity with id " + mainEntity.getId()
                + " have a different own collection");
        }

        handler.logInfo("Row " + mainEntity.getRow() + " - Item update successfully");

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

    private String getIdFromRow(Row row) {
        return WorkbookUtils.getCellValue(row, ID_CELL_INDEX);
    }

    private String getActionFromRow(Row row) {
        return WorkbookUtils.getCellValue(row, ACTION_CELL_INDEX);
    }

    private MultiValuedMap<String, String> getMetadataFromRow(Row row, List<String> headers) {
        MultiValuedMap<String, String> metadata = new ArrayListValuedHashMap<String, String>();
        int index = 0;
        for (String header : headers) {
            if (index != ID_CELL_INDEX && index != ACTION_CELL_INDEX) {
                String cellValue = WorkbookUtils.getCellValue(row, index);
                String[] values = cellValue != null ? cellValue.split(METADATA_VALUES_SEPARATOR) : new String[] {};
                metadata.putAll(header, Arrays.asList(values));
            }
            index++;
        }
        return metadata;
    }

    private List<NestedEntity> getOwnNestedEntities(Row row, List<NestedEntity> nestedEntities) {
        String id = getIdFromRow(row);
        int rowIndex = row.getRowNum() + 1;
        return nestedEntities.stream()
            .filter(nested -> nested.getParentId().equals(id)
                || nested.getParentId().equals(ROW_ID + ID_SEPARATOR + rowIndex))
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

    private boolean isNestedEntityRowValid(Row row) {
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

    private boolean isValidId(String id, boolean isNestedEntity) {

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
        if (isNestedEntity) {
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
