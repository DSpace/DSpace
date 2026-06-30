/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.util.WorkbookUtils.getCellIndexFromHeaderName;
import static org.dspace.util.WorkbookUtils.getCellValue;
import static org.dspace.util.WorkbookUtils.getColumnWithoutHeader;
import static org.dspace.util.WorkbookUtils.getEntityTypeCellValue;
import static org.dspace.util.WorkbookUtils.getEntityTypeValue;
import static org.dspace.util.WorkbookUtils.getNotEmptyRowsSkippingHeader;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.script.service.CrisLayoutToolRenderValidator;
import org.dspace.layout.script.service.CrisLayoutToolValidationResult;
import org.dspace.layout.script.service.CrisLayoutToolValidator;
import org.dspace.util.WorkbookUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link CrisLayoutToolValidator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutToolValidatorImpl implements CrisLayoutToolValidator {

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private List<CrisLayoutToolRenderValidator> renderingValidators;

    private Map<String, CrisLayoutToolRenderValidator> renderingValidatorsByName;

    @PostConstruct
    private void setup() {
        renderingValidatorsByName = renderingValidators.stream()
            .collect(Collectors.toMap(CrisLayoutToolRenderValidator::getName, Function.identity()));
    }

    @Override
    public CrisLayoutToolValidationResult validate(Context context, Workbook workbook) {

        List<String> allEntityTypes = getAllEntityTypes(context);
        List<String> allMetadataFields = getAllMetadataFields(context);

        CrisLayoutToolValidationResult result = new CrisLayoutToolValidationResult();

        validateTabSheet(workbook, result, allEntityTypes);
        validateBoxSheet(workbook, result, allEntityTypes);
        validateTab2BoxSheet(workbook, result);
        validateBox2MetadataSheet(allMetadataFields, workbook, result);
        validateMetadataGroupsSheet(allMetadataFields, workbook, result);
        validateBoxPolicySheet(context, allMetadataFields, workbook, result);
        validateTabPolicySheet(context, allMetadataFields, workbook, result);

        return result;
    }

    private void validateTabSheet(Workbook workbook, CrisLayoutToolValidationResult result, List<String> entityTypes) {

        Sheet tabSheet = workbook.getSheet(TAB_SHEET);
        if (tabSheet == null) {
            result.addError("The " + TAB_SHEET + " sheet is missing");
            return;
        }

        int entityTypeColumn = getCellIndexFromHeaderName(tabSheet, ENTITY_COLUMN);
        if (entityTypeColumn != -1) {
            validateEntityTypes(result, tabSheet, entityTypeColumn, entityTypes);
        } else {
            result.addError("The sheet " + TAB_SHEET + " has no " + ENTITY_COLUMN + " column");
        }

        validateBooleanColumns(tabSheet, result, LEADING_COLUMN);
        validateIntegerColumns(tabSheet, result, PRIORITY_COLUMN);
        validateSecurityColumns(tabSheet, result, SECURITY_COLUMN);

        int shortnameColumn = getCellIndexFromHeaderName(tabSheet, SHORTNAME_COLUMN);
        if (shortnameColumn == -1) {
            result.addError("The sheet " + TAB_SHEET + " has no " + SHORTNAME_COLUMN + " column");
        }

        if (getCellIndexFromHeaderName(tabSheet, LABEL_COLUMN) == -1) {
            result.addError("The sheet " + TAB_SHEET + " has no " + LABEL_COLUMN + " column");
        }

        if (entityTypeColumn != -1 && shortnameColumn != -1) {
            validatePresenceInTab2BoxSheet(result, tabSheet, TAB_COLUMN, entityTypeColumn, shortnameColumn);
        }

    }

    private void validateBoxSheet(Workbook workbook, CrisLayoutToolValidationResult result, List<String> entityTypes) {

        Sheet boxSheet = workbook.getSheet(BOX_SHEET);
        if (boxSheet == null) {
            result.addError("The " + BOX_SHEET + " sheet is missing");
            return;
        }

        int typeColumn = getCellIndexFromHeaderName(boxSheet, TYPE_COLUMN);
        if (typeColumn != -1) {
            validateBoxTypes(result, boxSheet, typeColumn);
        } else {
            result.addError("The sheet " + BOX_SHEET + " has no " + TYPE_COLUMN + " column");
        }

        int entityTypeColumn = getCellIndexFromHeaderName(boxSheet, ENTITY_COLUMN);
        if (entityTypeColumn != -1) {
            validateEntityTypes(result, boxSheet, entityTypeColumn, entityTypes);
        } else {
            result.addError("The sheet " + BOX_SHEET + " has no " + ENTITY_COLUMN + " column");
        }

        validateBooleanColumns(boxSheet, result, COLLAPSED_COLUMN, CONTAINER_COLUMN, MINOR_COLUMN);
        validateSecurityColumns(boxSheet, result, SECURITY_COLUMN);

        int shortnameColumn = getCellIndexFromHeaderName(boxSheet, SHORTNAME_COLUMN);
        if (shortnameColumn == -1) {
            result.addError("The sheet " + BOX_SHEET + " has no " + SHORTNAME_COLUMN + " column");
        }

        if (entityTypeColumn != -1 && shortnameColumn != -1) {
            validatePresenceInTab2BoxSheet(result, boxSheet, BOXES_COLUMN, entityTypeColumn, shortnameColumn);
            validateDuplicateBoxes(boxSheet, result, entityTypeColumn, shortnameColumn);
        }

    }

    private void validateDuplicateBoxes(Sheet boxSheet, CrisLayoutToolValidationResult result,
                                        int entityTypeColumn, int shortnameColumn) {

        Map<String, List<String>> lowerCaseToOriginals = new HashMap<>();
        Map<String, List<Integer>> originalToRows = new HashMap<>();

        for (Row row : getNotEmptyRowsSkippingHeader(boxSheet)) {
            String entityType = getEntityTypeCellValue(row, entityTypeColumn);
            String shortname = getCellValue(row, shortnameColumn);

            if (StringUtils.isNotBlank(entityType) && StringUtils.isNotBlank(shortname)) {
                String key = (entityType + ":" + shortname).toLowerCase();
                String original = entityType + ":" + shortname;
                int excelRowNumber = row.getRowNum() + 1;

                // Track lowercase to originals mapping
                lowerCaseToOriginals.computeIfAbsent(key, k -> new ArrayList<>()).add(original);

                // Track original values to row numbers
                originalToRows.computeIfAbsent(original, k -> new ArrayList<>()).add(excelRowNumber);
            }
        }

        lowerCaseToOriginals.entrySet().stream()
                            .filter(entry -> entry.getValue().stream().distinct().count() > 1 ||
                                entry.getValue().size() > entry.getValue().stream().distinct().count())
                            .forEach(entry -> {
                                List<String> duplicateOriginals = entry.getValue().stream().distinct()
                                                                       .collect(Collectors.toList());

                                StringBuilder errorMsg = new StringBuilder();
                                errorMsg.append("Duplicate boxes detected in '")
                                        .append(BOX_SHEET).append("' sheet: ");

                                for (int i = 0; i < duplicateOriginals.size(); i++) {
                                    String original = duplicateOriginals.get(i);
                                    List<Integer> rows = originalToRows.get(original);

                                    errorMsg.append("'").append(original).append("'");
                                    if (rows.size() == 1) {
                                        errorMsg.append(" (row ").append(rows.get(0)).append(")");
                                    } else {
                                        errorMsg.append(" (rows ").append(rows).append(")");
                                    }

                                    if (i < duplicateOriginals.size() - 1) {
                                        errorMsg.append(" and ");
                                    }
                                }

                                result.addError(errorMsg.toString());
                            });
    }


    private void validateTab2BoxSheet(Workbook workbook, CrisLayoutToolValidationResult result) {

        Sheet tab2boxSheet = workbook.getSheet(TAB2BOX_SHEET);
        if (tab2boxSheet == null) {
            result.addError("The " + TAB2BOX_SHEET + " sheet is missing");
            return;
        }

        validateColumnsPresence(tab2boxSheet, result, ENTITY_COLUMN, TAB_COLUMN, BOXES_COLUMN);

        validateTab2BoxRowsReferences(tab2boxSheet, result);
        validateRowStyleColumn(tab2boxSheet, TAB_COLUMN, result);

    }

    private void validateBox2MetadataSheet(List<String> allMetadataFields,
        Workbook workbook, CrisLayoutToolValidationResult result) {

        Sheet box2metadataSheet = workbook.getSheet(BOX2METADATA_SHEET);
        if (box2metadataSheet == null) {
            result.addError("The " + BOX2METADATA_SHEET + " sheet is missing");
            return;
        }

        validateIntegerColumns(box2metadataSheet, result, ROW_COLUMN, CELL_COLUMN);
        validateBooleanColumns(box2metadataSheet, result, LABEL_AS_HEADING_COLUMN, VALUES_INLINE_COLUMN);
        validateColumnsPresence(box2metadataSheet, result, BUNDLE_COLUMN, VALUE_COLUMN);
        validateRowStyleColumn(box2metadataSheet, BOX_COLUMN, result);

        int fieldTypeColumn = getCellIndexFromHeaderName(box2metadataSheet, FIELD_TYPE_COLUMN);
        if (fieldTypeColumn == -1) {
            result.addError("The sheet " + BOX2METADATA_SHEET + " has no " + FIELD_TYPE_COLUMN + " column");
        } else {
            validateBox2MetadataFieldTypes(box2metadataSheet, result, fieldTypeColumn);
        }

        int metadataColumn = getCellIndexFromHeaderName(box2metadataSheet, METADATA_COLUMN);
        if (metadataColumn == -1) {
            result.addError("The sheet " + BOX2METADATA_SHEET + " has no " + METADATA_COLUMN + " column");
        } else {
            validateMetadataFields(allMetadataFields, box2metadataSheet, metadataColumn, fieldTypeColumn, result);
        }

        int entityTypeColumn = getCellIndexFromHeaderName(box2metadataSheet, ENTITY_COLUMN);
        if (entityTypeColumn == -1) {
            result.addError("The sheet " + BOX2METADATA_SHEET + " has no " + ENTITY_COLUMN + " column");
        }

        int boxColumn = getCellIndexFromHeaderName(box2metadataSheet, BOX_COLUMN);
        if (boxColumn == -1) {
            result.addError("The sheet " + BOX2METADATA_SHEET + " has no " + BOX_COLUMN + " column");
        }

        if (entityTypeColumn != -1 && boxColumn != -1) {
            validatePresenceInBoxSheet(result, box2metadataSheet, entityTypeColumn, boxColumn);
        }

        validateRenderingColumn(box2metadataSheet, fieldTypeColumn, result);

    }

    private void validateMetadataGroupsSheet(List<String> allMetadataFields,
        Workbook workbook, CrisLayoutToolValidationResult result) {

        Sheet metadataGroupsSheet = workbook.getSheet(METADATAGROUPS_SHEET);
        if (metadataGroupsSheet == null) {
            result.addError("The " + METADATAGROUPS_SHEET + " sheet is missing");
            return;
        }

        validateColumnsPresence(metadataGroupsSheet, result, ENTITY_COLUMN);

        int fieldTypeColumn = getCellIndexFromHeaderName(metadataGroupsSheet, FIELD_TYPE_COLUMN);

        int metadataColumn = getCellIndexFromHeaderName(metadataGroupsSheet, METADATA_COLUMN);
        if (metadataColumn == -1) {
            result.addError("The sheet " + METADATAGROUPS_SHEET + " has no " + METADATA_COLUMN + " column");
        } else {
            validateMetadataFields(allMetadataFields, metadataGroupsSheet, metadataColumn, fieldTypeColumn, result);
        }

        int parentColumn = getCellIndexFromHeaderName(metadataGroupsSheet, PARENT_COLUMN);
        if (parentColumn == -1) {
            result.addError("The sheet " + METADATAGROUPS_SHEET + " has no " + PARENT_COLUMN + " column");
        } else {
            validateMetadataFields(allMetadataFields, metadataGroupsSheet, parentColumn, fieldTypeColumn, result);
        }

        validateRenderingColumn(metadataGroupsSheet, fieldTypeColumn, result);

    }

    private void validateBoxPolicySheet(Context context, List<String> allMetadataFields, Workbook workbook,
        CrisLayoutToolValidationResult result) {
        validatePolicySheet(context, allMetadataFields, workbook, result, BOX_POLICY_SHEET);
    }

    private void validateTabPolicySheet(Context context, List<String> allMetadataFields, Workbook workbook,
        CrisLayoutToolValidationResult result) {
        validatePolicySheet(context, allMetadataFields, workbook, result, TAB_POLICY_SHEET);
    }

    private void validatePolicySheet(Context context, List<String> allMetadataFields, Workbook workbook,
        CrisLayoutToolValidationResult result, String policySheetName) {

        Sheet policySheet = workbook.getSheet(policySheetName);
        if (policySheet == null) {
            result.addError("The " + policySheetName + " sheet is missing");
            return;
        }

        int metadataColumn = getCellIndexFromHeaderName(policySheet, METADATA_COLUMN);
        if (metadataColumn == -1) {
            result.addError("The sheet " + policySheetName + " has no " + METADATA_COLUMN + " column");
        }

        int groupColumn = getCellIndexFromHeaderName(policySheet, GROUP_COLUMN);
        if (groupColumn == -1) {
            result.addError("The sheet " + policySheetName + " has no " + GROUP_COLUMN + " column");
        }

        if (metadataColumn != -1 && groupColumn != -1) {
            validateMetadataAndGroupFields(context, allMetadataFields, policySheet,
                                           metadataColumn, groupColumn, result);
        }

        validateColumnsPresence(policySheet, result, ENTITY_COLUMN, SHORTNAME_COLUMN);
    }

    private void validateBox2MetadataFieldTypes(Sheet sheet, CrisLayoutToolValidationResult result, int typeColumn) {

        int bundleColumn = getCellIndexFromHeaderName(sheet, BUNDLE_COLUMN);
        int valueColumn = getCellIndexFromHeaderName(sheet, VALUE_COLUMN);

        for (Row row : getNotEmptyRowsSkippingHeader(sheet)) {
            String fieldType = getCellValue(row, typeColumn);
            validateFieldType(row, result, bundleColumn, valueColumn, fieldType);
        }

    }

    private void validateFieldType(Row row, CrisLayoutToolValidationResult result, int bundleColumn,
        int valueColumn, String fieldType) {

        String sheetName = row.getSheet().getSheetName();
        int rowNum = row.getRowNum();

        if (StringUtils.isBlank(fieldType)) {
            result.addError("The " + sheetName + " contains an empty field type at row " + rowNum);
            return;
        }

        if (!ALLOWED_FIELD_TYPES.contains(fieldType)) {
            result.addError("The " + sheetName + " contains an unknown field type " + fieldType + " at row " + rowNum);
            return;
        }

        if (METADATA_TYPE.equals(fieldType) && bundleColumn != -1 && valueColumn != -1) {
            String bundle = getCellValue(row, bundleColumn);
            String value = getCellValue(row, valueColumn);
            if (StringUtils.isNotBlank(bundle) || StringUtils.isNotBlank(value)) {
                result.addError("The " + sheetName + " contains a " + METADATA_TYPE + " field " + fieldType +
                    " with " + BUNDLE_COLUMN + " or " + VALUE_COLUMN + " set at row " + rowNum);
            }
        }

        if (BITSTREAM_TYPE.equals(fieldType) && bundleColumn != -1) {
            String bundle = getCellValue(row, bundleColumn);
            if (StringUtils.isBlank(bundle)) {
                result.addError("The " + sheetName + " contains a " + BITSTREAM_TYPE + " field "
                    + " without " + BUNDLE_COLUMN + " at row " + rowNum);
            }
        }
    }

    private void validateMetadataFields(List<String> allMetadataFields, Sheet sheet,
        int metadataColumn, int fieldTypeColumn, CrisLayoutToolValidationResult result) {

        for (Cell cell : getColumnWithoutHeader(sheet, metadataColumn)) {

            String metadataField = WorkbookUtils.getCellValue(cell);

            if (StringUtils.isBlank(metadataField) && isNotBitstreamType(cell.getRow(), fieldTypeColumn)) {
                result.addError("The " + sheet.getSheetName() + " contains an empty metadata "
                    + "field at row " + cell.getRowIndex());
            }

            if (StringUtils.isNotBlank(metadataField) && !allMetadataFields.contains(metadataField)) {
                result.addError("The " + sheet.getSheetName() + " contains an unknown metadata field " + metadataField
                    + " at row " + cell.getRowIndex());
            }
        }

    }

    private void validateRenderingColumn(Sheet sheet, int fieldTypeColumn, CrisLayoutToolValidationResult result) {

        int renderingColumn = getCellIndexFromHeaderName(sheet, RENDERING_COLUMN);
        if (renderingColumn == -1) {
            result.addError("The sheet " + sheet.getSheetName() + " has no " + RENDERING_COLUMN + " column");
            return;
        }

        for (Cell cell : getColumnWithoutHeader(sheet, renderingColumn)) {
            if (WorkbookUtils.isCellNotEmpty(cell)) {
                validateRenderType(cell, fieldTypeColumn, result);
            }
        }

    }

    private void validateRenderType(Cell cell, int fieldTypeColumn, CrisLayoutToolValidationResult result) {

        String renderType = WorkbookUtils.getCellValue(cell);

        String fieldType = getCellValue(cell.getRow(), fieldTypeColumn);
        if (StringUtils.isEmpty(fieldType)) {
            fieldType = CrisLayoutBoxTypes.METADATA.name();
        }

        String sheetName = cell.getRow().getSheet().getSheetName();

        CrisLayoutToolRenderValidator renderValidator = findRenderValidatorByName(renderType);

        if (renderValidator == null) {
            result.addError("The sheet " + sheetName + " contains an unknown RENDERING type " + renderType + " at row "
                + cell.getRow().getRowNum());
            return;
        }

        renderValidator.validate(renderType, fieldType)
            .ifPresent(validationError -> result.addError("The sheet " + sheetName + " contains an invalid "
                + "RENDERING type at row " + cell.getRow().getRowNum() + ": " + validationError));

    }

    private CrisLayoutToolRenderValidator findRenderValidatorByName(String renderType) {
        String renderName = renderType.split("\\.")[0];
        return renderingValidatorsByName.get(renderName);
    }

    private void validateMetadataAndGroupFields(Context context, List<String> allMetadataFields, Sheet sheet,
                                                int metadataColumn, int groupColumn,
                                                CrisLayoutToolValidationResult result) {
        List<Cell> metadataCells = getColumnWithoutHeader(sheet, metadataColumn);
        List<Cell> groupCells = getColumnWithoutHeader(sheet, groupColumn);

        // Only METADATA or GROUP column must have a value for each row
        for (int i = 0; i < metadataCells.size(); i++) {
            String metadataValue = getCellValue(metadataCells.get(i));
            String groupValue = getCellValue(groupCells.get(i));

            if (StringUtils.isBlank(metadataValue) == StringUtils.isBlank(groupValue)) {
                result.addError("The " + sheet.getSheetName() + " at row " + i
                    + " contains invalid values for METADATA/GROUP column.");
                return;
            }

            if (StringUtils.isNotBlank(metadataValue) && !allMetadataFields.contains(metadataValue)) {
                result.addError("The " + sheet.getSheetName() + " contains an unknown metadata field: '" + metadataValue
                                    + "' at row " + i);
            }

            if (StringUtils.isNotBlank(groupValue) && !doesGroupExists(context, groupValue)) {
                result.addError("The " + sheet.getSheetName() + " contains an unknown group field: '" + groupValue
                                    + "' at row " + i);
            }
        }
    }

    private boolean isNotBitstreamType(Row row, int fieldTypeColumn) {
        if (fieldTypeColumn == -1) {
            return true;
        }

        return !BITSTREAM_TYPE.equals(getCellValue(row, fieldTypeColumn));
    }

    private void validatePresenceInBoxSheet(CrisLayoutToolValidationResult result, Sheet sheet,
        int entityTypeColumn, int nameColumn) {

        for (Row row : getNotEmptyRowsSkippingHeader(sheet)) {
            String entityType = getEntityTypeCellValue(row, entityTypeColumn);
            String name = getCellValue(row, nameColumn);
            if (isNotPresentOnSheet(sheet.getWorkbook(), BOX_SHEET, entityType, name)) {
                result.addError("The box with name " + name +
                    " and entity type " + entityType + " in the row "
                    + row.getRowNum() + " of sheet " + sheet.getSheetName()
                    + " is not present in the " + BOX_SHEET + " sheet");
            }
        }

    }

    private void validateTab2BoxRowsReferences(Sheet tab2boxSheet, CrisLayoutToolValidationResult result) {

        int entityTypeColumn = getCellIndexFromHeaderName(tab2boxSheet, ENTITY_COLUMN);
        int tabColumn = getCellIndexFromHeaderName(tab2boxSheet, TAB_COLUMN);
        int boxesColumn = getCellIndexFromHeaderName(tab2boxSheet, BOXES_COLUMN);

        if (entityTypeColumn != -1 && tabColumn != -1 && boxesColumn != -1) {
            getNotEmptyRowsSkippingHeader(tab2boxSheet)
                .forEach(row -> validateTab2BoxRowReferences(row, result, entityTypeColumn, tabColumn, boxesColumn));
        }

    }

    private void validatePresenceInTab2BoxSheet(CrisLayoutToolValidationResult result, Sheet sheet, String columnName,
        int entityTypeColumn, int shortnameColumn) {

        Sheet tab2boxSheet = sheet.getWorkbook().getSheet(TAB2BOX_SHEET);
        if (tab2boxSheet == null) {
            return;
        }

        for (Row row : getNotEmptyRowsSkippingHeader(sheet)) {
            String entityType = getEntityTypeCellValue(row, entityTypeColumn);
            String shortname = getCellValue(row, shortnameColumn);
            if (isNotPresentOnTab2Box(tab2boxSheet, columnName, entityType, shortname)) {
                result.addWarning("The " + sheet.getSheetName() + " with name " + shortname +
                    " and entity type " + entityType + " in the row "
                    + row.getRowNum() + " of sheet " + sheet.getSheetName()
                    + " is not present in the " + TAB2BOX_SHEET + " sheet");
            }
        }

    }

    private void validateTab2BoxRowReferences(Row row, CrisLayoutToolValidationResult result,
        int entityTypeColumn, int tabColumn, int boxesColumn) {

        Sheet tab2boxSheet = row.getSheet();

        String entityType = getEntityTypeCellValue(row, entityTypeColumn);
        String tab = getCellValue(row, tabColumn);
        String[] boxes = splitByCommaAndTrim(getCellValue(row, boxesColumn));

        if (isNotPresentOnSheet(tab2boxSheet.getWorkbook(), TAB_SHEET, entityType, tab)) {
            result.addError("The Tab with name " + tab + " and entity type " + entityType + " in the row " +
                row.getRowNum() + " of sheet " + tab2boxSheet.getSheetName() + " is not present in the " + TAB_SHEET
                + " sheet");
        }

        for (String box : boxes) {
            if (isNotPresentOnSheet(tab2boxSheet.getWorkbook(), BOX_SHEET, entityType, box)) {
                result.addError("The Box with name " + box + " and entity type " + entityType + " in the row " +
                    row.getRowNum() + " of sheet " + tab2boxSheet.getSheetName() + " is not present in the " + BOX_SHEET
                    + " sheet");
            }
        }

    }

    private void validateRowStyleColumn(Sheet sheet, String containerColumnName,
        CrisLayoutToolValidationResult result) {

        int rowStyleColumn = getCellIndexFromHeaderName(sheet, ROW_STYLE_COLUMN);
        if (rowStyleColumn == -1) {
            result.addError("The sheet " + sheet.getSheetName() + " has no " + ROW_STYLE_COLUMN + " column");
            return;
        }

        int rowColumn = getCellIndexFromHeaderName(sheet, ROW_COLUMN);
        if (rowColumn == -1) {
            return;
        }

        int entityTypeColumn = getCellIndexFromHeaderName(sheet, ENTITY_COLUMN);
        int containerColumn = getCellIndexFromHeaderName(sheet, containerColumnName);
        if (entityTypeColumn == -1 || containerColumn == -1) {
            return;
        }

        List<Integer> detectedRowWithConflicts = new ArrayList<Integer>();

        for (Row row : getNotEmptyRowsSkippingHeader(sheet)) {

            if (detectedRowWithConflicts.contains(row.getRowNum())) {
                continue;
            }

            String style = getCellValue(row, rowStyleColumn);
            if (StringUtils.isBlank(style)) {
                continue;
            }

            String entityType = getEntityTypeCellValue(row, entityTypeColumn);
            String container = getCellValue(row, containerColumn);
            String rowCount = getCellValue(row, rowColumn);

            List<Integer> sameRowsWithDifferentStyle = findSameRowsWithDifferentStyle(sheet,
                entityType, container, containerColumn, rowCount, style, row.getRowNum());

            if (CollectionUtils.isNotEmpty(sameRowsWithDifferentStyle)) {
                detectedRowWithConflicts.addAll(sameRowsWithDifferentStyle);
                result.addError("Row style conflict between rows " + row.getRowNum() + " and rows "
                    + sameRowsWithDifferentStyle.toString() + " of sheet " + sheet.getSheetName());
            }

        }
    }

    private List<Integer> findSameRowsWithDifferentStyle(Sheet sheet, String entity,
        String container, int containerColumn, String row, String style, int excelRowNum) {
        int rowStyleColumn = getCellIndexFromHeaderName(sheet, ROW_STYLE_COLUMN);
        int entityTypeColumn = getCellIndexFromHeaderName(sheet, ENTITY_COLUMN);
        int rowColumn = getCellIndexFromHeaderName(sheet, ROW_COLUMN);
        return getNotEmptyRowsSkippingHeader(sheet).stream()
            .filter(sheetRow -> excelRowNum != sheetRow.getRowNum())
            .filter(sheetRow -> row.equals(getCellValue(sheetRow, rowColumn)))
            .filter(sheetRow -> container.equals(getCellValue(sheetRow, containerColumn)))
            .filter(sheetRow -> entity.equals(getEntityTypeCellValue(sheetRow, entityTypeColumn)))
            .filter(sheetRow -> hasDifferentStyle(sheetRow, rowStyleColumn, style))
            .map(Row::getRowNum)
            .collect(Collectors.toList());
    }

    private boolean hasDifferentStyle(Row row, int rowStyleColumn, String style) {
        String cellValue = getCellValue(row, rowStyleColumn);
        return isNotBlank(cellValue) && !style.trim().equals(cellValue.trim());
    }

    private boolean isNotPresentOnSheet(Workbook workbook, String sheetName, String entityType, String shortname) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            // Return false to avoid many validation error if the sheet is missing
            return false;
        }

        int entityTypeColumn = getCellIndexFromHeaderName(sheet, ENTITY_COLUMN);
        int shortnameColumn = getCellIndexFromHeaderName(sheet, SHORTNAME_COLUMN);
        if (entityTypeColumn == -1 || shortnameColumn == -1) {
            // Return false to avoid many validation error if one of the two columns is missing
            return false;
        }

        return getNotEmptyRowsSkippingHeader(sheet).stream()
            .noneMatch(row -> sameEntityTypeAndName(row, entityTypeColumn, entityType, shortnameColumn, shortname));
    }

    private boolean isNotPresentOnTab2Box(Sheet tab2boxSheet, String columnName, String entityType, String shortname) {

        int entityTypeColumn = getCellIndexFromHeaderName(tab2boxSheet, ENTITY_COLUMN);
        int nameColumn = getCellIndexFromHeaderName(tab2boxSheet, columnName);
        if (entityTypeColumn == -1 || nameColumn == -1) {
            // Return false to avoid many validation error if one of the two columns is
            // missing
            return false;
        }

        return getNotEmptyRowsSkippingHeader(tab2boxSheet).stream()
            .noneMatch(row -> sameEntityTypeAndName(row, entityTypeColumn, entityType, nameColumn, shortname));

    }

    private boolean sameEntityTypeAndName(Row row, int entityTypeColumn, String entityType,
        int nameColumn, String name) {

        String[] namesOnColumn = splitByCommaAndTrim(getCellValue(row, nameColumn));
        return entityType.equals(getEntityTypeCellValue(row, entityTypeColumn))
                && ArrayUtils.contains(namesOnColumn, name);

    }

    private void validateEntityTypes(CrisLayoutToolValidationResult result, Sheet sheet,
        int entityColumn, List<String> allEntityTypes) {

        for (Cell entityTypeCell : getColumnWithoutHeader(sheet, entityColumn)) {
            String entityType = getCellValue(entityTypeCell);
            if (
                    !allEntityTypes.contains(entityType) &&
                    !allEntityTypes.contains(getEntityTypeValue(entityTypeCell))
            ) {
                result.addError("The " + sheet.getSheetName() + " contains an unknown entity type '" + entityType
                    + "' at row " + entityTypeCell.getRowIndex());
            }
        }
    }

    private void validateBoxTypes(CrisLayoutToolValidationResult result, Sheet sheet, int typeColumn) {

        for (Cell typeCell : getColumnWithoutHeader(sheet, typeColumn)) {
            String type = WorkbookUtils.getCellValue(typeCell);
            if (StringUtils.isNotBlank(type) && !EnumUtils.isValidEnum(CrisLayoutBoxTypes.class, type)) {
                result.addError("The sheet " + sheet.getSheetName() + " contains an invalid type " + type
                    + " at row " + typeCell.getRowIndex());
            }
        }

    }

    private void validateColumnsPresence(Sheet sheet, CrisLayoutToolValidationResult result, String... columns) {
        for (String column : columns) {
            if (getCellIndexFromHeaderName(sheet, column) == -1) {
                result.addError("The sheet " + sheet.getSheetName() + " has no " + column + " column");
            }
        }
    }

    private void validateBooleanColumns(Sheet sheet, CrisLayoutToolValidationResult result, String... columnNames) {
        for (String columnName : columnNames) {
            validateColumn(sheet, columnName, (value) -> isNotBoolean(value), result,
                ALLOWED_BOOLEAN_VALUES.toString());
        }
    }

    private void validateIntegerColumns(Sheet sheet, CrisLayoutToolValidationResult result, String... columnNames) {
        for (String columnName : columnNames) {
            validateColumn(sheet, columnName, (value) -> isNotInteger(value), result, "integer values");
        }
    }

    private void validateSecurityColumns(Sheet sheet, CrisLayoutToolValidationResult result, String... columnNames) {
        for (String columnName : columnNames) {
            validateColumn(sheet, columnName, (value) -> !ALLOWED_SECURITY_VALUES.contains(value), result,
                ALLOWED_SECURITY_VALUES.toString());
        }
    }

    private void validateColumn(Sheet sheet, String columnName, Predicate<String> predicate,
        CrisLayoutToolValidationResult result, String allowedValues) {

        int rowColumn = getCellIndexFromHeaderName(sheet, columnName);
        if (rowColumn == -1) {
            result.addError("The sheet " + sheet.getSheetName() + " has no " + columnName + " column");
            return;
        }

        for (Row row : getNotEmptyRowsSkippingHeader(sheet)) {
            String rowValue = getCellValue(row, rowColumn);
            if (StringUtils.isBlank(rowValue)) {
                result.addError("The " + columnName + " value specified on the row " + row.getRowNum() + " of sheet "
                    + sheet.getSheetName() + " is empty. Allowed values: " + allowedValues);
            } else if (predicate.test(rowValue)) {
                result.addError("The " + columnName + " value specified on the row " + row.getRowNum() + " of sheet "
                    + sheet.getSheetName() + " is not valid: " + rowValue + ". Allowed values: " + allowedValues);
            }
        }
    }

    private List<String> getAllEntityTypes(Context context) {
        try {
            return entityTypeService.findAll(context).stream()
                .peek(entityType -> uncacheEntity(context, entityType))
                .map(entityType -> entityType.getLabel())
                .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<String> getAllMetadataFields(Context context) {
        try {
            return metadataFieldService.findAll(context).stream()
                .peek(metadataField -> uncacheEntity(context, metadataField))
                .map(metadataField -> metadataField.toString('.'))
                .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private void uncacheEntity(Context context, ReloadableEntity<?> object) {
        try {
            context.uncacheEntity(object);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isNotInteger(String number) {
        try {
            Integer.parseInt(number);
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    private boolean isNotBoolean(String value) {
        return !ALLOWED_BOOLEAN_VALUES.contains(value);
    }

    private String[] splitByCommaAndTrim(String name) {
        return Arrays.stream(name.split(",")).map(String::trim).toArray(String[]::new);
    }

    private boolean doesGroupExists(Context context, String groupName) {
        try {
            return groupService.findByName(context, groupName) != null;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

}
