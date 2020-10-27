/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Implementation of {@StreamDisseminationCrosswalk} to produce a xls file starting from a template.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XlsCrosswalk extends TabularCrosswalk {

    private String sheetName;

    @Override
    public String getMIMEType() {
        return "application/vnd.ms-excel";
    }

    @Override
    protected void writeRows(List<List<String>> rows, OutputStream out) {

        try (Workbook workbook = new HSSFWorkbook()) {

            Sheet sheet = workbook.createSheet(sheetName);

            int rowCount = 0;
            for (List<String> row : rows) {
                Row sheetRow = sheet.createRow(rowCount++);
                int cellCount = 0;
                for (String field : row) {
                    Cell cell = sheetRow.createCell(cellCount++);
                    cell.setCellValue(field);
                }
            }

            autoSizeColumns(sheet);

            workbook.write(out);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

    protected String getValuesSeparator() {
        return configurationService.getProperty("crosswalk.xls.separator.values", "||");
    }

    protected String getNestedValuesSeparator() {
        return configurationService.getProperty("crosswalk.xls.separator.nested-values", "||");
    }

    protected String getInsideNestedSeparator() {
        return configurationService.getProperty("crosswalk.xls.separator.inside-nested", "/");
    }

    protected String escapeValue(String value) {
        return value;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

}
