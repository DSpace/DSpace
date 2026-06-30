/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service.impl;

import static org.dspace.layout.script.service.CrisLayoutToolValidator.ALTERNATIVE_TO_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BITSTREAM_TYPE;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX2METADATA_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOXES_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX_POLICY_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BUNDLE_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.CELL_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.CELL_STYLE_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.COLLAPSED_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.CONTAINER_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.ENTITY_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.FIELD_TYPE_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.GROUP_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.LABEL_AS_HEADING_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.LABEL_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.LEADING_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.METADATAGROUPS_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.METADATAGROUP_TYPE;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.METADATA_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.METADATA_TYPE;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.MINOR_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.PARENT_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.PRIORITY_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.RENDERING_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.ROW_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.ROW_STYLE_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.SECURITY_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.SHORTNAME_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.STYLE_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.STYLE_LABEL_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.STYLE_VALUE_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB2BOX_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB_POLICY_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TYPE_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.VALUES_INLINE_COLUMN;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.VALUE_COLUMN;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBox2SecurityGroup;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutCell;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisLayoutFieldMetadata;
import org.dspace.layout.CrisLayoutRow;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisLayoutTab2SecurityGroup;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.script.service.CrisLayoutToolParser;
import org.dspace.util.WorkbookUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link CrisLayoutToolParser}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutToolParserImpl implements CrisLayoutToolParser {

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private GroupService groupService;

    @Override
    public List<CrisLayoutTab> parse(Context context, Workbook workbook) {
        Sheet tabSheet = getSheetByName(workbook, TAB_SHEET);
        List<CrisLayoutTab> tabs =
            WorkbookUtils.getNotEmptyRowsSkippingHeader(tabSheet).stream()
                         .map(row -> buildTab(context, row))
                         .collect(Collectors.toList());

        tabs.forEach(tab -> {
            tab.setTab2SecurityGroups(buildTab2SecurityGroups(context,
                workbook, TAB_POLICY_SHEET, tab.getEntity().getLabel(), tab.getShortName(), tab, tabs));

            tab.getBoxes().forEach(box ->
                box.setBox2SecurityGroups(buildBox2SecurityGroups(context,
                    workbook, BOX_POLICY_SHEET, box.getEntitytype().getLabel(), box.getShortname(), box, tabs)));
        });

        return  tabs;
    }

    private CrisLayoutTab buildTab(Context context, Row tabRow) {
        CrisLayoutTab tab = new CrisLayoutTab();

        Workbook workbook = tabRow.getSheet().getWorkbook();
        String name = getCellValue(tabRow, SHORTNAME_COLUMN);
        String entityColumn = getCellValue(tabRow, ENTITY_COLUMN);

        int index = entityColumn.indexOf(".");
        String customFilter = (index > 0 && index < entityColumn.length()) ? entityColumn.substring(index + 1) : null;
        String entityType = (index > 0) ? entityColumn.substring(0, index) : entityColumn;

        tab.setEntity(getEntityType(context, entityType));
        tab.setCustomFilter(customFilter);
        tab.setShortName(name);
        tab.setHeader(getCellValue(tabRow, LABEL_COLUMN));
        tab.setLeading(toBoolean(getCellValue(tabRow, LEADING_COLUMN)));
        tab.setPriority(toInteger(getCellValue(tabRow, PRIORITY_COLUMN)));
        tab.setSecurity(toSecurity(getCellValue(tabRow, SECURITY_COLUMN)));
        buildTabRows(context, workbook, entityType, name).forEach(tab::addRow);
        tab.setMetadataSecurityFields(buildMetadataSecurityField(context, workbook,
            TAB_POLICY_SHEET, entityType, name));

        return tab;
    }

    private List<CrisLayoutRow> buildTabRows(Context context, Workbook workbook, String entityType, String shortname) {
        Sheet tab2boxSheet = getSheetByName(workbook, TAB2BOX_SHEET);

        Map<Integer, List<Row>> groupSheetRowsByRowColumn =
            getRowsByEntityAndColumnValue(tab2boxSheet, entityType, TAB_COLUMN, shortname)
            .collect(Collectors.groupingBy(row -> toInteger(getCellValue(row, ROW_COLUMN))));

        return groupSheetRowsByRowColumn.keySet().stream().sorted()
            .map(rowIndex -> buildTabRow(context, groupSheetRowsByRowColumn.get(rowIndex)))
            .collect(Collectors.toList());

    }

    private CrisLayoutRow buildTabRow(Context context, List<Row> tab2boxRows) {

        CrisLayoutRow row = new CrisLayoutRow();
        getFirstNotEmptyRowStyle(tab2boxRows).ifPresent(row::setStyle);

        tab2boxRows.stream()
            .sorted(Comparator.comparing(Row::getRowNum))
            .forEach(tab2boxRow -> row.addCell(buildCell(context, tab2boxRow)));

        return row;

    }

    private CrisLayoutCell buildCell(Context context, Row tab2boxRow) {
        CrisLayoutCell cell = new CrisLayoutCell();
        cell.setStyle(getCellValue(tab2boxRow, CELL_STYLE_COLUMN));
        buildBoxes(context, tab2boxRow).forEach(cell::addBox);
        return cell;
    }

    private List<CrisLayoutBox> buildBoxes(Context context, Row tab2boxRow) {

        String entityType = getEntityValue(tab2boxRow, ENTITY_COLUMN);

        String boxes = getCellValue(tab2boxRow, BOXES_COLUMN);
        if (StringUtils.isBlank(boxes)) {
            throw new IllegalArgumentException("The row " + tab2boxRow.getRowNum() + " of sheet "
                + tab2boxRow.getSheet().getSheetName() + " has no " + BOXES_COLUMN);
        }

        Sheet boxSheet = getSheetByName(tab2boxRow.getSheet().getWorkbook(), BOX_SHEET);

        return Arrays.stream(boxes.split(","))
            .map(String::trim)
            .map(box -> buildBox(context, boxSheet, entityType, box))
            .collect(Collectors.toList());
    }

    private CrisLayoutBox buildBox(Context context, Sheet boxSheet, String entityType, String boxName) {

        Workbook workbook = boxSheet.getWorkbook();
        Row boxRow = getBowRowFromBoxSheet(boxSheet, entityType, boxName);

        CrisLayoutBox box = new CrisLayoutBox();

        String boxType = getCellValue(boxRow, TYPE_COLUMN);
        if (StringUtils.isBlank(boxType)) {
            boxType = CrisLayoutBoxTypes.METADATA.name();
        } else {
            boxType = boxType.toUpperCase();
        }

        box.setType(boxType);
        box.setCollapsed(toBoolean(getCellValue(boxRow, COLLAPSED_COLUMN)));
        box.setContainer(toBoolean(getCellValue(boxRow, CONTAINER_COLUMN)));
        box.setEntitytype(getEntityType(context, entityType));
        box.setHeader(getCellValue(boxRow, LABEL_COLUMN));
        box.setMinor(toBoolean(getCellValue(boxRow, MINOR_COLUMN)));
        box.setSecurity(toSecurity(getCellValue(boxRow, SECURITY_COLUMN)));
        box.setShortname(boxName);
        box.setStyle(getCellValue(boxRow, STYLE_COLUMN));
        box.setMetadataSecurityFields(buildMetadataSecurityField(context, workbook,
            BOX_POLICY_SHEET, entityType, boxName));

        if (boxType.equals(CrisLayoutBoxTypes.METADATA.name())) {
            buildCrisLayoutFields(context, workbook, entityType, boxName).forEach(box::addLayoutField);
        }

        return box;
    }

    private List<CrisLayoutField> buildCrisLayoutFields(Context context, Workbook workbook, String entityType,
        String boxName) {

        Sheet box2metadataSheet = getSheetByName(workbook, BOX2METADATA_SHEET);

        AtomicInteger priority = new AtomicInteger(0);
        return getRowsByEntityAndColumnValue(box2metadataSheet, entityType, BOX_COLUMN, boxName)
            .map(row -> buildCrisLayoutField(context, row, priority))
            .collect(Collectors.toList());
    }

    private CrisLayoutField buildCrisLayoutField(Context context, Row row, AtomicInteger priority) {

        CrisLayoutField field = buildCrisLayoutFieldByType(context, row);

        field.setLabel(getCellValue(row, LABEL_COLUMN));
        field.setLabelAsHeading(toBoolean(getCellValue(row, LABEL_AS_HEADING_COLUMN)));
        String metadataField = getCellValue(row, METADATA_COLUMN);
        if (StringUtils.isNotBlank(metadataField)) {
            field.setMetadataField(getMetadataField(context, metadataField));
        }
        field.setPriority(priority.getAndIncrement());
        field.setRendering(getCellValue(row, RENDERING_COLUMN));
        field.setRow(toInteger(getCellValue(row, ROW_COLUMN)));
        field.setCell(toInteger(getCellValue(row, CELL_COLUMN)));
        field.setRowStyle(getCellValue(row, ROW_STYLE_COLUMN));
        field.setCellStyle(getCellValue(row, CELL_STYLE_COLUMN));
        field.setStyleLabel(getCellValue(row, STYLE_LABEL_COLUMN));
        field.setStyleValue(getCellValue(row, STYLE_VALUE_COLUMN));
        field.setValuesInline(toBoolean(getCellValue(row, VALUES_INLINE_COLUMN)));

        return field;
    }

    private CrisLayoutField buildCrisLayoutFieldByType(Context context, Row row) {
        String fieldType = getCellValue(row, FIELD_TYPE_COLUMN);

        switch (fieldType) {
            case METADATAGROUP_TYPE:
                return buildMetadataGroupField(context, row);
            case BITSTREAM_TYPE:
                return buildBitstreamField(row);
            case METADATA_TYPE:
            default:
                return new CrisLayoutFieldMetadata();
        }
    }

    private CrisLayoutField buildMetadataGroupField(Context context, Row row) {
        CrisLayoutFieldMetadata field = new CrisLayoutFieldMetadata();
        buildCrisMetadataGroups(context, row).forEach(field::addCrisMetadataGroupList);
        return field;
    }

    private List<CrisMetadataGroup> buildCrisMetadataGroups(Context context, Row row) {
        String metadataField = getCellValue(row, METADATA_COLUMN);
        String entity = getEntityValue(row, ENTITY_COLUMN);

        Sheet metadatagroupsSheet = getSheetByName(row.getSheet().getWorkbook(), METADATAGROUPS_SHEET);

        AtomicInteger priority = new AtomicInteger(0);
        return getRowsByEntityAndColumnValue(metadatagroupsSheet, entity, PARENT_COLUMN, metadataField)
            .map(groupRow -> buildCrisMetadataGroup(context, groupRow, entity, priority))
            .collect(Collectors.toList());
    }

    private CrisMetadataGroup buildCrisMetadataGroup(Context context, Row row, String entity, AtomicInteger priority) {
        CrisMetadataGroup metadataGroup = new CrisMetadataGroup();
        metadataGroup.setLabel(getCellValue(row, LABEL_COLUMN));
        String metadataField = getCellValue(row, METADATA_COLUMN);
        if (StringUtils.isNotBlank(metadataField)) {
            metadataGroup.setMetadataField(getMetadataField(context, metadataField));
        }
        metadataGroup.setPriority(priority.getAndIncrement());
        metadataGroup.setRendering(getCellValue(row, RENDERING_COLUMN));
        metadataGroup.setStyleLabel(getCellValue(row, STYLE_LABEL_COLUMN));
        metadataGroup.setStyleValue(getCellValue(row, STYLE_VALUE_COLUMN));
        return metadataGroup;
    }

    private CrisLayoutField buildBitstreamField(Row row) {
        CrisLayoutFieldBitstream field = new CrisLayoutFieldBitstream();
        field.setBundle(getCellValue(row, BUNDLE_COLUMN));
        field.setMetadataValue(getCellValue(row, VALUE_COLUMN));
        return field;
    }

    private Row getBowRowFromBoxSheet(Sheet boxSheet, String entity, String boxName) {
        return getRowsByEntityAndColumnValue(boxSheet, entity, SHORTNAME_COLUMN, boxName)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No box found with entity "
                + entity + " and name " + boxName));
    }

    private Optional<String> getFirstNotEmptyRowStyle(List<Row> tab2boxRows) {
        return tab2boxRows.stream()
            .map(tab2boxRow -> getCellValue(tab2boxRow, ROW_STYLE_COLUMN))
            .filter(style -> StringUtils.isNotBlank(style))
            .findFirst();
    }

    private Set<MetadataField> buildMetadataSecurityField(Context context, Workbook workbook,
        String sheetName, String entity, String name) {

        Sheet sheet = getSheetByName(workbook, sheetName);

        return getRowsByEntityAndColumnValue(sheet, entity, SHORTNAME_COLUMN, name)
            .map(row -> getCellValue(row, METADATA_COLUMN))
            .filter(StringUtils::isNotBlank)
            .map(metadataField -> getMetadataField(context, metadataField))
            .collect(Collectors.toSet());

    }

    private Set<Group> buildGroupSecurityField(Context context, Workbook workbook,
                                               String sheetName, String entity, String name) {
        Sheet sheet = getSheetByName(workbook, sheetName);

        return getRowsByEntityAndColumnValue(sheet, entity, SHORTNAME_COLUMN, name)
            .map(row -> getCellValue(row, GROUP_COLUMN))
            .filter(StringUtils::isNotBlank)
            .map(groupField -> getGroupField(context, groupField))
            .collect(Collectors.toSet());
    }

    private Set<CrisLayoutBox2SecurityGroup> buildBox2SecurityGroups(Context context, Workbook workbook,
                                                                     String sheetName, String entity, String name,
                                                                     CrisLayoutBox crisLayoutBox,
                                                                     List<CrisLayoutTab> tabs) {
        Sheet sheet = getSheetByName(workbook, sheetName);
        Set<CrisLayoutBox2SecurityGroup> box2SecurityGroups = new HashSet<>();

        getRowsByEntityAndColumnValue(sheet, entity, SHORTNAME_COLUMN, name)
            .forEach(row -> {
                String groupName = getCellValue(row, GROUP_COLUMN);
                String alternativeBox = getCellValue(row, ALTERNATIVE_TO_COLUMN);

                if (StringUtils.isNotBlank(groupName)) {
                    Group group = getGroupField(context, groupName);
                    if (group != null) {
                        box2SecurityGroups.add(
                            buildBox2SecurityGroup(group, crisLayoutBox, entity, alternativeBox, tabs)
                        );
                    }
                }
            });

        return box2SecurityGroups;
    }

    private CrisLayoutBox2SecurityGroup buildBox2SecurityGroup(Group group, CrisLayoutBox box,
                                                               String entity,
                                                               String alternativeBox, List<CrisLayoutTab> tabs) {

        CrisLayoutBox2SecurityGroup.CrisLayoutBox2SecurityGroupId box2SecurityGroupId =
            new CrisLayoutBox2SecurityGroup.CrisLayoutBox2SecurityGroupId(box, group);

        return new CrisLayoutBox2SecurityGroup(box2SecurityGroupId, box, group,
            findAlternativeBox(alternativeBox, entity, tabs));
    }

    private CrisLayoutBox findAlternativeBox(String alternativeBox, String entityType, List<CrisLayoutTab> tabs) {

        if (alternativeBox == null) {
            return null;
        }

        return tabs.stream()
                   .flatMap(tab -> tab.getBoxes().stream())
                   .filter(crisLayoutBox -> crisLayoutBox.getShortname().equals(alternativeBox) &&
                       crisLayoutBox.getEntitytype().getLabel().equals(entityType))
                   .findFirst()
                   .orElseThrow(() -> new RuntimeException("Alternative box not found for shortname: " +
                       alternativeBox + ", entityType: " + entityType));
    }

    private Set<CrisLayoutTab2SecurityGroup> buildTab2SecurityGroups(Context context, Workbook workbook,
                                                                     String sheetName, String entity, String name,
                                                                     CrisLayoutTab crisLayoutTab,
                                                                     List<CrisLayoutTab> tabs) {
        Sheet sheet = getSheetByName(workbook, sheetName);
        Set<CrisLayoutTab2SecurityGroup> tab2SecurityGroups = new HashSet<>();

        getRowsByEntityAndColumnValue(sheet, entity, SHORTNAME_COLUMN, name)
            .forEach(row -> {
                String groupName = getCellValue(row, GROUP_COLUMN);
                String alternativeTab = getCellValue(row, ALTERNATIVE_TO_COLUMN);

                if (StringUtils.isNotBlank(groupName)) {
                    Group group = getGroupField(context, groupName);
                    if (group != null) {
                        tab2SecurityGroups.add(
                            buildTab2SecurityGroup(group, crisLayoutTab, entity, alternativeTab, tabs)
                        );
                    }
                }
            });

        return tab2SecurityGroups;
    }

    private CrisLayoutTab2SecurityGroup buildTab2SecurityGroup(Group group, CrisLayoutTab tab,
                                                               String entity,
                                                               String alternativeTab, List<CrisLayoutTab> tabs) {

        CrisLayoutTab2SecurityGroup.CrisLayoutTab2SecurityGroupId tab2SecurityGroupId =
            new CrisLayoutTab2SecurityGroup.CrisLayoutTab2SecurityGroupId(tab, group);

        return new CrisLayoutTab2SecurityGroup(tab2SecurityGroupId, tab, group,
            findAlternativeTab(alternativeTab, entity, tabs));
    }

    private CrisLayoutTab findAlternativeTab(String alternativeTab, String entityType, List<CrisLayoutTab> tabs) {

        if (alternativeTab == null) {
            return null;
        }

        return tabs.stream()
                   .filter(crisLayoutTab -> crisLayoutTab.getShortName().equals(alternativeTab) &&
                       crisLayoutTab.getEntity().getLabel().equals(entityType))
                   .findFirst()
                   .orElseThrow(() -> new RuntimeException("Alternative tab not found for shortname: " +
                       alternativeTab + ", entityType: " + entityType));
    }

    private Stream<Row> getRowsByEntityAndColumnValue(Sheet sheet, String entity, String columnName, String value) {
        return WorkbookUtils.getNotEmptyRowsSkippingHeader(sheet).stream()
            .filter(row -> value.equals(getCellValue(row, columnName)))
            .filter(row -> entity.equals(getEntityValue(row, ENTITY_COLUMN)));
    }

    private boolean toBoolean(String value) {
        return BooleanUtils.toBoolean(value);
    }

    private Integer toInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer value: " + value);
        }
    }

    private String getEntityValue(Row row, String header) {
        String cellValue = WorkbookUtils.getCellValue(row, header);
        return Optional.ofNullable(cellValue)
                       .filter(cell -> cell.contains("."))
                       .map(cell -> cell.split("\\.")[0])
                       .orElse(StringUtils.isNotBlank(cellValue) ? cellValue : null);
    }

    private String getCellValue(Row row, String header) {
        String cellValue = WorkbookUtils.getCellValue(row, header);
        return StringUtils.isNotBlank(cellValue) ? cellValue : null;
    }

    private Integer toSecurity(String cellValue) {
        String securityValue = cellValue.trim().toUpperCase().replaceAll(" ", "_").replaceAll("&", "AND");
        if (!EnumUtils.isValidEnum(LayoutSecurity.class, securityValue)) {
            throw new IllegalArgumentException("Invalid security value: " + securityValue);
        }
        return LayoutSecurity.valueOf(securityValue).getValue();
    }

    private Sheet getSheetByName(Workbook workbook, String name) {
        Sheet tabSheet = workbook.getSheet(name);
        if (tabSheet == null) {
            throw new IllegalArgumentException("The given workbook has not the " + name + " sheet");
        }
        return tabSheet;
    }

    private MetadataField getMetadataField(Context context, String metadataSecurityField) {
        try {
            return metadataFieldService.findByString(context, metadataSecurityField, '.');
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private Group getGroupField(Context context, String groupName) {
        try {
            return groupService.findByName(context, groupName);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private EntityType getEntityType(Context context, String entityType) {
        try {
            return entityTypeService.findByEntityType(context, entityType);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

}
