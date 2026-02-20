/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.BooleanUtils.toBooleanObject;
import static org.apache.commons.lang3.StringUtils.isAllBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.splitByWholeSeparator;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;
import static org.dspace.authorize.ResourcePolicy.TYPE_CUSTOM;
import static org.dspace.authorize.ResourcePolicy.TYPE_INHERITED;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.dspace.util.WorkbookUtils.getCellValue;
import static org.dspace.util.WorkbookUtils.getRows;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.app.bulkimport.exception.BulkImportException;
import org.dspace.app.bulkimport.model.AccessCondition;
import org.dspace.app.bulkimport.model.ChildRow;
import org.dspace.app.bulkimport.model.EntityRow;
import org.dspace.app.bulkimport.model.ImportAction;
import org.dspace.app.bulkimport.model.MetadataGroup;
import org.dspace.app.bulkimport.model.UploadDetails;
import org.dspace.app.bulkimport.util.ImportFileUtil;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authority.service.ItemSearchService;
import org.dspace.authority.service.ItemSearcherMapper;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.dspace.util.MultiFormatDateParser;
import org.dspace.util.UUIDUtils;
import org.dspace.util.WorkbookUtils;
import org.dspace.utils.DSpace;
import org.dspace.validation.service.ValidationService;
import org.dspace.validation.service.factory.ValidationServiceFactory;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItemService;
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


    public static final String BITSTREAMS_SHEET_NAME = "bitstream-metadata";

    public static final String ACCESS_CONDITION_ATTRIBUTES_SEPARATOR = "$$";

    public static final String METADATA_ATTRIBUTES_SEPARATOR = "$$";

    public static final String METADATA_SEPARATOR = "||";

    public static final String LANGUAGE_SEPARATOR_PREFIX = "[";

    public static final String LANGUAGE_SEPARATOR_SUFFIX = "]";

    public static final String ID_SEPARATOR = "::";

    public static final String SECURITY_LEVEL_PREFIX = "sl-";

    public static final String ROW_ID = "ROW-ID";


    public static final String ID_HEADER = "ID";

    public static final String ACTION_HEADER = "ACTION";

    public static final String DISCOVERABLE_HEADER = "DISCOVERABLE";

    public static final String PARENT_ID_HEADER = "PARENT-ID";

    public static final String FILE_PATH_HEADER = "FILE-PATH";

    public static final String BUNDLE_HEADER = "BUNDLE-NAME";

    public static final String BITSTREAM_POSITION_HEADER = "POSITION";

    public static final String ACCESS_CONDITION_HEADER = "ACCESS-CONDITION";

    public static final String ADDITIONAL_ACCESS_CONDITION_HEADER = "ADDITIONAL-ACCESS-CONDITION";

    public static final String[] ENTITY_ROWS_SHEET_HEADERS = { ID_HEADER };

    public static final String[] ENTITY_ROWS_SHEET_OPTIONAL_HEADERS = { ACTION_HEADER, DISCOVERABLE_HEADER };

    public static final String[] METADATA_GROUPS_SHEET_HEADERS = { PARENT_ID_HEADER };

    public static final String[] BITSTREAMS_SHEET_HEADERS = { PARENT_ID_HEADER, FILE_PATH_HEADER, BUNDLE_HEADER,
        BITSTREAM_POSITION_HEADER, ACCESS_CONDITION_HEADER, ADDITIONAL_ACCESS_CONDITION_HEADER };


    private CollectionService collectionService;

    private ConfigurationService configurationService;

    private ItemService itemService;

    private MetadataFieldService metadataFieldService;

    private WorkspaceItemService workspaceItemService;

    private WorkflowItemService<?> workflowItemService;

    private InstallItemService installItemService;

    private WorkflowService<XmlWorkflowItem> workflowService;

    private ItemSearcherMapper itemSearcherMapper;

    private ItemSearchService itemSearchService;

    private AuthorizeService authorizeService;

    private ValidationService validationService;

    private DCInputsReader reader;

    private BulkImportTransformerService bulkImportTransformerService;

    private String collectionId;

    private String filename;

    private boolean abortOnError;

    private Context context;

    private ImportFileUtil importFileUtil;

    private BundleService bundleService;

    private BitstreamService bitstreamService;

    private BitstreamFormatService bitstreamFormatService;

    private UploadConfigurationService uploadConfigurationService;

    private ResourcePolicyService resourcePolicyService;

    private Map<String, AccessConditionOption> uploadAccessConditions;

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
        this.itemSearcherMapper = new DSpace().getSingletonService(ItemSearcherMapper.class);
        this.itemSearchService = new DSpace().getSingletonService(ItemSearchService.class);
        this.validationService = ValidationServiceFactory.getInstance().getValidationService();
        this.workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
        this.bulkImportTransformerService = new DSpace().getServiceManager().getServiceByName(
               BulkImportTransformerService.class.getName(), BulkImportTransformerService.class);
        if (this.importFileUtil == null) {
            this.importFileUtil = new ImportFileUtil(this.handler);
        }
        this.bundleService = ContentServiceFactory.getInstance().getBundleService();
        this.bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        this.bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
        this.uploadConfigurationService = AuthorizeServiceFactory.getInstance().getUploadConfigurationService();
        this.resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        try {
            this.reader = new DCInputsReader();
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e);
        }

        collectionId = commandLine.getOptionValue('c');
        filename = commandLine.getOptionValue('f');

        if (commandLine.hasOption("er")) {
            abortOnError = true;
        }
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context(Context.Mode.BATCH_EDIT);
        assignCurrentUserInContext(context);
        assignSpecialGroupsInContext();

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
            context.turnOffAuthorisationSystem();
            performImport(inputStream);
            context.complete();
            context.restoreAuthSystemState();
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
        } catch (EncryptedDocumentException | IOException e) {
            throw new BulkImportException("An error occurs during the workbook creation", e);
        }
    }

    private void validateWorkbook(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new BulkImportException("The Workbook should have at least one sheet");
        }

        List<String> groups = getSubmissionFormMetadataGroups();

        for (Sheet sheet : workbook) {
            String name = sheet.getSheetName();

            if (WorkbookUtils.isSheetEmpty(sheet)) {
                throw new BulkImportException("The sheet " + name + " of the Workbook is empty");
            }

            if (WorkbookUtils.isRowEmpty(sheet.getRow(0))) {
                throw new BulkImportException("The header of sheet " + name + " of the Workbook is empty");
            }

            if (isMetadataGroupsSheet(sheet) && !groups.contains(name)) {
                throw new BulkImportException("The sheet name " + name + " is not a valid metadata group");
            }

            validateHeaders(sheet);
        }
    }

    private void validateHeaders(Sheet sheet) {
        List<String> headers = WorkbookUtils.getAllHeaders(sheet);
        validateMainHeaders(sheet, headers);
        validateMetadataFields(sheet, headers);
    }

    private void validateMainHeaders(Sheet sheet, List<String> headers) {

        String[] mandatoryMainHeaders = getMandatoryMainHeadersBySheetType(sheet);
        validateMandatoryMainHeaders(sheet, headers, mandatoryMainHeaders);

        String[] optionalMainHeaders = getOptionalMainHeadersBySheetType(sheet);
        validateOptionalMainHeaders(sheet, headers, mandatoryMainHeaders, optionalMainHeaders);

    }

    private String[] getMandatoryMainHeadersBySheetType(Sheet sheet) {
        BulkImportSheetType sheetType = BulkImportSheetType.getTypeFromSheet(sheet);
        switch (sheetType) {
            case ENTITY_ROWS:
                return ENTITY_ROWS_SHEET_HEADERS;
            case BITSTREAMS:
                return BITSTREAMS_SHEET_HEADERS;
            case METADATA_GROUPS:
                return METADATA_GROUPS_SHEET_HEADERS;
            default:
                throw new IllegalStateException("Unexpected sheet type: " + sheetType);
        }
    }

    private String[] getOptionalMainHeadersBySheetType(Sheet sheet) {
        BulkImportSheetType sheetType = BulkImportSheetType.getTypeFromSheet(sheet);
        switch (sheetType) {
            case ENTITY_ROWS:
                return ENTITY_ROWS_SHEET_OPTIONAL_HEADERS;
            default:
                return new String[] {};
        }
    }

    private void validateMandatoryMainHeaders(Sheet sheet, List<String> headers, String[] mandatoryMainHeaders) {

        String sheetName = sheet.getSheetName();

        if (headers.size() < mandatoryMainHeaders.length) {
            throw new BulkImportException("The columns " + ArrayUtils.toString(mandatoryMainHeaders)
                + " are required for the sheet " + sheetName);
        }

        for (int i = 0; i < mandatoryMainHeaders.length; i++) {
            String header = headers.get(i);
            String expected = mandatoryMainHeaders[i];
            if (!mandatoryMainHeaders[i].equals(header)) {
                throw new BulkImportException("Wrong " + expected + " header on sheet " + sheetName + ": " + header);
            }
        }

    }

    private void validateOptionalMainHeaders(Sheet sheet, List<String> headers, String[] mandatoryMainHeaders,
        String[] optionalMainHeaders) {

        long optionalHeadersCount = countOptionalHeaders(headers, optionalMainHeaders);
        long maxMainHeadersCount = mandatoryMainHeaders.length + optionalHeadersCount;

        for (String optionalMainHeader : optionalMainHeaders) {
            int indexOfOptionalHeader = headers.indexOf(optionalMainHeader);
            if (indexOfOptionalHeader >= maxMainHeadersCount) {
                throw new BulkImportException("The optional column " + optionalMainHeader
                    + " present in sheet " + sheet.getSheetName() + " must be placed before the metadata fields");
            }
        }

    }

    private void validateMetadataFields(Sheet sheet, List<String> headers) {

        List<String> metadataFields = headers.subList(getFirstMetadataIndex(sheet), headers.size());
        List<String> expectedMetadataFields = getSubmissionFormMetadataBySheetType(sheet);

        List<String> invalidMetadataMessages = new ArrayList<>();


        for (String metadataField : metadataFields) {

            String metadata = getMetadataField(metadataField);

            if (StringUtils.isBlank(metadata)) {
                invalidMetadataMessages.add("Empty metadata");
                continue;
            }

            if (!expectedMetadataFields.contains(metadata)) {
                invalidMetadataMessages.add(metadata + " is not valid for the given collection");
                continue;
            }

            if (isUnknownMetadataField(metadata)) {
                invalidMetadataMessages.add(metadata + " not found");
            }

        }

        if (CollectionUtils.isNotEmpty(invalidMetadataMessages)) {
            throw new BulkImportException("The following metadata fields of the sheet named '" + sheet.getSheetName()
                + "' are invalid:" + invalidMetadataMessages);
        }
    }

    private List<String> getSubmissionFormMetadataBySheetType(Sheet sheet) {
        BulkImportSheetType sheetType = BulkImportSheetType.getTypeFromSheet(sheet);
        switch (sheetType) {
            case ENTITY_ROWS:
                return getSubmissionFormMetadata();
            case BITSTREAMS:
                return getSubmissionFormBitstreamMetadata();
            case METADATA_GROUPS:
                return getSubmissionFormMetadataGroup(sheet.getSheetName());
            default:
                throw new IllegalStateException("Unexpected sheet type: " + sheetType);
        }
    }

    private List<String> getSubmissionFormBitstreamMetadata() {
        try {
            return this.reader.getUploadMetadataFieldsFromCollection(getCollection());
        } catch (DCInputsReaderException e) {
            throw new BulkImportException("An error occurs reading the input configuration by collection", e);
        }
    }

    private List<String> getSubmissionFormMetadataGroup(String groupName) {
        try {
            return this.reader.getAllNestedMetadataByGroupName(getCollection(), groupName);
        } catch (DCInputsReaderException e) {
            throw new BulkImportException("An error occurs reading the input configuration "
                + "by group name " + groupName, e);
        }
    }

    private List<String> getSubmissionFormMetadata() {
        try {
            return this.reader.getSubmissionFormMetadata(getCollection());
        } catch (DCInputsReaderException e) {
            throw new BulkImportException("An error occurs reading the input configuration by collection", e);
        }
    }

    private List<String> getSubmissionFormMetadataGroups() {
        try {
            return this.reader.getSubmissionFormMetadataGroups(getCollection());
        } catch (DCInputsReaderException e) {
            throw new BulkImportException("An error occurs reading the input configuration by collection", e);
        }
    }

    private List<EntityRow> getValidEntityRows(Workbook workbook) {
        Sheet entityRowSheet = workbook.getSheetAt(0);
        Map<String, Integer> headers = getHeaderMap(entityRowSheet);

        List<Sheet> metadataGroupSheets = getAllMetadataGroupSheets(workbook);

        handler.logInfo("Start reading all the metadata group rows");
        List<MetadataGroup> metadataGroups = getValidMetadataGroups(metadataGroupSheets);
        handler.logInfo("Found " + metadataGroups.size() + " metadata groups to process");

        List<UploadDetails> uploadDetails = getUploadDetails(workbook);

        return WorkbookUtils.getRows(entityRowSheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .filter(this::isEntityRowRowValid)
            .map(row -> buildEntityRow(row, headers, metadataGroups, uploadDetails))
            .collect(Collectors.toList());
    }

    private boolean isEntityRowRowValid(Row row) {
        String id = getIdFromRow(row);
        String action = getActionFromRow(row);

        if (!isValidId(id, false)) {
            handleValidationErrorOnRow(row, "Invalid ID " + id);
            return false;
        }

        if (!isValidAction(id, action, row)) {
            return false;
        }

        return areMetadataValuesValid(row, true);
    }

    private EntityRow buildEntityRow(Row row, Map<String, Integer> headers,
        List<MetadataGroup> metadataGroups, List<UploadDetails> uploadDetails) {

        String id = getIdFromRow(row);
        String action = getActionFromRow(row);
        Boolean discoverable = headers.containsKey(DISCOVERABLE_HEADER) ? getDiscoverableFromRow(row) : null;

        MultiValuedMap<String, MetadataValueVO> metadata = getMetadataFromRow(row, headers);
        List<MetadataGroup> ownMetadataGroup = getOwnChildRows(row, metadataGroups);
        List<UploadDetails> ownUploadDetails = getOwnChildRows(row, uploadDetails);

        return new EntityRow(id, action, row.getRowNum(), discoverable, metadata, ownMetadataGroup, ownUploadDetails);

    }

    private List<Sheet> getAllMetadataGroupSheets(Workbook workbook) {
        return StreamSupport.stream(workbook.spliterator(), false)
            .filter(sheet -> sheet.getWorkbook().getSheetIndex(sheet) != 0)
            .filter(sheet -> !BITSTREAMS_SHEET_NAME.equals(sheet.getSheetName()))
            .collect(Collectors.toList());
    }

    /**
     * Read all the metadata groups from all the given sheets.
     *
     * @param  metadataGroupSheets the metadata group sheets to read
     * @return                     a list of MetadataGroup
     */
    private List<MetadataGroup> getValidMetadataGroups(List<Sheet> metadataGroupSheets) {
        return metadataGroupSheets.stream()
            .flatMap(this::getValidMetadataGroups)
            .collect(Collectors.toList());
    }

    /**
     * Read all the metadata groups from a single sheet.
     *
     * @param  metadataGroupSheet the metadata group sheet
     * @return                    a stream of MetadataGroup
     */
    private Stream<MetadataGroup> getValidMetadataGroups(Sheet metadataGroupSheet) {
        Map<String, Integer> headers = getHeaderMap(metadataGroupSheet);

        Stream<MetadataGroup> metadataGroupStream = WorkbookUtils.getRows(metadataGroupSheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .filter(this::isMetadataGroupRowValid)
            .map(row -> buildMetadataGroup(row, headers));

        return metadataGroupStream;
    }

    private boolean isMetadataGroupRowValid(Row row) {
        String parentId = getParentIdFromRow(row);

        if (StringUtils.isBlank(parentId)) {
            handleValidationErrorOnRow(row, "No " + PARENT_ID_HEADER + " set");
            return false;
        }

        if (!isValidId(parentId, true)) {
            handleValidationErrorOnRow(row, "Invalid " + PARENT_ID_HEADER + ": " + parentId);
            return false;
        }

        return areMetadataValuesValid(row, false);
    }

    private boolean areMetadataValuesValid(Row row, boolean manyMetadataValuesAllowed) {

        int firstMetadataIndex = getFirstMetadataIndex(row.getSheet());
        for (int index = firstMetadataIndex; index < row.getLastCellNum(); index++) {

            String cellValue = WorkbookUtils.getCellValue(row, index);
            String[] values = isNotBlank(cellValue) ? splitByWholeSeparator(cellValue, METADATA_SEPARATOR)
                : new String[] { "" };
            if (values.length > 1 && !manyMetadataValuesAllowed) {
                handleValidationErrorOnRow(row, "Multiple metadata value on the same cell not allowed "
                    + "in the metadata group sheets: " + cellValue);
                return false;
            }

            for (String value : values) {
                if (!isMetadataValueValid(row, value)) {
                    return false;
                }
            }
        }

        return true;

    }

    /**
     * The allowed metadata value syntax are:
     * <ul>
     * <li>value</li>
     * <li>value$$authority</li>
     * <li>value$$security-level</li>
     * <li>value$$authority$$security-level</li>
     * <li>value$$authority$$confidence</li>
     * <li>value$$authority$$confidence$$security-level</li>
     * </ul>
     */
    private boolean isMetadataValueValid(Row row, String value) {

        if (!value.contains(METADATA_ATTRIBUTES_SEPARATOR)) {
            return true;
        }

        String[] attributes = StringUtils.split(value, METADATA_ATTRIBUTES_SEPARATOR);
        if (attributes.length > 4) {
            handleValidationErrorOnRow(row, "Invalid metadata value " + value + ": too many sections "
                + "splitted by " + METADATA_ATTRIBUTES_SEPARATOR);
            return false;
        }

        if (attributes.length == 4 && isConfidenceNotValid(attributes[2])) {
            handleValidationErrorOnRow(row,
                "Invalid metadata value " + value + ": invalid confidence value " + attributes[2]);
            return false;
        }

        if (attributes.length == 4 && isSecurityLevelNotValid(attributes[3])) {
            handleValidationErrorOnRow(row,
                "Invalid metadata value " + value + ": invalid security level " + attributes[3]);
            return false;
        }

        if (attributes.length == 3 && isSecurityLevelNotValid(attributes[2]) && isConfidenceNotValid(attributes[2])) {
            handleValidationErrorOnRow(row,
                "Invalid metadata value " + value + ": invalid security level or confidence value " + attributes[2]);
            return false;
        }

        return true;
    }

    private MetadataGroup buildMetadataGroup(Row row, Map<String, Integer> headers) {
        String parentId = getParentIdFromRow(row);
        MultiValuedMap<String, MetadataValueVO> metadata = getMetadataFromRow(row, headers);
        return new MetadataGroup(parentId, row.getSheet().getSheetName(), metadata);
    }

    private List<UploadDetails> getUploadDetails(Workbook workbook) {

        Sheet uploadSheet = workbook.getSheet(BITSTREAMS_SHEET_NAME);

        if (uploadSheet == null) {
            return Collections.emptyList();
        }

        handler.logInfo("Start reading all the bitstream rows");

        List<UploadDetails> uploadDetails = getRows(uploadSheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .filter(this::isUploadRowValid)
            .map(row -> buildUploadDetails(row))
            .collect(Collectors.toList());

        handler.logInfo("Found " + uploadDetails.size() + " bitstreams to process");

        return uploadDetails;

    }

    private boolean isUploadRowValid(Row row) {

        String parentId = getParentIdFromRow(row);

        if (StringUtils.isBlank(parentId)) {
            handleValidationErrorOnRow(row, "No " + PARENT_ID_HEADER + " set");
            return false;
        }

        if (!isValidId(parentId, true)) {
            handleValidationErrorOnRow(row, "Invalid " + PARENT_ID_HEADER + ": " + parentId);
            return false;
        }

        String filePath = getCellValue(row, FILE_PATH_HEADER);
        String position = getCellValue(row, BITSTREAM_POSITION_HEADER);

        if (isAllBlank(filePath, position)) {
            String message = format("Both %s and %s could not be empty", FILE_PATH_HEADER, BITSTREAM_POSITION_HEADER);
            handleValidationErrorOnRow(row, message);
            return false;
        }

        if (isInvalidBitstreamPosition(position)) {
            handleValidationErrorOnRow(row, "Invalid " + BITSTREAM_POSITION_HEADER + ": " + position);
        }

        List<String> accessConditionsValidations = validateAccessConditions(row);
        if (CollectionUtils.isNotEmpty(accessConditionsValidations)) {
            handleValidationErrorOnRow(row, "Invalid " + ACCESS_CONDITION_HEADER + ": " + accessConditionsValidations);
            return false;
        }

        return true;

    }

    private List<String> validateAccessConditions(Row row) {

        Map<String, AccessConditionOption> accessConditionOptions = getUploadAccessConditions();

        return Arrays.stream(getAccessConditionValues(row))
            .map(accessCondition -> splitByWholeSeparator(accessCondition, ACCESS_CONDITION_ATTRIBUTES_SEPARATOR)[0])
            .filter(accessConditionName -> !accessConditionOptions.containsKey(accessConditionName))
            .collect(Collectors.toList());
    }

    private UploadDetails buildUploadDetails(Row row) {

        Map<String, Integer> headers = getHeaderMap(row.getSheet());

        String[] accessCondition = getAccessConditionValues(row);

        String parentId = getValueFromRow(row, PARENT_ID_HEADER);
        int rowNumber = row.getRowNum();
        String filePath = getValueFromRow(row, FILE_PATH_HEADER);
        String bundleName = getValueFromRow(row, BUNDLE_HEADER);
        Integer bitstreamPosition = getBitstreamPosition(row);
        List<AccessCondition> accessConditions = buildAccessConditions(row, accessCondition);
        boolean additionalAccessCondition = BooleanUtils.toBoolean(getCellValue(row.getCell(5)));
        MultiValuedMap<String, MetadataValueVO> metadata = getMetadataFromRow(row, headers);

        return new UploadDetails(parentId, rowNumber, filePath, bundleName, bitstreamPosition,
            accessConditions, additionalAccessCondition, metadata);

    }

    private boolean isInvalidBitstreamPosition(String position) {
        try {
            return isNotBlank(position) && Integer.valueOf(position) < 1;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private Integer getBitstreamPosition(Row row) {
        String position = getValueFromRow(row, BITSTREAM_POSITION_HEADER);
        return isNotBlank(position) ? Integer.valueOf(position) : null;
    }

    private List<AccessCondition> buildAccessConditions(Row row, String[] accessConditions) {

        if (accessConditions.length == 0) {
            return List.of();
        }

        return Arrays.stream(accessConditions)
            .map(accessCondition -> splitByWholeSeparator(accessCondition, ACCESS_CONDITION_ATTRIBUTES_SEPARATOR))
            .map(accessConditionAttributes -> buildAccessCondition(accessConditionAttributes))
            .collect(Collectors.toList());
    }

    private String[] getAccessConditionValues(Row row) {
        String accessConditionCellValue = getCellValue(row, ACCESS_CONDITION_HEADER);
        return splitByWholeSeparator(accessConditionCellValue, METADATA_SEPARATOR);
    }

    private AccessCondition buildAccessCondition(String[] accessCondition) {
        String name = accessCondition[0];
        String description = null;
        LocalDate startDate = null;
        LocalDate endDate = null;

        AccessConditionOption accessConditionOption = getUploadAccessConditions().get(name);

        if (accessConditionOption.getHasStartDate() && accessConditionOption.getHasEndDate()) {
            startDate = accessCondition.length > 1 ? parseDate(accessCondition[1]) : null;
            endDate = accessCondition.length > 2 ? parseDate(accessCondition[2]) : null;
            description = accessCondition.length == 4 ? accessCondition[3] : null;
        } else if (accessConditionOption.getHasStartDate()) {
            startDate = accessCondition.length > 1 ? parseDate(accessCondition[1]) : null;
            description = accessCondition.length == 3 ? accessCondition[2] : null;
        } else if (accessConditionOption.getHasEndDate()) {
            endDate = accessCondition.length > 1 ? parseDate(accessCondition[1]) : null;
            description = accessCondition.length == 3 ? accessCondition[2] : null;
        } else {
            description = accessCondition.length == 2 ? accessCondition[1] : null;
        }

        return new AccessCondition(name, description, startDate, endDate);
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

    private void performImport(List<EntityRow> entityRows) {
        handler.logInfo("Found " + entityRows.size() + " items to process");
        entityRows.forEach(entityRow -> performImport(entityRow));
    }

    private void performImport(EntityRow entityRow) {

        try {

            Item item = null;

            switch (entityRow.getAction()) {
                case ADD:
                case ADD_ARCHIVE:
                case ADD_WORKSPACE:
                    item = addItem(entityRow);
                    break;
                case UPDATE:
                case UPDATE_WORKFLOW:
                case UPDATE_ARCHIVE:
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

        Item item = workspaceItem.getItem();

        PackageUtils.addDepositLicense(context, null, item, workspaceItem.getCollection());

        addMetadata(item, entityRow, false);
        addUploadsToItem(item, entityRow);
        configureDiscoverability(item, entityRow);

        String itemId = item.getID().toString();
        int row = entityRow.getRow();

        switch (entityRow.getAction()) {
            case ADD:
            case NOT_SPECIFIED:
                startWorkflow(entityRow, workspaceItem);
                break;
            case ADD_ARCHIVE:
                installItem(entityRow, workspaceItem);
                break;
            case ADD_WORKSPACE:
                handler.logInfo("Row " + row + " - WorkspaceItem created successfully - ID: " + itemId);
                break;
            default:
                break;
        }

        return item;

    }

    private void addUploadsToItem(Item item, EntityRow entityRow) {

        ArrayListValuedHashMap<String, Bitstream> bitstreamsByBundle = findBitstreamsGroupedByBundle(item);

        for (UploadDetails uploadDetails : entityRow.getUploadDetails()) {

            Integer bitstreamPosition = uploadDetails.getBitstreamPosition();
            if (bitstreamPosition == null) {
                createBitstream(item, uploadDetails);
                continue;
            }

            int zeroBasedPosition = bitstreamPosition - 1;

            List<Bitstream> bitstreams = bitstreamsByBundle.get(uploadDetails.getBundleName());

            if (zeroBasedPosition >= bitstreams.size()) {
                handler.logError("Sheet " + BITSTREAMS_SHEET_NAME + " - Row " + uploadDetails.getRow() +
                    " - No bitstream found at position " + bitstreamPosition + " for Item with id " + item.getID());
                continue;
            }

            updateOrDeleteBitstream(bitstreams.get(zeroBasedPosition), item, uploadDetails);

        }
    }

    private ArrayListValuedHashMap<String, Bitstream> findBitstreamsGroupedByBundle(Item item) {
        ArrayListValuedHashMap<String, Bitstream> bitstreams = new ArrayListValuedHashMap<String, Bitstream>();
        for (Bundle bundle : item.getBundles()) {
            bitstreams.putAll(bundle.getName(), bundle.getBitstreams());
        }
        return bitstreams;
    }

    private void updateOrDeleteBitstream(Bitstream bitstream, Item item, UploadDetails uploadDetails) {
        if (StringUtils.isEmpty(uploadDetails.getFilePath())) {
            deleteBitstream(bitstream, uploadDetails);
        } else {
            updateBitstream(bitstream, item, uploadDetails);
        }
    }

    private void deleteBitstream(Bitstream bitstream, UploadDetails uploadDetails) {
        try {
            bitstreamService.delete(context, bitstream);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e);
        }

        handler.logInfo("Sheet " + BITSTREAMS_SHEET_NAME + " - Row " + uploadDetails.getRow()
            + " - Bitstream deleted successfully - ID: " + bitstream.getID());
    }

    private void updateBitstream(Bitstream bitstream, Item item, UploadDetails uploadDetails) {

        updateBitstreamMetadata(bitstream, uploadDetails);
        updateBitstreamPolicies(bitstream, item, uploadDetails);

        handler.logInfo("Sheet " + BITSTREAMS_SHEET_NAME + " - Row " + uploadDetails.getRow()
            + " - Bitstream updated successfully - ID: " + bitstream.getID());
    }

    private void updateBitstreamMetadata(Bitstream bitstream, UploadDetails uploadDetails) {
        try {
            removeMetadata(bitstream, uploadDetails.getMetadata());
            addMetadata(bitstream, uploadDetails.getMetadata());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private void updateBitstreamPolicies(Bitstream bitstream, Item item, UploadDetails uploadDetails) {

        if (uploadDetails.isNotAdditionalAccessCondition()) {
            removeReadPolicies(bitstream, TYPE_CUSTOM);
        }

        if (isAppendModeDisabled() && item.isArchived()) {
            removeReadPolicies(bitstream, TYPE_INHERITED);
        }

        setBitstreamPolicies(bitstream, uploadDetails);

    }

    private void setBitstreamPolicies(Bitstream bitstream, UploadDetails uploadDetails) {
        uploadDetails.getAccessConditions()
            .forEach(accessCondition -> createResourcePolicy(bitstream, uploadDetails, accessCondition));
    }

    private void removeReadPolicies(Bitstream bitstream, String type) {
        try {
            resourcePolicyService.removePolicies(context, bitstream, type, Constants.READ);
        } catch (SQLException | AuthorizeException e) {
            throw new BulkImportException(e);
        }
    }

    private void createResourcePolicy(DSpaceObject obj, UploadDetails uploadDetails, AccessCondition accessCondition) {

        String name = accessCondition.getName();
        String description = accessCondition.getDescription();
        LocalDate startDate = accessCondition.getStartDate();
        LocalDate endDate = accessCondition.getEndDate();

        for (AccessConditionOption aco : uploadAccessConditions.values()) {
            if (aco.getName().equalsIgnoreCase(name)) {
                try {
                    aco.createResourcePolicy(context, obj, name, description, startDate, endDate);
                } catch (Exception e) {
                    handler.logError("Sheet " + BITSTREAMS_SHEET_NAME + " - Row "
                        + uploadDetails.getRow() + " - " + e.getMessage());
                }
                break;
            }
        }

    }

    private void createBitstream(Item item, UploadDetails uploadDetails) {

        String filePath = uploadDetails.getFilePath();

        Optional<InputStream> inputStream = importFileUtil.getInputStream(filePath);

        if (inputStream.isEmpty()) {
            handler.logError("Cannot create bitstream from file at path " + filePath);
            return;
        }

        Bundle bundle = getBundle(item, uploadDetails);

        Bitstream bitstream = createBitstream(bundle, inputStream.get());

        try {
            addMetadata(bitstream, uploadDetails.getMetadata());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

        setBitstreamPolicies(bitstream, uploadDetails);
        setBitstreamFormat(bitstream);

        handler.logInfo("Sheet " + BITSTREAMS_SHEET_NAME + " - Row " + uploadDetails.getRow()
            + " - Bitstream created successfully - ID: " + bitstream.getID());

    }

    private Bundle getBundle(Item item, UploadDetails uploadDetails) {
        String bundleName = uploadDetails.getBundleName();
        return getBundleByName(item, bundleName)
            .orElseGet(() -> createBundle(item, bundleName));
    }

    private Optional<Bundle> getBundleByName(Item item, String name) {
        return item.getBundles(name).stream().findFirst();
    }

    private Bundle createBundle(Item item, String bundleName) {
        try {
            return bundleService.create(context, item, bundleName);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private void setBitstreamFormat(Bitstream bitstream) {
        try {
            BitstreamFormat bf = bitstreamFormatService.guessFormat(context, bitstream);
            bitstreamService.setFormat(context, bitstream, bf);
            bitstreamService.update(context, bitstream);
        } catch (SQLException | AuthorizeException e) {
            handler.logError(e.getMessage());
        }
    }

    private Bitstream createBitstream(Bundle bundle, InputStream inputStream) {
        try {
            return bitstreamService.create(context, bundle, inputStream);
        } catch (IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private void installItem(EntityRow entityRow, InProgressSubmission<?> inProgressItem)
        throws SQLException, AuthorizeException {

        String itemId = inProgressItem.getItem().getID().toString();
        int row = entityRow.getRow();

        if (authorizeService.isAdmin(context)) {
            installItemService.installItem(context, inProgressItem);
            handler.logInfo("Row " + row + " - Item archived successfully - ID: " + itemId);
        } else {
            handler.logWarning("Row " + row + " - Current user can't deposit an item directly bypassing the workflow");
        }

    }

    private void startWorkflow(EntityRow entityRow, WorkspaceItem workspaceItem)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        String itemId = workspaceItem.getItem().getID().toString();
        int row = entityRow.getRow();

        List<String> validationErrors = validateItem(workspaceItem);
        if (CollectionUtils.isEmpty(validationErrors)) {
            workflowService.start(context, workspaceItem);
            handler.logInfo("Row " + row + " - WorkflowItem created successfully - ID: " + itemId);
        } else {
            handler.logWarning("Row " + row + " - Invalid item left in workspace - ID: " + itemId
                + " - validation errors: " + validationErrors);
        }

    }

    private Item updateItem(EntityRow entityRow) throws Exception {
        Item item = findItem(entityRow);
        if (item == null) {
            throw new BulkImportException("No item to update found for entity with id " + entityRow.getId());
        }

        return updateItem(entityRow, item);
    }

    private Item updateItem(EntityRow entityRow, Item item)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        if (!isInSpecifiedCollection(item)) {
            throw new BulkImportException("The item related to the entity with id " + entityRow.getId()
                + " have a different collection");
        }

        addMetadata(item, entityRow, true);
        addUploadsToItem(item, entityRow);
        configureDiscoverability(item, entityRow);

        handler.logInfo("Row " + entityRow.getRow() + " - Item updated successfully - ID: " + item.getID());

        switch (entityRow.getAction()) {
            case UPDATE_WORKFLOW:
                startWorkflow(entityRow, item);
                break;
            case UPDATE_ARCHIVE:
                installItem(entityRow, item);
                break;
            default:
                itemService.update(context, item);
                break;
        }

        return item;

    }

    private void installItem(EntityRow entityRow, Item item) throws SQLException, AuthorizeException {

        InProgressSubmission<Integer> inProgressItem = findInProgressSubmission(item);
        if (inProgressItem != null) {
            installItem(entityRow, inProgressItem);
        } else {
            handler.logInfo("Row " + entityRow.getRow() + " - No workspace/workflow item to archive found");
        }

    }

    private void startWorkflow(EntityRow entityRow, Item item)
        throws SQLException, AuthorizeException, IOException, WorkflowException {

        WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, item);
        if (workspaceItem != null) {
            startWorkflow(entityRow, workspaceItem);
        } else {
            handler.logInfo("Row " + entityRow.getRow() + " - No workspace item to start found");
        }
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
        return entityRow.getId() != null ? itemSearchService.search(context, entityRow.getId()) : null;
    }

    private List<String> validateItem(WorkspaceItem workspaceItem) {
        return validationService.validate(context, workspaceItem).stream()
            .map(error -> error.getMessage() + ": " + error.getPaths())
            .collect(Collectors.toList());
    }

    private void configureDiscoverability(Item item, EntityRow entityRow) {
        Boolean discoverable = entityRow.getDiscoverable();
        if (discoverable != null) {
            item.setDiscoverable(discoverable);
        }
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

    private void addMetadata(DSpaceObject dso, MultiValuedMap<String, MetadataValueVO> metadata) throws SQLException {

        DSpaceObjectService<DSpaceObject> dSpaceObjectService = ContentServiceFactory.getInstance()
            .getDSpaceObjectService(dso);

        Iterable<String> metadataFields = metadata.keySet();
        for (String field : metadataFields) {
            String lang = getMetadataLanguage(field);
            MetadataField metadataField = metadataFieldService.findByString(context, getMetadataField(field), '.');
            for (MetadataValueVO metadataValue : metadata.get(field)) {
                metadataValue = bulkImportTransformerService.converter(context, field, metadataValue);
                String authority = metadataValue.getAuthority();
                int confidence = metadataValue.getConfidence();
                String value = metadataValue.getValue();
                Integer security = metadataValue.getSecurityLevel();
                if (StringUtils.isNotEmpty(value)) {
                    dSpaceObjectService.addSecuredMetadata(context, dso, metadataField, lang, value,
                        authority, confidence, security);
                }
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

    private void removeMetadata(DSpaceObject dso, MultiValuedMap<String, MetadataValueVO> metadata)
        throws SQLException {

        Iterable<String> fields = metadata.keySet();
        for (String field : fields) {
            String language = getMetadataLanguage(field);
            MetadataField metadataField = metadataFieldService.findByString(context, getMetadataField(field), '.');
            removeSingleMetadata(dso, metadataField, language);
        }

    }

    private void removeSingleMetadata(DSpaceObject dso, MetadataField field, String language) throws SQLException {

        DSpaceObjectService<DSpaceObject> dSpaceObjectService = ContentServiceFactory.getInstance()
            .getDSpaceObjectService(dso);

        List<MetadataValue> metadata = dSpaceObjectService.getMetadata(dso, field.getMetadataSchema().getName(),
            field.getElement(), field.getQualifier(), language);
        dSpaceObjectService.removeMetadataValues(context, dso, metadata);
    }

    private String getMetadataField(String field) {
        return field.contains(LANGUAGE_SEPARATOR_PREFIX) ? splitByWholeSeparator(field, LANGUAGE_SEPARATOR_PREFIX)[0]
            : field;
    }

    private String getMetadataLanguage(String field) {
        if (field.contains(LANGUAGE_SEPARATOR_PREFIX)) {
            return splitByWholeSeparator(field, LANGUAGE_SEPARATOR_PREFIX)[1].replace(LANGUAGE_SEPARATOR_SUFFIX, "");
        }
        return null;
    }

    private String getIdFromRow(Row row) {
        return getValueFromRow(row, ID_HEADER);
    }

    private String getActionFromRow(Row row) {
        return getValueFromRow(row, ACTION_HEADER);
    }

    private String getParentIdFromRow(Row row) {
        return getValueFromRow(row, PARENT_ID_HEADER);
    }

    private Boolean getDiscoverableFromRow(Row row) {
        String discoverableValue = getValueFromRow(row, DISCOVERABLE_HEADER);
        return StringUtils.isBlank(discoverableValue) ? null : toBooleanObject(discoverableValue);
    }

    private String getValueFromRow(Row row, String header) {

        Sheet sheet = row.getSheet();

        List<String> headers = WorkbookUtils.getAllHeaders(sheet);
        int headerIndex = headers.indexOf(header);

        if (headerIndex != -1) {
            return WorkbookUtils.getCellValue(row, headerIndex);
        }

        if (isOptionalHeader(sheet, header)) {
            return null;
        } else {
            throw new IllegalStateException("Header not found " + header + " on sheet " + sheet.getSheetName());
        }

    }

    private MultiValuedMap<String, MetadataValueVO> getMetadataFromRow(Row row, Map<String, Integer> headers) {

        MultiValuedMap<String, MetadataValueVO> metadata = new ArrayListValuedHashMap<String, MetadataValueVO>();

        int firstMetadataIndex = getFirstMetadataIndex(row.getSheet());
        boolean isMetadataGroupsSheet = isMetadataGroupsSheet(row.getSheet());

        for (String header : headers.keySet()) {
            int index = headers.get(header);
            if (index >= firstMetadataIndex) {

                String cellValue = WorkbookUtils.getCellValue(row, index);
                String[] values = isNotBlank(cellValue) ? splitByWholeSeparator(cellValue, METADATA_SEPARATOR)
                    : new String[] { "" };

                List<MetadataValueVO> metadataValues = Arrays.stream(values)
                    .map(value -> buildMetadataValueVO(row, value, isMetadataGroupsSheet))
                    .collect(Collectors.toList());

                metadata.putAll(header, metadataValues);
            }
        }

        return metadata;
    }

    private MetadataValueVO buildMetadataValueVO(Row row, String metadataValue, boolean isMetadataGroup) {

        if (isBlank(metadataValue)) {
            return new MetadataValueVO(isMetadataGroup ? PLACEHOLDER_PARENT_METADATA_VALUE : metadataValue);
        }

        if (!metadataValue.contains(METADATA_ATTRIBUTES_SEPARATOR)) {
            return new MetadataValueVO(metadataValue);
        }

        String[] valueAttributes = StringUtils.split(metadataValue, METADATA_ATTRIBUTES_SEPARATOR);

        String value = valueAttributes[0];
        String authority = null;
        int confidence = -1;
        Integer securityLevel = null;

        if (valueAttributes.length > 3) {
            authority = valueAttributes[1];
            confidence = Integer.valueOf(valueAttributes[2]);
            securityLevel = parseSecurityLevel(valueAttributes[3]);
        }

        if (valueAttributes.length == 3) {
            authority = valueAttributes[1];
            if (isSecurityLevelNotValid(valueAttributes[2])) {
                confidence = Integer.valueOf(valueAttributes[2]);
            } else {
                securityLevel = parseSecurityLevel(valueAttributes[2]);
                confidence = 600;
            }
        }

        if (valueAttributes.length == 2) {
            if (isSecurityLevelNotValid(valueAttributes[1])) {
                authority = valueAttributes[1];
                confidence = 600;
            } else {
                securityLevel = parseSecurityLevel(valueAttributes[1]);
            }
        }

        return new MetadataValueVO(value, authority, confidence, securityLevel);
    }

    private Integer parseSecurityLevel(String securityLevel) {
        return Integer.valueOf(removeSecurityLevelPrefix(securityLevel));
    }

    private boolean isMetadataGroupsSheet(Sheet sheet) {
        return BulkImportSheetType.getTypeFromSheet(sheet) == BulkImportSheetType.METADATA_GROUPS;
    }

    private long countOptionalHeaders(List<String> headers, String[] optionalHeaders) {
        return headers.stream()
            .filter(header -> ArrayUtils.contains(optionalHeaders, header))
            .count();
    }

    private boolean isOptionalHeader(Sheet sheet, String header) {
        String[] optionalMainHeaders = getOptionalMainHeadersBySheetType(sheet);
        return ArrayUtils.contains(optionalMainHeaders, header);
    }

    private int getFirstMetadataIndex(Sheet sheet) {

        List<String> headers = WorkbookUtils.getAllHeaders(sheet);

        String[] mandatoryHeaders = getMandatoryMainHeadersBySheetType(sheet);
        String[] optionalHeaders = getOptionalMainHeadersBySheetType(sheet);

        int firstMetadataIndex = mandatoryHeaders.length;

        for (String optionalHeader : optionalHeaders) {
            if (headers.contains(optionalHeader)) {
                firstMetadataIndex++;
            }
        }

        return firstMetadataIndex;

    }

    private boolean isUnknownMetadataField(String metadataField) {
        try {
            return metadataFieldService.findByString(context, metadataField, '.') == null;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private <T extends ChildRow> List<T> getOwnChildRows(Row row, List<T> childRows) {
        String id = getIdFromRow(row);
        int rowIndex = row.getRowNum() + 1;
        return childRows.stream()
            .filter(g -> g.getParentId().equals(id) || g.getParentId().equals(ROW_ID + ID_SEPARATOR + rowIndex))
            .collect(Collectors.toList());
    }

    private boolean isValidAction(String id, String action, Row row) {

        if (isBlank(action)) {
            return true;
        }

        ImportAction[] actions = ImportAction.values();
        if (!ImportAction.isValid(action)) {
            handleValidationErrorOnRow(row,
                "Invalid action " + action + ": allowed values are " + Arrays.toString(actions));
            return false;
        }

        if (isBlank(id) && !ImportAction.valueOf(action).isAddAction()) {
            handleValidationErrorOnRow(row, "Only adding actions can have an empty ID");
            return false;
        }

        if (isNotBlank(id) && ImportAction.valueOf(action).isAddAction()) {
            handleValidationErrorOnRow(row, "Adding actions can not have an ID set");
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

    private boolean isInSpecifiedCollection(Item item) throws SQLException {
        Collection collection = getCollection();
        if (item.getOwningCollection() != null) {
            return item.getOwningCollection().equals(collection);
        }

        InProgressSubmission<Integer> inProgressSubmission = findInProgressSubmission(item);
        return collection.equals(inProgressSubmission.getCollection());
    }

    private InProgressSubmission<Integer> findInProgressSubmission(Item item) throws SQLException {
        WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, item);
        return workspaceItem != null ? workspaceItem : workflowItemService.findByItem(context, item);
    }

    private Map<String, AccessConditionOption> getUploadAccessConditions() {

        if (uploadAccessConditions != null) {
            return uploadAccessConditions;
        }

        UploadConfiguration uploadConfiguration = uploadConfigurationService.getMap().get("upload");
        if (uploadConfiguration == null) {
            throw new IllegalStateException("No upload access conditions configuration found");
        }

        uploadAccessConditions = uploadConfiguration.getOptions().stream()
            .collect(Collectors.toMap(AccessConditionOption::getName, Function.identity()));

        return uploadAccessConditions;
    }

    private boolean isConfidenceNotValid(String confidence) {
        return !isCreatable(confidence);
    }

    private boolean isSecurityLevelNotValid(String securityLevel) {
        return !Strings.CS.startsWith(securityLevel, SECURITY_LEVEL_PREFIX)
            || !isCreatable(removeSecurityLevelPrefix(securityLevel));
    }

    private String removeSecurityLevelPrefix(String str) {
        return StringUtils.removeStart(str, SECURITY_LEVEL_PREFIX);
    }

    private void handleException(EntityRow entityRow, BulkImportException bie) {

        rollback();

        if (abortOnError) {
            throw bie;
        }

        String message = "Row " + entityRow.getRow() + " - " + getRootCauseMessage(bie);
        handler.logError(message);

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

    private void rollback() {
        try {
            context.rollback();
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    protected void assignCurrentUserInContext(Context context) throws SQLException, ParseException {
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

    private LocalDate parseDate(String date) {
        ZonedDateTime result = MultiFormatDateParser.parse(date);
        return result != null ? result.toLocalDate() : null;
    }

    private Collection getCollection() {
        try {
            return collectionService.find(context, UUID.fromString(collectionId));
        } catch (SQLException e) {
            throw new BulkImportException(e);
        }
    }

    private boolean isAppendModeDisabled() {
        return !configurationService.getBooleanProperty("core.authorization.installitem.inheritance-read.append-mode");
    }

    public void setImportFileUtil(ImportFileUtil importFileUtil) {
        this.importFileUtil = importFileUtil;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BulkImportScriptConfiguration<BulkImport> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("bulk-import", BulkImportScriptConfiguration.class);
    }

}
