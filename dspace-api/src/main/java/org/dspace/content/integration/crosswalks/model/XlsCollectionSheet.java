/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.model;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.dspace.app.bulkimport.utils.WorkbookUtils.createCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.app.bulkimport.utils.WorkbookUtils;
import org.dspace.content.integration.crosswalks.XlsCollectionCrosswalk;

/**
 * Class that model one of the sheets of the workbook produced by
 * {@link XlsCollectionCrosswalk}.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class XlsCollectionSheet {

    private final Sheet sheet;

    private final Row headerRow;

    private final Map<String, Integer> headers;

    public XlsCollectionSheet(Workbook workbook, String sheetname) {
        this.sheet = workbook.createSheet(sheetname);
        this.headerRow = sheet.createRow(0);
        this.headers = new HashMap<String, Integer>();
    }

    public Sheet getSheet() {
        return sheet;
    }

    public List<String> getHeaders() {
        return new ArrayList<>(headers.keySet());
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
        createCell(lastRow, column, isEmpty(cellContent) ? value : cellContent + separator + value);
    }

}
