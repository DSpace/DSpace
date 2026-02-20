/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.dspace.util.WorkbookUtils.createCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.app.bulkimport.service.BulkImportWorkbookBuilderImpl;
import org.dspace.content.Collection;
import org.dspace.util.WorkbookUtils;

/**
 * Class that model one of the sheets of the workbook produced by
 * {@link BulkImportWorkbookBuilderImpl}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class BulkImportSheet {

    private final Collection collection;

    private final Sheet sheet;

    private final Row headerRow;

    private final Map<String, Integer> headers;

    private final boolean nestedMetadata;

    public BulkImportSheet(Workbook workbook, String sheetname, boolean nestedMetadata, Collection collection) {
        this.sheet = workbook.createSheet(sheetname);
        this.collection = collection;
        this.headerRow = sheet.createRow(0);
        this.headers = new HashMap<String, Integer>();
        this.nestedMetadata = nestedMetadata;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public Collection getCollection() {
        return collection;
    }

    public List<String> getHeaders() {
        return new ArrayList<>(headers.keySet());
    }

    public boolean isNestedMetadata() {
        return nestedMetadata;
    }

    public boolean hasHeader(String header) {
        return headers.containsKey(header);
    }

    public int getHeaderPosition(String header) {
        return headers.getOrDefault(header, -1);
    }

    public Integer appendHeaderIfNotPresent(String header) {
        if (!hasHeader(header)) {
            return appendHeader(header);
        }
        return getHeaderPosition(header);
    }

    public Integer appendHeader(String header) {
        int lastColumn = headers.size();
        headers.put(header, lastColumn);
        createCell(headerRow, lastColumn, header);
        return lastColumn;
    }

    public Row appendRow() {
        return sheet.createRow(sheet.getPhysicalNumberOfRows());
    }

    public void setValueOnLastRow(String header, String value) {
        Row lastRow = sheet.getRow(sheet.getPhysicalNumberOfRows() - 1);
        int column = getHeaderPosition(header);
        if (column == -1) {
            throw new IllegalArgumentException("Unknown header '" + header + "'");
        }
        createCell(lastRow, column, value);
    }

    public void appendValueOnLastRow(String header, String value, String separator) {
        Row lastRow = sheet.getRow(sheet.getPhysicalNumberOfRows() - 1);
        int column = getHeaderPosition(header);
        if (column == -1) {
            throw new IllegalArgumentException("Unknown header '" + header + "'");
        }
        String cellContent = WorkbookUtils.getCellValue(lastRow, column);
        createCell(lastRow, column,
                getValueLimitedByLength(isEmpty(cellContent) ? value : cellContent + separator + value));
    }

    private String getValueLimitedByLength(String value) {
        return StringUtils.length(value) > 32726 ? value.substring(0, 32725) + "…" : value;
    }

}
