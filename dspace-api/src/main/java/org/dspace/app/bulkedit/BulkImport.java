/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static java.util.stream.Collectors.toMap;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.app.bulkimport.exception.BulkImportException;
import org.dspace.app.bulkimport.model.EntityRow;
import org.dspace.app.bulkimport.model.ImportAction;
import org.dspace.app.bulkimport.model.MetadataGroup;
import org.dspace.app.bulkimport.model.MetadataValueVO;
import org.dspace.app.bulkimport.service.ItemSearcherMapper;
import org.dspace.app.bulkimport.utils.WorkbookUtils;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
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

/**
 * Implementation of {@link DSpaceRunnable} to perfom a bulk import via excel
 * file.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class BulkImport extends DSpaceRunnable<BulkImportScriptConfiguration<BulkImport>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkImport.class);


    private static final int ID_CELL_INDEX = 0;

    private static final int ACTION_CELL_INDEX = 1;

    private static final String ID_CELL = "ID";

    private static final String ACTION_CELL = "ACTION";

    private static final String PARENT_ID_CELL = "PARENT-ID";

    private static final String UUID_PREFIX = "UUID";

    private static final String ID_SEPARATOR = "::";

    private static final String AUTHORITY_SEPARATOR = "::";

    private static final String METADATA_SEPARATOR = "\\|\\|";

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

    private DCInputsReader reader;

    private SubmissionConfigReader submissionConfigReader;


    private String collectionId;

    private String filename;

    private boolean useWorkflow;

    private boolean abortOnError;

    private Context context;


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

        try {
            this.reader = new DCInputsReader();
            this.submissionConfigReader = new SubmissionConfigReader();
        } catch (DCInputsReaderException | SubmissionConfigReaderException e) {
            throw new RuntimeException(e);
        }

        collectionId = commandLine.getOptionValue('c');
        filename = commandLine.getOptionValue('f');

        if (commandLine.hasOption('w')) {
            useWorkflow = true;
        }

        if (commandLine.hasOption('e')) {
            abortOnError = true;
        }
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        context.setMode(Context.Mode.BATCH_EDIT);
        assignCurrentUserInContext();

        InputStream inputStream = handler.getFileStream(context, filename)
            .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                + "found for filename: " + filename));

        Collection collection = getCollection();
        if (collection == null) {
            throw new IllegalArgumentException("No collection found with id " + collectionId);
        }

        if (!this.authorizeService.isAdmin(context, collection)) {
            throw new IllegalArgumentException("The user is not an admin of the given collection");
        }

        try {
            performImport(inputStream);
            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }
    }

    public void performImport(InputStream is) {
        Workbook workbook = createWorkbook(is);
        validateWorkbook(workbook);
        List<EntityRow> entityRows = getValidEntityRows(workbook);
        performImport(entityRows);
    }

    private Workbook createWorkbook(InputStream is) {
        try {
            return WorkbookFactory.create(is);
        } catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
            throw new BulkImportException("An error occurs during the workbook creation", e);
        }
    }

    private void validateWorkbook(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new BulkImportException("The Workbook should have at least one sheet");
        }

        List<String> groups = getSubmissionFormMetadata(true);

        for (Sheet sheet : workbook) {
            String name = sheet.getSheetName();

            if (WorkbookUtils.isSheetEmpty(sheet)) {
                throw new BulkImportException("The sheet " + name + " of the Workbook is empty");
            }

            if (WorkbookUtils.isRowEmpty(sheet.getRow(0))) {
                throw new BulkImportException("The header of sheet " + name + " of the Workbook is empty");
            }

            if (!isEntityRowSheet(sheet) && !groups.contains(name)) {
                throw new BulkImportException("The sheet name " + name + " is not a valid metadata group");
            }

            validateHeaders(sheet);
        }
    }

    private void validateHeaders(Sheet sheet) {
        List<String> headers = getAllHeaders(sheet);
        validateMainHeaders(sheet, headers);
        validateMetadataFields(sheet, headers);
    }

    private void validateMainHeaders(Sheet sheet, List<String> headers) {
        String sheetName = sheet.getSheetName();
        boolean isEntityRowSheet = isEntityRowSheet(sheet);

        if (isEntityRowSheet && headers.size() < 2) {
            throw new BulkImportException("At least the columns ID and ACTION are required for the entity sheet");
        }

        if (isEntityRowSheet) {
            String id = headers.get(ID_CELL_INDEX);
            if (!ID_CELL.equals(id)) {
                throw new BulkImportException("Wrong " + ID_CELL + " header on sheet " + sheetName + ": " + id);
            }
            String action = headers.get(ACTION_CELL_INDEX);
            if (!ACTION_CELL.equals(action)) {
                throw new BulkImportException("Wrong " + ACTION_CELL + " header on sheet " + sheetName + ": " + action);
            }
        } else {
            String id = headers.get(ID_CELL_INDEX);
            if (!PARENT_ID_CELL.equals(id)) {
                throw new BulkImportException("Wrong " + PARENT_ID_CELL + " header on sheet " + sheetName + ": " + id);
            }
        }
    }

    private void validateMetadataFields(Sheet sheet, List<String> headers) {
        String sheetName = sheet.getSheetName();
        boolean isEntityRowSheet = isEntityRowSheet(sheet);

        List<String> metadataFields = headers.subList(getFirstMetadataIndex(sheet), headers.size());
        List<String> invalidMetadataMessages = new ArrayList<>();

        List<String> submissionMetadata = isEntityRowSheet ? getSubmissionFormMetadata(false)
            : getSubmissionFormMetadataGroup(sheetName);

        for (String metadataField : metadataFields) {

            String metadata = getMetadataField(metadataField);

            if (StringUtils.isBlank(metadata)) {
                invalidMetadataMessages.add("Empty metadata");
                continue;
            }

            if (!submissionMetadata.contains(metadata)) {
                invalidMetadataMessages.add(metadata + " is not valid for the given collection");
                continue;
            }

            try {
                if (metadataFieldService.findByString(context, metadata, '.') == null) {
                    invalidMetadataMessages.add(metadata + " not found");
                }
            } catch (SQLException e) {
                handler.logError(ExceptionUtils.getRootCauseMessage(e));
                invalidMetadataMessages.add(metadata);
            }

        }

        if (CollectionUtils.isNotEmpty(invalidMetadataMessages)) {
            throw new BulkImportException("The following metadata fields of the sheet named '" + sheetName
                + "' are invalid:" + invalidMetadataMessages);
        }
    }

    private List<String> getSubmissionFormMetadataGroup(String groupName) {
        try {
            String submissionName = this.submissionConfigReader.getSubmissionConfigByCollection(getCollection())
                .getSubmissionName();
            String formName = submissionName + "-" + groupName.replaceAll("\\.", "-");
            return Arrays.stream(this.reader.getInputsByFormName(formName).getFields())
                .flatMap(dcInputs -> Arrays.stream(dcInputs))
                .map(dcInput -> dcInput.getFieldName())
                .collect(Collectors.toList());
        } catch (DCInputsReaderException e) {
            throw new BulkImportException("An error occurs reading the input configuration "
                + "by group name " + groupName, e);
        }
    }

    private List<String> getSubmissionFormMetadata(boolean group) {

        try {
            return this.reader.getInputsByCollection(getCollection()).stream()
                .flatMap(dcInputSet -> Arrays.stream(dcInputSet.getFields()))
                .flatMap(dcInputs -> Arrays.stream(dcInputs))
                .filter(dcInput -> group ? isGroupType(dcInput) : !isGroupType(dcInput))
                .map(dcInput -> dcInput.getFieldName())
                .collect(Collectors.toList());
        } catch (DCInputsReaderException e) {
            throw new BulkImportException("An error occurs reading the input configuration by collection", e);
        }

    }

    private boolean isGroupType(DCInput dcInput) {
        return "group".equals(dcInput.getInputType());
    }

    private List<EntityRow> getValidEntityRows(Workbook workbook) {
        Sheet entityRowSheet = workbook.getSheetAt(0);
        Map<String, Integer> headers = getHeaderMap(entityRowSheet);

        List<Sheet> metadataGroupSheets = getAllMetadataGroupSheets(workbook);

        handler.logInfo("Start reading all the metadata group rows");
        List<MetadataGroup> metadataGroups = getValidMetadataGroups(metadataGroupSheets);
        handler.logInfo("Found " + metadataGroups.size() + " metadata groups to process");

        return WorkbookUtils.getRows(entityRowSheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .filter(this::isEntityRowRowValid)
            .map(row -> buildEntityRow(row, headers, metadataGroups))
            .collect(Collectors.toList());
    }

    private List<Sheet> getAllMetadataGroupSheets(Workbook workbook) {
        return StreamSupport.stream(workbook.spliterator(), false).skip(1)
            .collect(Collectors.toList());
    }

    /**
     * Read all the metadata groups from all the given sheets.
     *
     * @param metadataGroupSheets the metadata group sheets to read
     * @return a list of MetadataGroup
     */
    private List<MetadataGroup> getValidMetadataGroups(List<Sheet> metadataGroupSheets) {
        return metadataGroupSheets.stream()
            .flatMap(this::getValidMetadataGroups)
            .collect(Collectors.toList());
    }

    /**
     * Read all the metadata groups from a single sheet.
     *
     * @param metadataGroupSheet the metadata group sheet
     * @return a stream of MetadataGroup
     */
    private Stream<MetadataGroup> getValidMetadataGroups(Sheet metadataGroupSheet) {
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
            .collect(Collectors.toList());
    }

    private Map<String, Integer> getHeaderMap(Sheet sheet) {
        return WorkbookUtils.getCells(sheet.getRow(0))
            .filter(cell -> StringUtils.isNotBlank(getCellValue(cell)))
            .collect(toMap(cell -> getCellValue(cell), cell -> cell.getColumnIndex(), handleDuplication(sheet)));
    }

    private BinaryOperator<Integer> handleDuplication(Sheet sheet) {
        return (i1, i2) -> {
            throw new BulkImportException("Sheet " + sheet.getSheetName() + " - Duplicated headers found on cells "
                + (i1 + 1) + " and " + (i2 + 1));
        };
    }

    private MetadataGroup buildMetadataGroup(Row row, Map<String, Integer> headers) {
        String parentId = getIdFromRow(row);
        MultiValuedMap<String, MetadataValueVO> metadata = getMetadataFromRow(row, headers);
        return new MetadataGroup(parentId, row.getSheet().getSheetName(), metadata);
    }

    private EntityRow buildEntityRow(Row row, Map<String, Integer> headers, List<MetadataGroup> metadataGroups) {
        String id = getIdFromRow(row);
        String action = getActionFromRow(row);
        MultiValuedMap<String, MetadataValueVO> metadata = getMetadataFromRow(row, headers);
        List<MetadataGroup> ownMetadataGroup = getOwnMetadataGroups(row, metadataGroups);
        return new EntityRow(id, action, row.getRowNum(), metadata, ownMetadataGroup);
    }

    private void performImport(List<EntityRow> entityRows) {
        handler.logInfo("Found " + entityRows.size() + " items to process");
        entityRows.forEach(entityRow -> performImport(entityRow));
    }

    private void performImport(EntityRow entityRow) {

        try {

            Item item = null;

            switch (entityRow.getAction()) {
                case ADD:
                    item = addItem(entityRow);
                    break;
                case UPDATE:
                    item = updateItem(entityRow);
                    break;
                case DELETE:
                    deleteItem(entityRow);
                    break;
                case NOT_SPECIFIED:
                default:
                    item = addOrUpdateItem(entityRow);
                    break;
            }

            if (item != null) {
                context.uncacheEntity(item);
            }

            context.commit();

        } catch (BulkImportException bie) {
            handleException(entityRow, bie);
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurs during the import", e);
            handleException(entityRow, new BulkImportException(e));
        }

    }

    private Item addItem(EntityRow entityRow)
        throws AuthorizeException, SQLException, IOException, WorkflowException {

        WorkspaceItem workspaceItem = workspaceItemService.create(context, getCollection(), false);

        if (useWorkflow) {
            workflowService.start(context, workspaceItem);
        } else {
            installItemService.installItem(context, workspaceItem);
        }

        Item item = workspaceItem.getItem();
        addMetadata(item, entityRow, false);

        handler.logInfo("Row " + entityRow.getRow() + " - Item created successfully - ID: " + item.getID());
        return item;

    }

    private Item updateItem(EntityRow entityRow) throws Exception {
        Item item = findItem(entityRow);
        if (item == null) {
            throw new BulkImportException("No item to update found for entity with id " + entityRow.getId());
        }

        return updateItem(entityRow, item);
    }

    private Item updateItem(EntityRow entityRow, Item item) throws SQLException {

        if (!getCollection().equals(item.getOwningCollection())) {
            throw new BulkImportException("The item related to the entity with id " + entityRow.getId()
                + " have a different own collection");
        }

        addMetadata(item, entityRow, true);

        handler.logInfo("Row " + entityRow.getRow() + " - Item updated successfully - ID: " + item.getID());
        return item;

    }

    private void deleteItem(EntityRow entityRow) throws Exception {

        Item item = findItem(entityRow);
        if (item == null) {
            throw new BulkImportException("No item to delete found for entity with id " + entityRow.getId());
        }

        itemService.delete(context, item);
        handler.logInfo("Row " + entityRow.getRow() + " - Item deleted successfully");
    }

    private Item addOrUpdateItem(EntityRow entityRow) throws Exception {

        Item item = findItem(entityRow);
        if (item == null) {
            return addItem(entityRow);
        } else {
            return updateItem(entityRow, item);
        }

    }

    private Item findItem(EntityRow entityRow) throws Exception {

        String id = entityRow.getId();
        if (id == null) {
            return null;
        }

        if (UUIDUtils.fromString(id) != null) {
            return itemSearcherMapper.search(context, UUID_PREFIX, id);
        }

        String[] idSections = entityRow.getId().split(ID_SEPARATOR);
        return itemSearcherMapper.search(context, idSections[0].toUpperCase(), idSections[1]);
    }

    private void addMetadata(Item item, EntityRow entityRow, boolean replace) throws SQLException {

        if (replace) {
            removeMetadata(item, entityRow);
        }

        addMetadata(item, entityRow.getMetadata());

        List<MetadataGroup> metadataGroups = entityRow.getMetadataGroups();
        for (MetadataGroup metadataGroup : metadataGroups) {
            addMetadata(item, metadataGroup.getMetadata());
        }

    }

    private void addMetadata(Item item, MultiValuedMap<String, MetadataValueVO> metadata)
        throws SQLException {

        Iterable<String> metadataFields = metadata.keySet();
        for (String field : metadataFields) {
            String language = getMetadataLanguage(field);
            MetadataField metadataField = metadataFieldService.findByString(context, getMetadataField(field), '.');
            for (MetadataValueVO metadataValue : metadata.get(field)) {
                String authority = metadataValue.getAuthority();
                int confidence = metadataValue.getConfidence();
                String value = metadataValue.getValue();
                itemService.addMetadata(context, item, metadataField, language, value, authority, confidence);
            }
        }

    }

    private void removeMetadata(Item item, EntityRow entityRow) throws SQLException {

        removeMetadata(item, entityRow.getMetadata());

        List<MetadataGroup> metadataGroups = entityRow.getMetadataGroups();
        for (MetadataGroup metadataGroup : metadataGroups) {
            removeMetadata(item, metadataGroup.getMetadata());
        }
    }

    private void removeMetadata(Item item, MultiValuedMap<String, MetadataValueVO> metadata)
        throws SQLException {

        Iterable<String> fields = metadata.keySet();
        for (String field : fields) {
            String language = getMetadataLanguage(field);
            MetadataField metadataField = metadataFieldService.findByString(context, getMetadataField(field), '.');
            removeSingleMetadata(item, metadataField, language);
        }

    }

    private void removeSingleMetadata(Item item, MetadataField metadataField, String language)
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

    private MultiValuedMap<String, MetadataValueVO> getMetadataFromRow(Row row, Map<String, Integer> headers) {

        MultiValuedMap<String, MetadataValueVO> metadata = new ArrayListValuedHashMap<String, MetadataValueVO>();

        int firstMetadataIndex = getFirstMetadataIndex(row.getSheet());
        boolean isEntityRowSheet = isEntityRowSheet(row.getSheet());

        for (String header : headers.keySet()) {
            int index = headers.get(header);
            if (index >= firstMetadataIndex) {

                String cellValue = WorkbookUtils.getCellValue(row, index);
                String[] values = isNotBlank(cellValue) ? cellValue.split(METADATA_SEPARATOR) : new String[] { "" };

                List<MetadataValueVO> metadataValues = Arrays.stream(values)
                    .map(value -> buildMetadataValueVO(row, value, isEntityRowSheet))
                    .collect(Collectors.toList());

                metadata.putAll(header, metadataValues);
            }
        }

        return metadata;
    }

    private MetadataValueVO buildMetadataValueVO(Row row, String metadataValue, boolean isEntityRowSheet) {

        if (isBlank(metadataValue)) {
            return new MetadataValueVO(isEntityRowSheet ? metadataValue : PLACEHOLDER_PARENT_METADATA_VALUE, null, -1);
        }

        if (!metadataValue.contains(AUTHORITY_SEPARATOR)) {
            return new MetadataValueVO(metadataValue, null, -1);
        }

        String[] valueSections = metadataValue.split(AUTHORITY_SEPARATOR);

        String value = valueSections[0];
        String authority = valueSections[1];
        int confidence = 600;
        if (valueSections.length > 2) {
            String confidenceAsString = valueSections[2];
            confidence = Integer.valueOf(confidenceAsString);
        }

        return new MetadataValueVO(value, authority, confidence);
    }

    private boolean isEntityRowSheet(Sheet sheet) {
        return sheet.getWorkbook().getSheetIndex(sheet) == 0;
    }

    private int getFirstMetadataIndex(Sheet sheet) {
        return isEntityRowSheet(sheet) ? 2 : 1;
    }

    private List<MetadataGroup> getOwnMetadataGroups(Row row, List<MetadataGroup> metadataGroups) {
        String id = getIdFromRow(row);
        int rowIndex = row.getRowNum() + 1;
        return metadataGroups.stream()
            .filter(g -> g.getParentId().equals(id) || g.getParentId().equals(ROW_ID + ID_SEPARATOR + rowIndex))
            .collect(Collectors.toList());
    }

    private boolean isEntityRowRowValid(Row row) {
        String id = getIdFromRow(row);
        String action = getActionFromRow(row);

        if (!isValidId(id, false)) {
            handleValidationErrorOnRow(row, "Invalid ID " + id);
            return false;
        }

        return isNotBlank(action) ? isValidAction(id, action, row) : true;
    }

    private boolean isValidAction(String id, String action, Row row) {

        ImportAction[] actions = ImportAction.values();
        if (!ImportAction.isValid(action)) {
            handleValidationErrorOnRow(row,
                "Invalid action " + action + ": allowed values are " + Arrays.toString(actions));
            return false;
        }

        if (isBlank(id) && ImportAction.valueOf(action) != ImportAction.ADD) {
            handleValidationErrorOnRow(row, "Only ADD action can have an empty ID");
            return false;
        }

        if (isNotBlank(id) && ImportAction.valueOf(action) == ImportAction.ADD) {
            handleValidationErrorOnRow(row, "ADD action can not have an ID set");
            return false;
        }

        return true;
    }

    private boolean isMetadataGroupRowValid(Row row) {
        String parentId = getIdFromRow(row);

        if (StringUtils.isBlank(parentId)) {
            handleValidationErrorOnRow(row, "No PARENT-ID set");
            return false;
        }

        if (!isValidId(parentId, true)) {
            handleValidationErrorOnRow(row, "Invalid PARENT-ID " + parentId);
            return false;
        }

        int firstMetadataIndex = getFirstMetadataIndex(row.getSheet());
        for (int index = firstMetadataIndex; index < row.getLastCellNum(); index++) {

            String cellValue = WorkbookUtils.getCellValue(row, index);
            String[] values = isNotBlank(cellValue) ? cellValue.split(METADATA_SEPARATOR) : new String[] { "" };
            if (values.length > 1) {
                handleValidationErrorOnRow(row, "Multiple metadata value on the same cell not allowed "
                    + "in the metadata group sheets: " + cellValue);
                return false;
            }

            String value = values[0];
            if (value.contains(AUTHORITY_SEPARATOR)) {

                String[] valueSections = value.split(AUTHORITY_SEPARATOR);
                if (valueSections.length > 3) {
                    handleValidationErrorOnRow(row, "Invalid metadata value " + value + ": too many sections "
                        + "splitted by " + AUTHORITY_SEPARATOR);
                    return false;
                }

                if (valueSections.length > 2 && !NumberUtils.isCreatable(valueSections[2])) {
                    handleValidationErrorOnRow(row,
                        "Invalid metadata value " + value + ": invalid confidence value " + valueSections[2]);
                    return false;
                }

            }
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

    private void handleException(EntityRow entityRow, BulkImportException bie) {
        if (abortOnError) {
            throw bie;
        } else {
            String message = "Row " + entityRow.getRow() + " - " + getRootCauseMessage(bie);
            handler.logError(message);
        }
    }

    private void handleValidationErrorOnRow(Row row, String message) {
        String sheetName = row.getSheet().getSheetName();
        String errorMessage = "Sheet " + sheetName + " - Row " + (row.getRowNum() + 1) + " - " + message;
        if (abortOnError) {
            throw new BulkImportException(errorMessage);
        } else {
            handler.logError(errorMessage);
        }
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private Collection getCollection() {
        try {
            return collectionService.find(context, UUID.fromString(collectionId));
        } catch (SQLException e) {
            throw new BulkImportException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public BulkImportScriptConfiguration<BulkImport> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("bulk-import", BulkImportScriptConfiguration.class);
    }

}
