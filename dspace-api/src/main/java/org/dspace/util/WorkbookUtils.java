/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public final class WorkbookUtils {

    private WorkbookUtils() {

    }

    public static boolean isSheetEmpty(Sheet sheet) {
        return getRows(sheet).allMatch(WorkbookUtils::isRowEmpty);
    }

    public static boolean isRowEmpty(Row row) {
        return getCells(row).allMatch(cell -> StringUtils.isBlank(getCellValue(cell)));
    }

    public static boolean isNotEmptyRow(Row row) {
        return !isRowEmpty(row);
    }

    public static boolean isNotFirstRow(Row row) {
        return row.getRowNum() != 0;
    }

    public static boolean isCellEmpty(Cell cell) {
        return StringUtils.isBlank(getCellValue(cell));
    }

    public static boolean isCellNotEmpty(Cell cell) {
        return !isCellEmpty(cell);
    }

    public static Stream<Cell> getCells(Row row) {
        int lastNotEmptyColumnIndex = stream(spliteratorUnknownSize(row.cellIterator(), 0), false)
            .filter(WorkbookUtils::isCellNotEmpty)
            .mapToInt(Cell::getColumnIndex)
            .max().orElse(-1);

        List<Cell> cells = new ArrayList<Cell>();
        for (int i = 0; i <= lastNotEmptyColumnIndex; i++) {
            cells.add(row.getCell(i));
        }

        return cells.stream();
    }

    public static Stream<Row> getRows(Sheet sheet) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(sheet.rowIterator(), 0), false);
    }

    public static List<Row> getNotEmptyRowsSkippingHeader(Sheet sheet) {
        return getRows(sheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .collect(Collectors.toList());
    }

    public static List<String> getRowValues(Row row, int size) {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            values.add(getCellValue(row, i));
        }
        return values;
    }

    public static String getEntityTypeCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        return getEntityTypeValue(cell);
    }

    public static String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        return getCellValue(cell);
    }

    public static String getCellValue(Row row, String headerName) {
        int headerIndex = getCellIndexFromHeaderName(row.getSheet(), headerName);
        return headerIndex != -1 ? getCellValue(row.getCell(headerIndex)) : null;
    }

    public static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    public static String getEntityTypeValue(Cell cell) {
        String cellValue = getCellValue(cell);
        return Optional.ofNullable(cellValue)
                    .filter(value -> StringUtils.isNotBlank(value))
                    .filter(value -> value.contains("."))
                    .map(value -> value.split("\\.")[0])
                    .orElse(cellValue);
    }

    public static Cell createCell(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        return cell;
    }

    public static List<String> getAllHeaders(Sheet sheet) {
        return getCells(sheet.getRow(0))
            .map(cell -> getCellValue(cell))
            .collect(Collectors.toList());
    }

    public static List<Cell> getColumnWithoutHeader(Sheet sheet, int column) {
        return WorkbookUtils.getRows(sheet)
            .filter(WorkbookUtils::isNotFirstRow)
            .filter(WorkbookUtils::isNotEmptyRow)
            .map(row -> row.getCell(column, CREATE_NULL_AS_BLANK))
            .collect(Collectors.toList());
    }

    public static int getCellIndexFromHeaderName(Sheet sheet, String headerName) {
        Row row = sheet.getRow(0);
        if (row == null) {
            return -1;
        }

        return WorkbookUtils.getCells(row)
            .filter(cell -> headerName.equals(WorkbookUtils.getCellValue(cell)))
            .map(cell -> cell.getColumnIndex())
            .findFirst().orElse(-1);
    }
}
