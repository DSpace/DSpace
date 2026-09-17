/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service.impl;

import static org.dspace.layout.script.service.DynamicLayoutToolValidator.ALTERNATIVE_TO_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.BITSTREAM_TYPE;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.BOX2METADATA_SHEET;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.BOXES_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.BOX_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.BOX_POLICY_SHEET;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.BOX_SHEET;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.BUNDLE_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.CELL_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.CELL_STYLE_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.COLLAPSED_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.CONTAINER_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.ENTITY_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.FIELD_TYPE_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.GROUP_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.LABEL_AS_HEADING_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.LABEL_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.LEADING_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.METADATAGROUPS_SHEET;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.METADATAGROUP_TYPE;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.METADATA_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.METADATA_TYPE;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.MINOR_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.PARENT_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.PRIORITY_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.RENDERING_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.ROW_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.ROW_STYLE_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.SECURITY_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.SHORTNAME_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.STYLE_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.STYLE_LABEL_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.STYLE_VALUE_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.TAB2BOX_SHEET;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.TAB_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.TAB_POLICY_SHEET;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.TAB_SHEET;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.TYPE_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.VALUES_INLINE_COLUMN;
import static org.dspace.layout.script.service.DynamicLayoutToolValidator.VALUE_COLUMN;

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
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBox2SecurityGroup;
import org.dspace.layout.DynamicLayoutBoxTypes;
import org.dspace.layout.DynamicLayoutCell;
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.DynamicLayoutFieldBitstream;
import org.dspace.layout.DynamicLayoutFieldMetadata;
import org.dspace.layout.DynamicLayoutRow;
import org.dspace.layout.DynamicLayoutTab;
import org.dspace.layout.DynamicLayoutTab2SecurityGroup;
import org.dspace.layout.DynamicMetadataGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.script.service.DynamicLayoutToolParser;
import org.dspace.util.WorkbookUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link DynamicLayoutToolParser}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DynamicLayoutToolParserImpl implements DynamicLayoutToolParser {

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private GroupService groupService;

    @Override
    public List<DynamicLayoutTab> parse(Context context, Workbook workbook) {
        Sheet tabSheet = getSheetByName(workbook, TAB_SHEET);
        List<DynamicLayoutTab> tabs =
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

    private DynamicLayoutTab buildTab(Context context, Row tabRow) {
        DynamicLayoutTab tab = new DynamicLayoutTab();

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

    private List<DynamicLayoutRow> buildTabRows(Context context, Workbook workbook, String entityType,
            String shortname) {
        Sheet tab2boxSheet = getSheetByName(workbook, TAB2BOX_SHEET);

        Map<Integer, List<Row>> groupSheetRowsByRowColumn =
            getRowsByEntityAndColumnValue(tab2boxSheet, entityType, TAB_COLUMN, shortname)
            .collect(Collectors.groupingBy(row -> toInteger(getCellValue(row, ROW_COLUMN))));

        return groupSheetRowsByRowColumn.keySet().stream().sorted()
            .map(rowIndex -> buildTabRow(context, groupSheetRowsByRowColumn.get(rowIndex)))
            .collect(Collectors.toList());

    }

    private DynamicLayoutRow buildTabRow(Context context, List<Row> tab2boxRows) {

        DynamicLayoutRow row = new DynamicLayoutRow();
        getFirstNotEmptyRowStyle(tab2boxRows).ifPresent(row::setStyle);

        tab2boxRows.stream()
            .sorted(Comparator.comparing(Row::getRowNum))
            .forEach(tab2boxRow -> row.addCell(buildCell(context, tab2boxRow)));

        return row;

    }

    private DynamicLayoutCell buildCell(Context context, Row tab2boxRow) {
        DynamicLayoutCell cell = new DynamicLayoutCell();
        cell.setStyle(getCellValue(tab2boxRow, CELL_STYLE_COLUMN));
        buildBoxes(context, tab2boxRow).forEach(cell::addBox);
        return cell;
    }

    private List<DynamicLayoutBox> buildBoxes(Context context, Row tab2boxRow) {

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

    private DynamicLayoutBox buildBox(Context context, Sheet boxSheet, String entityType, String boxName) {

        Workbook workbook = boxSheet.getWorkbook();
        Row boxRow = getBowRowFromBoxSheet(boxSheet, entityType, boxName);

        DynamicLayoutBox box = new DynamicLayoutBox();

        String boxType = getCellValue(boxRow, TYPE_COLUMN);
        if (StringUtils.isBlank(boxType)) {
            boxType = DynamicLayoutBoxTypes.METADATA.name();
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

        if (boxType.equals(DynamicLayoutBoxTypes.METADATA.name())) {
            buildDynamicLayoutFields(context, workbook, entityType, boxName).forEach(box::addLayoutField);
        }

        return box;
    }

    private List<DynamicLayoutField> buildDynamicLayoutFields(Context context, Workbook workbook, String entityType,
        String boxName) {

        Sheet box2metadataSheet = getSheetByName(workbook, BOX2METADATA_SHEET);

        AtomicInteger priority = new AtomicInteger(0);
        return getRowsByEntityAndColumnValue(box2metadataSheet, entityType, BOX_COLUMN, boxName)
            .map(row -> buildDynamicLayoutField(context, row, priority))
            .collect(Collectors.toList());
    }

    private DynamicLayoutField buildDynamicLayoutField(Context context, Row row, AtomicInteger priority) {

        DynamicLayoutField field = buildDynamicLayoutFieldByType(context, row);

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

    private DynamicLayoutField buildDynamicLayoutFieldByType(Context context, Row row) {
        String fieldType = getCellValue(row, FIELD_TYPE_COLUMN);

        switch (fieldType) {
            case METADATAGROUP_TYPE:
                return buildMetadataGroupField(context, row);
            case BITSTREAM_TYPE:
                return buildBitstreamField(row);
            case METADATA_TYPE:
            default:
                return new DynamicLayoutFieldMetadata();
        }
    }

    private DynamicLayoutField buildMetadataGroupField(Context context, Row row) {
        DynamicLayoutFieldMetadata field = new DynamicLayoutFieldMetadata();
        buildDynamicMetadataGroups(context, row).forEach(field::addDynamicMetadataGroupList);
        return field;
    }

    private List<DynamicMetadataGroup> buildDynamicMetadataGroups(Context context, Row row) {
        String metadataField = getCellValue(row, METADATA_COLUMN);
        String entity = getEntityValue(row, ENTITY_COLUMN);

        Sheet metadatagroupsSheet = getSheetByName(row.getSheet().getWorkbook(), METADATAGROUPS_SHEET);

        AtomicInteger priority = new AtomicInteger(0);
        return getRowsByEntityAndColumnValue(metadatagroupsSheet, entity, PARENT_COLUMN, metadataField)
            .map(groupRow -> buildDynamicMetadataGroup(context, groupRow, entity, priority))
            .collect(Collectors.toList());
    }

    private DynamicMetadataGroup buildDynamicMetadataGroup(Context context, Row row, String entity,
            AtomicInteger priority) {
        DynamicMetadataGroup metadataGroup = new DynamicMetadataGroup();
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

    private DynamicLayoutField buildBitstreamField(Row row) {
        DynamicLayoutFieldBitstream field = new DynamicLayoutFieldBitstream();
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

    private Set<DynamicLayoutBox2SecurityGroup> buildBox2SecurityGroups(Context context, Workbook workbook,
                                                                     String sheetName, String entity, String name,
                                                                     DynamicLayoutBox dynamicLayoutBox,
                                                                     List<DynamicLayoutTab> tabs) {
        Sheet sheet = getSheetByName(workbook, sheetName);
        Set<DynamicLayoutBox2SecurityGroup> box2SecurityGroups = new HashSet<>();

        getRowsByEntityAndColumnValue(sheet, entity, SHORTNAME_COLUMN, name)
            .forEach(row -> {
                String groupName = getCellValue(row, GROUP_COLUMN);
                String alternativeBox = getCellValue(row, ALTERNATIVE_TO_COLUMN);

                if (StringUtils.isNotBlank(groupName)) {
                    Group group = getGroupField(context, groupName);
                    if (group != null) {
                        box2SecurityGroups.add(
                            buildBox2SecurityGroup(group, dynamicLayoutBox, entity, alternativeBox, tabs)
                        );
                    }
                }
            });

        return box2SecurityGroups;
    }

    private DynamicLayoutBox2SecurityGroup buildBox2SecurityGroup(Group group, DynamicLayoutBox box,
                                                               String entity,
                                                               String alternativeBox, List<DynamicLayoutTab> tabs) {

        DynamicLayoutBox2SecurityGroup.DynamicLayoutBox2SecurityGroupId box2SecurityGroupId =
            new DynamicLayoutBox2SecurityGroup.DynamicLayoutBox2SecurityGroupId(box, group);

        return new DynamicLayoutBox2SecurityGroup(box2SecurityGroupId, box, group,
            findAlternativeBox(alternativeBox, entity, tabs));
    }

    private DynamicLayoutBox findAlternativeBox(String alternativeBox, String entityType, List<DynamicLayoutTab> tabs) {

        if (alternativeBox == null) {
            return null;
        }

        return tabs.stream()
                   .flatMap(tab -> tab.getBoxes().stream())
                   .filter(dynamicLayoutBox -> dynamicLayoutBox.getShortname().equals(alternativeBox) &&
                       dynamicLayoutBox.getEntitytype().getLabel().equals(entityType))
                   .findFirst()
                   .orElseThrow(() -> new RuntimeException("Alternative box not found for shortname: " +
                       alternativeBox + ", entityType: " + entityType));
    }

    private Set<DynamicLayoutTab2SecurityGroup> buildTab2SecurityGroups(Context context, Workbook workbook,
                                                                     String sheetName, String entity, String name,
                                                                     DynamicLayoutTab dynamicLayoutTab,
                                                                     List<DynamicLayoutTab> tabs) {
        Sheet sheet = getSheetByName(workbook, sheetName);
        Set<DynamicLayoutTab2SecurityGroup> tab2SecurityGroups = new HashSet<>();

        getRowsByEntityAndColumnValue(sheet, entity, SHORTNAME_COLUMN, name)
            .forEach(row -> {
                String groupName = getCellValue(row, GROUP_COLUMN);
                String alternativeTab = getCellValue(row, ALTERNATIVE_TO_COLUMN);

                if (StringUtils.isNotBlank(groupName)) {
                    Group group = getGroupField(context, groupName);
                    if (group != null) {
                        tab2SecurityGroups.add(
                            buildTab2SecurityGroup(group, dynamicLayoutTab, entity, alternativeTab, tabs)
                        );
                    }
                }
            });

        return tab2SecurityGroups;
    }

    private DynamicLayoutTab2SecurityGroup buildTab2SecurityGroup(Group group, DynamicLayoutTab tab,
                                                               String entity,
                                                               String alternativeTab, List<DynamicLayoutTab> tabs) {

        DynamicLayoutTab2SecurityGroup.DynamicLayoutTab2SecurityGroupId tab2SecurityGroupId =
            new DynamicLayoutTab2SecurityGroup.DynamicLayoutTab2SecurityGroupId(tab, group);

        return new DynamicLayoutTab2SecurityGroup(tab2SecurityGroupId, tab, group,
            findAlternativeTab(alternativeTab, entity, tabs));
    }

    private DynamicLayoutTab findAlternativeTab(String alternativeTab, String entityType, List<DynamicLayoutTab> tabs) {

        if (alternativeTab == null) {
            return null;
        }

        return tabs.stream()
                   .filter(dynamicLayoutTab -> dynamicLayoutTab.getShortName().equals(alternativeTab) &&
                       dynamicLayoutTab.getEntity().getLabel().equals(entityType))
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
