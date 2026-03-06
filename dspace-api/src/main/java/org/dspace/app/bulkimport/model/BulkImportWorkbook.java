/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.dspace.app.bulkimport.service.BulkImportWorkbookBuilderImpl;

/**
 * Class that model the workbook produced by {@link BulkImportWorkbookBuilderImpl}.
 * It is composed by the main sheet, the nested metadata sheets and the bistream
 * sheet.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
public class BulkImportWorkbook {

    private BulkImportSheet mainSheet;

    private List<BulkImportSheet> nestedMetadataSheets;

    private BulkImportSheet bitstreamSheet;


    public BulkImportWorkbook(BulkImportSheet mainSheet, List<BulkImportSheet> nestedMetadataSheets,
        BulkImportSheet bitstreamSheet) {
        this.mainSheet = mainSheet;
        this.nestedMetadataSheets = nestedMetadataSheets;
        this.bitstreamSheet = bitstreamSheet;
    }

    public BulkImportSheet getMainSheet() {
        return mainSheet;
    }

    public void setMainSheet(BulkImportSheet mainSheet) {
        this.mainSheet = mainSheet;
    }

    public List<BulkImportSheet> getNestedMetadataSheets() {
        return nestedMetadataSheets;
    }

    public Optional<BulkImportSheet> getNestedMetadataSheetByName(String name) {
        return nestedMetadataSheets.stream()
            .filter(sheet -> sheet.getSheet().getSheetName().equals(name))
            .findFirst();
    }

    public void setNestedMetadataSheets(List<BulkImportSheet> nestedMetadataSheets) {
        this.nestedMetadataSheets = nestedMetadataSheets;
    }

    public BulkImportSheet getBitstreamSheet() {
        return bitstreamSheet;
    }

    public void setBitstreamSheet(BulkImportSheet bitstreamSheet) {
        this.bitstreamSheet = bitstreamSheet;
    }

    public List<BulkImportSheet> getAllSheets() {
        List<BulkImportSheet> sheets = new ArrayList<BulkImportSheet>();
        sheets.add(mainSheet);
        sheets.addAll(nestedMetadataSheets);
        sheets.add(bitstreamSheet);
        return sheets;
    }

}
