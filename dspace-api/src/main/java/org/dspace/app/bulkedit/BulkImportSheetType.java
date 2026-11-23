/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.apache.poi.ss.usermodel.Sheet;

/**
 * Enum that identifies the type of a bulk import excel sheet.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum BulkImportSheetType {

    /**
     * The type of the main sheet of the Bulk Import excel.
     */
    ENTITY_ROWS,

    /**
     * The type of the sheets with nested metadata groups.
     */
    METADATA_GROUPS,

    /**
     * The type of the sheet with the bitstream access condition and metadata
     * fields.
     */
    BITSTREAMS;

    public static BulkImportSheetType getTypeFromSheet(Sheet sheet) {

        if (sheet.getWorkbook().getSheetIndex(sheet) == 0) {
            return BulkImportSheetType.ENTITY_ROWS;
        }

        if (BulkImport.BITSTREAMS_SHEET_NAME.equalsIgnoreCase(sheet.getSheetName())) {
            return BulkImportSheetType.BITSTREAMS;
        }

        return BulkImportSheetType.METADATA_GROUPS;

    }

}
