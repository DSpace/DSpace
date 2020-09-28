/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.app.bulkimport.exception.BulkImportException;
import org.dspace.app.bulkimport.model.ImportParams;
import org.dspace.app.bulkimport.model.MainEntity;
import org.dspace.app.bulkimport.model.NestedEntity;
import org.dspace.app.bulkimport.service.BulkImportService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkImportServiceImpl implements BulkImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkImportServiceImpl.class);

    private ItemService itemService;

    private WorkspaceItemService workspaceItemService;

    @Override
    public void performImport(Context context, InputStream is, ImportParams params) {
        Workbook workbook = createWorkbook(is);
        validateWorkbook(workbook);
        List<MainEntity> mainEntities = readMainEntities(workbook);
        performImport(context, mainEntities, params);
    }

    private Workbook createWorkbook(InputStream is) {
        try {
            return WorkbookFactory.create(is);
        } catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
            LOGGER.error("An error occurs during the workbook creation", e);
            throw new BulkImportException("An error occurs during the workbook creation", e);
        }
    }

    private void validateWorkbook(Workbook workbook) {
        if (workbook.getNumberOfSheets() != 2) {
            throw new BulkImportException("The input Workbook must have 2 sheets (main entity and nested entity)");
        }

        for (Sheet sheet : workbook) {
            String name = sheet.getSheetName();
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new BulkImportException("The sheet " + name + " of the Workbook is empty");
            }
            if (isRowEmpty(sheet.getRow(0))) {
                throw new BulkImportException("The header of sheet " + name + " of the Workbook is empty");
            }
        }
    }

    private List<MainEntity> readMainEntities(Workbook workbook) {
        Sheet mainEntitySheet = workbook.getSheetAt(0);
        Sheet nestedEntitySheet = workbook.getSheetAt(0);

        List<NestedEntity> nestedEntities = readNestedEntities(nestedEntitySheet);
        List<String> headers = getHeaders(mainEntitySheet);

        return getRows(mainEntitySheet)
            .filter(row -> isNotFirstRow(row))
            .map(row -> buildMainEntity(row, headers, nestedEntities))
            .collect(Collectors.toList());
    }

    private List<NestedEntity> readNestedEntities(Sheet nestedEntitySheet) {
        List<String> headers = getHeaders(nestedEntitySheet);
        return getRows(nestedEntitySheet)
            .filter(row -> isNotFirstRow(row))
            .map(row -> buildNestedEntity(row, headers))
            .collect(Collectors.toList());
    }

    private List<String> getHeaders(Sheet sheet) {
        return getCells(sheet.getRow(0)).map(Cell::getStringCellValue).collect(Collectors.toList());
    }

    private NestedEntity buildNestedEntity(Row row, List<String> headers) {
        return null;
    }

    private MainEntity buildMainEntity(Row row, List<String> headers, List<NestedEntity> nestedEntities) {
        return null;
    }

    private void performImport(Context context, List<MainEntity> mainEntities, ImportParams params) {
        mainEntities.forEach(mainEntity -> performImport(context, mainEntity, params));
    }

    private void performImport(Context context, MainEntity mainEntity, ImportParams params) {

        Collection collection = params.getCollection();

        Item item = findItem(mainEntity);
        if (item == null) {
            item = createNewItem(context, collection);
        }

        if (!collection.equals(item.getOwningCollection())) {
            throw new BulkImportException("The item related to the entity with id " + mainEntity.getId()
                + " have a different own collection");
        }
    }

    private Item findItem(MainEntity mainEntity) {
        return null;
    }

    private Item createNewItem(Context context, Collection collection) {
        return null;

    }

    private boolean isRowEmpty(Row row) {
        return getCells(row).noneMatch(cell -> cell != null && cell.getCellTypeEnum() != CellType.BLANK);
    }

    private boolean isNotFirstRow(Row row) {
        return row.getRowNum() != 0;
    }

    private Stream<Cell> getCells(Row row) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(row.cellIterator(), 0), false);
    }

    private Stream<Row> getRows(Sheet sheet) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(sheet.rowIterator(), 0), false);
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public void setWorkspaceItemService(WorkspaceItemService workspaceItemService) {
        this.workspaceItemService = workspaceItemService;
    }

}
