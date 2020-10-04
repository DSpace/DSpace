/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.utils;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterators;
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

    public static String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        return getCellValue(cell);
    }

    public static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}
