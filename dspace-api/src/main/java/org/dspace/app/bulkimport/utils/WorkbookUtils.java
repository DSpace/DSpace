/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.utils;

import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public final class WorkbookUtils {

    private WorkbookUtils() {

    }

    public static boolean isRowEmpty(Row row) {
        return row.getPhysicalNumberOfCells() == 0;
    }

    public static boolean isNotEmptyRow(Row row) {
        return !isRowEmpty(row);
    }

    public static boolean isNotFirstRow(Row row) {
        return row.getRowNum() != 0;
    }

    public static Stream<Cell> getCells(Row row) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(row.cellIterator(), 0), false);
    }

    public static Stream<Row> getRows(Sheet sheet) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(sheet.rowIterator(), 0), false);
    }

    public static String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }
}
