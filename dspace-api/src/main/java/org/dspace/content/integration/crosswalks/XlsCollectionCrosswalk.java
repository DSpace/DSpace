/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static org.dspace.app.bulkedit.BulkImport.AUTHORITY_SEPARATOR;
import static org.dspace.app.bulkedit.BulkImport.ID_CELL;
import static org.dspace.app.bulkedit.BulkImport.LANGUAGE_SEPARATOR_PREFIX;
import static org.dspace.app.bulkedit.BulkImport.LANGUAGE_SEPARATOR_SUFFIX;
import static org.dspace.app.bulkedit.BulkImport.METADATA_SEPARATOR;
import static org.dspace.app.bulkedit.BulkImport.PARENT_ID_CELL;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.app.bulkedit.BulkImport;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.model.XlsCollectionSheet;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link StreamDisseminationCrosswalk} to export all the item
 * of the given collection in the xls format. This format is the same expected
 * by the import performed with {@link BulkImport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XlsCollectionCrosswalk implements StreamDisseminationCrosswalk {

    private static Logger log = Logger.getLogger(XlsCollectionCrosswalk.class);

    @Autowired
    private ItemService itemService;

    private DCInputsReader reader;

    @PostConstruct
    private void postConstruct() {
        try {
            this.reader = new DCInputsReader();
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.COLLECTION;
    }

    @Override
    public String getMIMEType() {
        return "application/vnd.ms-excel";
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (!canDisseminate(context, dso)) {
            throw new CrosswalkObjectNotSupported("Can only crosswalk a Collection");
        }

        Collection collection = (Collection) dso;

        try (Workbook workbook = new HSSFWorkbook()) {

            XlsCollectionSheet mainSheet = writeMainSheetHeader(context, collection, workbook);
            List<XlsCollectionSheet> nestedMetadataSheets = writeNestedMetadataSheetsHeader(collection, workbook);

            writeWorkbookContent(context, collection, mainSheet, nestedMetadataSheets);

            List<XlsCollectionSheet> sheets = new ArrayList<XlsCollectionSheet>(nestedMetadataSheets);
            sheets.add(mainSheet);
            autoSizeColumns(sheets);

            workbook.write(out);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private List<XlsCollectionSheet> writeNestedMetadataSheetsHeader(Collection collection, Workbook workbook) {
        return getSubmissionFormMetadataGroups(collection).stream()
            .map(metadataGroup -> writeNestedMetadataSheetHeader(collection, workbook, metadataGroup))
            .collect(Collectors.toList());
    }

    private XlsCollectionSheet writeMainSheetHeader(Context context, Collection collection, Workbook workbook) {
        XlsCollectionSheet mainSheet = new XlsCollectionSheet(workbook, "items");
        mainSheet.appendHeader(ID_CELL);
        List<String> metadataFields = getSubmissionFormMetadata(collection);
        for (String metadataField : metadataFields) {
            mainSheet.appendHeaderIfNotPresent(metadataField);
        }
        return mainSheet;
    }

    private XlsCollectionSheet writeNestedMetadataSheetHeader(Collection collection, Workbook workbook, String field) {
        XlsCollectionSheet nestedMetadataSheet = new XlsCollectionSheet(workbook, field);
        List<String> nestedMetadataFields = getSubmissionFormMetadataGroup(collection, field);
        nestedMetadataSheet.appendHeader(PARENT_ID_CELL);
        for (String metadataField : nestedMetadataFields) {
            nestedMetadataSheet.appendHeader(metadataField);
        }
        return nestedMetadataSheet;
    }

    private void writeWorkbookContent(Context context, Collection collection, XlsCollectionSheet mainSheet,
        List<XlsCollectionSheet> nestedMetadataSheets) throws SQLException {

        Iterator<Item> itemIterator = itemService.findByCollection(context, collection);
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            writeMainSheet(context, item, mainSheet);
            nestedMetadataSheets.forEach(sheet -> writeNestedMetadataSheet(context, item, sheet));
            context.uncacheEntity(item);
        }

    }

    private void writeMainSheet(Context context, Item item, XlsCollectionSheet mainSheet) {

        mainSheet.appendRow();

        List<String> headers = mainSheet.getHeaders();
        for (String header : headers) {

            if (header.equals(ID_CELL)) {
                mainSheet.setValueOnLastRow(header, item.getID().toString());
                continue;
            }

            getMetadataValues(item, header).forEach(value -> writeMetadataValue(item, mainSheet, header, value));
        }

    }

    private void writeNestedMetadataSheet(Context context, Item item, XlsCollectionSheet nestedMetadataSheet) {

        String groupName = nestedMetadataSheet.getSheet().getSheetName();
        int groupSize = getMetadataGroupSize(item, groupName);
        List<String> headers = nestedMetadataSheet.getHeaders();

        Map<String, List<MetadataValue>> metadataValues = new HashMap<>();

        IntStream.range(0, groupSize).forEach(
            groupIndex -> writeNestedMetadataRow(item, nestedMetadataSheet, metadataValues, headers, groupIndex));

    }

    private void writeNestedMetadataRow(Item item, XlsCollectionSheet nestedMetadataSheet,
        Map<String, List<MetadataValue>> metadataValues, List<String> headers, int groupIndex) {

        nestedMetadataSheet.appendRow();

        for (String header : headers) {

            if (header.equals(PARENT_ID_CELL)) {
                nestedMetadataSheet.setValueOnLastRow(header, item.getID().toString());
                continue;
            }

            List<MetadataValue> metadata = null;
            if (metadataValues.containsKey(header)) {
                metadata = metadataValues.get(header);
            } else {
                metadata = getMetadataValues(item, header);
                metadataValues.put(header, metadata);
            }

            if (metadata.size() <= groupIndex) {
                log.warn("The cardinality of group with nested metadata " + header + " is inconsistent "
                    + "for item with id " + item.getID());
                continue;
            }

            writeMetadataValue(item, nestedMetadataSheet, header, metadata.get(groupIndex));

        }
    }

    private void writeMetadataValue(Item item, XlsCollectionSheet sheet, String header, MetadataValue metadataValue) {

        String language = metadataValue.getLanguage();
        if (StringUtils.isBlank(language)) {
            sheet.appendValueOnLastRow(header, formatMetadataValue(metadataValue), METADATA_SEPARATOR);
            return;
        }

        if (isLanguageSupported(item.getOwningCollection(), language, header)) {
            String headerWithLanguage = header + LANGUAGE_SEPARATOR_PREFIX + language + LANGUAGE_SEPARATOR_SUFFIX;
            sheet.appendHeaderIfNotPresent(headerWithLanguage);
            sheet.appendValueOnLastRow(headerWithLanguage, formatMetadataValue(metadataValue), METADATA_SEPARATOR);
        }

    }

    private String formatMetadataValue(MetadataValue metadata) {

        String value = metadata.getValue();
        value = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.equals(value) ? "" : value;

        String authority = metadata.getAuthority();
        int confidence = metadata.getConfidence();

        if (StringUtils.isBlank(authority)) {
            return value;
        }

        return value + AUTHORITY_SEPARATOR + authority + AUTHORITY_SEPARATOR + confidence;
    }

    private List<String> getSubmissionFormMetadataGroup(Collection collection, String groupName) {
        try {
            return this.reader.getAllNestedMetadataByGroupName(collection, groupName);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException("An error occurs reading the input configuration "
                + "by group name " + groupName, e);
        }
    }

    private List<String> getSubmissionFormMetadata(Collection collection) {
        try {
            return this.reader.getSubmissionFormMetadata(collection);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException("An error occurs reading the input configuration by collection", e);
        }
    }

    private List<String> getSubmissionFormMetadataGroups(Collection collection) {
        try {
            return this.reader.getSubmissionFormMetadataGroups(collection);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException("An error occurs reading the input configuration by collection", e);
        }
    }

    private boolean isLanguageSupported(Collection collection, String language, String metadataField) {
        try {
            List<String> languages = this.reader.getLanguagesForMetadata(collection, metadataField);
            return CollectionUtils.isNotEmpty(languages) ? languages.contains(language) : false;
        } catch (DCInputsReaderException e) {
            throw new RuntimeException("An error occurs reading the input configuration by collection", e);
        }
    }

    private int getMetadataGroupSize(Item item, String metadataGroupFieldName) {
        return getMetadataValues(item, metadataGroupFieldName).size();
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private void autoSizeColumns(List<XlsCollectionSheet> sheets) {
        sheets.forEach(sheet -> autoSizeColumns(sheet.getSheet()));
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

    public void setReader(DCInputsReader reader) {
        this.reader = reader;
    }

}
