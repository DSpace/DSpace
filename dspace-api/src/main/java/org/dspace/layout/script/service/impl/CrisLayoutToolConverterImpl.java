/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.service.impl;

import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX2METADATA_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX_POLICY_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.BOX_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.METADATAGROUPS_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.METADATAGROUP_TYPE;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB2BOX_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB_POLICY_SHEET;
import static org.dspace.layout.script.service.CrisLayoutToolValidator.TAB_SHEET;
import static org.dspace.util.WorkbookUtils.createCell;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.content.MetadataField;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBox2SecurityGroup;
import org.dspace.layout.CrisLayoutCell;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisLayoutTab2SecurityGroup;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.script.service.CrisLayoutToolConverter;

/**
 * Implementation of {@link CrisLayoutToolConverter}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class CrisLayoutToolConverterImpl implements CrisLayoutToolConverter {

    @Override
    public Workbook convert(List<CrisLayoutTab> tabs) {
        Workbook workbook = getTemplateWorkBook();
        buildTab(workbook, tabs);
        autoSizeAllSheetsColumns(workbook);
        return workbook;
    }

    private Workbook getTemplateWorkBook() {
        try (InputStream inputStream =
                 CrisLayoutToolConverterImpl.class
                     .getResourceAsStream("cris-layout-configuration-template.xls")) {
            return WorkbookFactory.create(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildTabRow(Sheet sheet, CrisLayoutTab tab) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, tab.getEntity().getLabel());
        createCell(row, 1, tab.getShortName());
        createCell(row, 2, tab.getHeader());
        createCell(row, 3, String.valueOf(tab.getPriority()));
        createCell(row, 4, convertToString(tab.isLeading()));
        createCell(row, 5, toSecurity(tab.getSecurity()));
    }

    private void buildTab(Workbook workbook, List<CrisLayoutTab> tabs) {
        Sheet sheet = workbook.getSheet(TAB_SHEET);
        Map<String, CrisLayoutBox> allUniqueBoxes = new HashMap<>();

        tabs.forEach(tab -> {
            buildTabRow(sheet, tab);

            // Collect boxes from each tab
            Map<String, CrisLayoutBox> tabBoxes = buildTab2boxRows(workbook, tab);
            allUniqueBoxes.putAll(tabBoxes);

            buildTabPolicy(workbook, tab);
        });

        // Process all unique boxes once
        buildAllBoxes(workbook, allUniqueBoxes);
    }

    // Builds tab2box rows and returns collected boxes
    private Map<String, CrisLayoutBox> buildTab2boxRows(Workbook workbook, CrisLayoutTab tab) {
        Sheet sheet = workbook.getSheet(TAB2BOX_SHEET);
        Map<String, CrisLayoutBox> tabUniqueBoxes = new HashMap<>();

        for (int i = 0; i < tab.getRows().size(); i++) {
            int rowIndex = i + 1;
            tab.getRows().get(i).getCells()
                   .forEach(cell -> {
                       buildTab2boxRow(sheet, rowIndex, cell);

                       cell.getBoxes().forEach(box -> {
                           String boxKey = createBoxKey(box);
                           tabUniqueBoxes.computeIfAbsent(boxKey, k -> box);
                       });
                   });
        }

        return tabUniqueBoxes;
    }

    private void buildAllBoxes(Workbook workbook, Map<String, CrisLayoutBox> allUniqueBoxes) {
        if (!allUniqueBoxes.isEmpty()) {
            List<CrisLayoutBox> uniqueBoxesList = new ArrayList<>(allUniqueBoxes.values());
            buildBox(workbook, uniqueBoxesList);
            buildBoxPolicy(workbook, uniqueBoxesList);
        }
    }


    /**
     * Create a unique string key for a CrisLayoutBox
     */
    private String createBoxKey(CrisLayoutBox box) {
        String entityLabel = box.getCell().getRow().getTab().getEntity().getLabel();
        String shortname = box.getShortname();
        return entityLabel + ":" + shortname;
    }

    private void buildTab2boxRow(Sheet sheet, int cellIndex, CrisLayoutCell cell) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, cell.getRow().getTab().getEntity().getLabel());
        createCell(row, 1, cell.getRow().getTab().getShortName());
        createCell(row, 2, String.valueOf(cellIndex));
        createCell(row, 3, cell.getRow().getStyle());
        createCell(row, 4, cell.getStyle());
        createCell(row, 5, getBoxesNames(cell.getBoxes()));
    }

    private String getBoxesNames(List<CrisLayoutBox> boxes) {
        return boxes.stream()
                    .map(box -> box.getShortname())
                    .collect(Collectors.joining(", "));
    }

    private void buildBox(Workbook workbook, List<CrisLayoutBox> boxes) {
        Sheet sheet = workbook.getSheet(BOX_SHEET);
        boxes.forEach(box -> {
            buildBoxRow(sheet, box);
            buildBox2metadata(sheet.getWorkbook(), box.getLayoutFields());
        });
    }

    private void buildBoxRow(Sheet sheet, CrisLayoutBox box) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, box.getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, convertToString(box.getCollapsed()));
        createCell(row, 2, box.getType());
        createCell(row, 3, box.getShortname());
        createCell(row, 4, box.getHeader());
        createCell(row, 5, convertToString(box.isContainer()));
        createCell(row, 6, convertToString(box.getMinor()));
        createCell(row, 7, toSecurity(box.getSecurity()));
        createCell(row, 8, box.getStyle());
    }

    private void buildBox2metadata(Workbook workbook, List<CrisLayoutField> layoutFields) {
        Sheet sheet = workbook.getSheet(BOX2METADATA_SHEET);
        layoutFields.forEach(layoutField -> {
            buildBox2metadataRow(sheet, layoutField);
            buildMetadataGroups(sheet.getWorkbook(), layoutField.getCrisMetadataGroupList());
        });
    }

    private void buildBox2metadataRow(Sheet sheet, CrisLayoutField layoutField) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, layoutField.getBox().getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, layoutField.getBox().getShortname());
        createCell(row, 2, String.valueOf(layoutField.getRow()));
        createCell(row, 3, String.valueOf(layoutField.getCell()));
        createCell(row, 4, getLayoutFieldType(layoutField));
        createCell(row, 5, getMetadataField(layoutField));
        createCell(row, 6, getMetadataValue(layoutField));
        createCell(row, 7, getBundle(layoutField));
        createCell(row, 8, layoutField.getLabel());
        createCell(row, 9, convertToString(layoutField.isLabelAsHeading()));
        createCell(row, 10, layoutField.getRendering());
        createCell(row, 11, convertToString(layoutField.isValuesInline()));
        createCell(row, 12, layoutField.getRowStyle());
        createCell(row, 13, layoutField.getCellStyle());
        createCell(row, 14, layoutField.getStyleLabel());
        createCell(row, 15, layoutField.getStyleValue());
    }

    private String getMetadataValue(CrisLayoutField layoutField) {
        String value = "";
        if (layoutField instanceof CrisLayoutFieldBitstream) {
            value = ((CrisLayoutFieldBitstream) layoutField).getMetadataValue();
        }
        return value;
    }

    private String getBundle(CrisLayoutField layoutField) {
        String value = "";
        if (layoutField instanceof CrisLayoutFieldBitstream) {
            value = ((CrisLayoutFieldBitstream) layoutField).getBundle();
        }
        return value;
    }

    private String getMetadataField(CrisLayoutField layoutField) {
        return Optional.ofNullable(layoutField.getMetadataField())
                       .map(metadataField -> metadataField.toString('.'))
                       .orElse("");
    }

    private String getLayoutFieldType(CrisLayoutField layoutField) {
        String type = layoutField.getType();
        if (CollectionUtils.isNotEmpty(layoutField.getCrisMetadataGroupList())) {
            type = METADATAGROUP_TYPE;
        }
        return type;
    }

    private void buildMetadataGroups(Workbook workbook, List<CrisMetadataGroup> crisMetadataGroups) {
        Sheet sheet = workbook.getSheet(METADATAGROUPS_SHEET);
        crisMetadataGroups
            .forEach(crisMetadataGroup ->
                buildMetadataGroupRow(sheet, crisMetadataGroup));
    }

    private void buildMetadataGroupRow(Sheet sheet, CrisMetadataGroup crisMetadataGroup) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        CrisLayoutField crisLayoutField = crisMetadataGroup.getCrisLayoutField();

        createCell(row, 0, crisLayoutField.getBox().getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, crisLayoutField.getMetadataField().toString('.'));
        createCell(row, 2, crisLayoutField.getType());
        createCell(row, 3, crisMetadataGroup.getMetadataField().toString('.'));
        createCell(row, 4, "");
        createCell(row, 5, "");
        createCell(row, 6, crisMetadataGroup.getLabel());
        createCell(row, 7, crisMetadataGroup.getRendering());
        createCell(row, 8, crisMetadataGroup.getStyleLabel());
        createCell(row, 9, crisMetadataGroup.getStyleValue());
    }

    private void buildTabPolicy(Workbook workbook, CrisLayoutTab tab) {
        Sheet sheet = workbook.getSheet(TAB_POLICY_SHEET);
        tab.getMetadataSecurityFields()
            .forEach(metadataField ->
                buildTabPolicyMetadataSecurityFieldRow(sheet, tab, metadataField)
            );

        tab.getTab2SecurityGroups()
            .forEach(tab2SecurityGroup ->
                buildTabPolicyGroupSecurityFieldRow(sheet, tab, tab2SecurityGroup)
            );
    }

    private void buildTabPolicyMetadataSecurityFieldRow(Sheet sheet, CrisLayoutTab tab, MetadataField metadataField) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, tab.getEntity().getLabel());
        createCell(row, 1, tab.getShortName());
        createCell(row, 2, metadataField.toString('.'));
        createCell(row, 3, "");
        createCell(row, 4, "");
    }

    private void buildTabPolicyGroupSecurityFieldRow(Sheet sheet, CrisLayoutTab tab,
                                                     CrisLayoutTab2SecurityGroup tab2SecurityGroup) {
        CrisLayoutTab alternativeTab = tab2SecurityGroup.getAlternativeTab();
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, tab.getEntity().getLabel());
        createCell(row, 1, tab.getShortName());
        createCell(row, 2, "");
        createCell(row, 3, tab2SecurityGroup.getGroup().getName());
        createCell(row, 4, alternativeTab == null ? "" : alternativeTab.getShortName());
    }

    private void buildBoxPolicy(Workbook workbook, List<CrisLayoutBox> boxes) {
        Sheet sheet = workbook.getSheet(BOX_POLICY_SHEET);
        boxes.forEach(box -> {
            box.getMetadataSecurityFields()
                .forEach(metadataField ->
                    buildBoxPolicyMetadataSecurityFieldRow(sheet, box, metadataField)
                );

            box.getBox2SecurityGroups()
                .forEach(box2SecurityGroup ->
                    buildBoxPolicyGroupSecurityFieldRow(sheet, box, box2SecurityGroup)
                );
        });
    }

    private void buildBoxPolicyMetadataSecurityFieldRow(Sheet sheet, CrisLayoutBox box, MetadataField metadataField) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, box.getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, box.getShortname());
        createCell(row, 2, metadataField.toString('.'));
        createCell(row, 3, "");
        createCell(row, 4, "");
    }

    private void buildBoxPolicyGroupSecurityFieldRow(Sheet sheet, CrisLayoutBox box,
                                                     CrisLayoutBox2SecurityGroup box2SecurityGroup) {

        CrisLayoutBox alternativeBox = box2SecurityGroup.getAlternativeBox();
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        createCell(row, 0, box.getCell().getRow().getTab().getEntity().getLabel());
        createCell(row, 1, box.getShortname());
        createCell(row, 2, "");
        createCell(row, 3, box2SecurityGroup.getGroup().getName());
        createCell(row, 4, alternativeBox == null ? "" : alternativeBox.getShortname());
    }

    private String convertToString(boolean value) {
        return value ? "y" : "n";
    }

    private String toSecurity(Integer security) {
        return String.valueOf(LayoutSecurity.valueOf(security))
                     .replaceAll("_", " ")
                     .replaceAll("AND", "&");
    }

    private void autoSizeAllSheetsColumns(Workbook workbook) {
        autoSizeColumns(workbook.getSheet(TAB_SHEET));
        autoSizeColumns(workbook.getSheet(TAB2BOX_SHEET));
        autoSizeColumns(workbook.getSheet(BOX_SHEET));
        autoSizeColumns(workbook.getSheet(BOX2METADATA_SHEET));
        autoSizeColumns(workbook.getSheet(METADATAGROUPS_SHEET));
        autoSizeColumns(workbook.getSheet(TAB_POLICY_SHEET));
        autoSizeColumns(workbook.getSheet(BOX_POLICY_SHEET));
    }

    private void autoSizeColumns(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > 0) {
            Row row = sheet.getRow(sheet.getFirstRowNum());
            for (Cell cell : row) {
                int columnIndex = cell.getColumnIndex();
                sheet.autoSizeColumn(columnIndex);
            }
        }
    }

}
