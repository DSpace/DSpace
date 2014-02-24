/*
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;
import org.dspace.workflow.DryadWorkflowUtils;


/**
 * Propagate metadata from a Dryad Data Package to Dryad Data Files.
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PropagateItemMetadataForm extends AbstractDSpaceTransformer{

    private static final Message T_title = message("xmlui.administrative.item.PropagateItemMetadataForm.title");
    private static final Message T_column_name = message("xmlui.administrative.item.PropagateItemMetadataForm.name");
    private static final Message T_column_file = message("xmlui.administrative.item.PropagateItemMetadataForm.file");
    private static final Message T_column_current_value = message("xmlui.administrative.item.PropagateItemMetadataForm.current_value");
    private static final Message T_column_new_value = message("xmlui.administrative.item.PropagateItemMetadataForm.new_value");
    private static final Message T_column_select = message("xmlui.administrative.item.PropagateItemMetadataForm.select");

    private static final Message T_cell_no_value = message("xmlui.administrative.item.PropagateItemMetadataForm.no_value");

    private static final Message T_button_update = message("xmlui.administrative.item.PropagateItemMetadataForm.update");
    private static final Message T_button_return = message("xmlui.administrative.item.PropagateItemMetadataForm.return");

    private static Logger log = Logger.getLogger(PropagateItemMetadataForm.class);

    private DOIIdentifierProvider doiService = new DSpace().getSingletonService(DOIIdentifierProvider.class);

    private String metadataSchema;
    private String metadataElement;
    private String metadataQualifier;

    private int metadataValuePosition = 0;
    private Item dataPackage;
    private Item[] dataFiles = new Item[] {};

    // addPageMeta?
    @Override
    public void addBody(Body body) throws SQLException, WingException {
        /*
         * Propagation of Metadata              X
         * ----------
         * Name             File            Current         New                 Select
         * dc.contrib.aut   dataFile1       Smith, J        Smith, Jane Ph. D   x
         *
         * <Update> <Close>
         */

        // Get the metadata field we're editing
        String metadataFieldName = null;
        try {
            metadataFieldName = parameters.getParameter("metadataFieldName");
        } catch (ParameterException ex) {
            log.error("Metadata Field Name not found in parameters", ex);
            body.addDivision("error").setHead("Error - metadata field ID not found in parameters");
            return;
        }
        loadMetadataField(metadataFieldName);

        // Get the metadata for the package item.
        String packageDoi = null;
        String fileDois[] = null;
        try {
            packageDoi = parameters.getParameter("packageDoi");
        } catch (ParameterException ex) {
            log.error("Package DOI not found in parameters", ex);
            body.addDivision("error").setHead("Error - package DOI not found in parameters");
            return;
        }

        loadDataPackage(packageDoi);
        loadDataFiles();

        // what metadata field?
        // get the metadata values for the file, including the file name

        Division main = body.addInteractiveDivision("propagate", contextPath + "/admin/propagate-metadata", Division.METHOD_POST, "primary administrative item");
        main.setHead(T_title);
        int numberOfDataFiles = dataFiles.length;
        int numberOfRows = numberOfDataFiles + 1;
        int numberOfColumns = 5; // [Name, File, Current, New, Select]
        Table propagateTable = main.addTable("propagate_table", numberOfRows, numberOfColumns);

        // Header row
        Row headerRow = propagateTable.addRow("propagate_table_headerrow", Row.ROLE_HEADER, "propagate-table-header");
        headerRow.addCellContent(T_column_name);
        headerRow.addCellContent(T_column_file);
        headerRow.addCellContent(T_column_current_value);
        headerRow.addCellContent(T_column_new_value);
        headerRow.addCellContent(T_column_select);

        // Data row for each package
        for(Item dataFile : dataFiles) {
            Row fileRow = propagateTable.addRow("propagate_table_filerow", Row.ROLE_DATA, "propagate-table-file");

            Cell cell = null; // reusable

            // metadata field name
            cell = fileRow.addCell();
            cell.addContent(formatMetadataField());

            // File name
            cell = fileRow.addCell();
            DCValue[] fileNames = dataFile.getMetadata("dc.title");
            if(fileNames.length > 0) {
                cell.addContent(fileNames[0].value);
            }

            // Current Value - from file
            cell = fileRow.addCell();
            DCValue[] fileValues = dataFile.getMetadata(this.metadataSchema, this.metadataElement, this.metadataQualifier, Item.ANY);
            if(fileValues.length > metadataValuePosition) {
                cell.addContent(fileValues[metadataValuePosition].value);
            } else {
                cell.addContent(T_cell_no_value);
            }

            // New value - from package
            cell = fileRow.addCell();
            DCValue[] packageValues = dataFile.getMetadata(this.metadataSchema, this.metadataElement, this.metadataQualifier, Item.ANY);
            if(packageValues.length > metadataValuePosition) {
                cell.addContent(packageValues[metadataValuePosition].value);
            } else {
                cell.addContent(T_cell_no_value);
            }

            // Select - checkbox
            cell = fileRow.addCell();
            cell.addCheckBox("propagate_select");
        }

        Para actions = main.addPara();
        actions.addButton("submit_update").setValue(T_button_update);
        actions.addButton("submit_return").setValue(T_button_return);
}

    private void loadMetadataField(String metadataFieldName) {
        if (metadataFieldName == null) {
            log.error("Unable to parse metadata field - no field name provided");
            return;
        }
        if (metadataFieldName.contains("_") == false) {
            log.error("Unable to split metadata fields - '_' not found in " + metadataFieldName);
            return;
        }

        String[] fields = StringUtils.split(metadataFieldName, "_");
        switch(fields.length) {
            case 2:
                this.metadataSchema = fields[0];
                this.metadataElement = fields[1];
                break;
            case 3:
                this.metadataSchema = fields[0];
                this.metadataElement = fields[1];
                this.metadataQualifier = fields[2];
                break;
            default:
                log.error("Expected 2 or 3 fields, found " +
                        fields.length + " after splitting " + metadataFieldName);
                break;
        }
        return;
    }
    
    private String formatMetadataField() {
        if(this.metadataQualifier != null && this.metadataQualifier.length() > 0) {
            return String.format("%s.%s.%s", this.metadataSchema, this.metadataElement, this.metadataQualifier);
        } else {
            return String.format("%s.%s", this.metadataSchema, this.metadataElement);
        }
    }


    private void loadDataPackage(String doi)  {
        if(doi == null) {
            log.error("Unable to load data package - no DOI provided");
            return;
        }
        try {
            this.dataPackage = (Item) doiService.resolve(context, doi, new String[] {});
        } catch (IdentifierNotFoundException ex) {
            log.error("Identifier not found for package with DOI: " + doi, ex);
        } catch (IdentifierNotResolvableException ex) {
            log.error("Identifier not resolvable for package with DOI: " + doi, ex);
        }
        return;
    }

    private void loadDataFiles() {
        if (dataPackage == null) {
            log.error("Unable to load files - no data package loaded");
            return;
        }
        try {
            this.dataFiles = DryadWorkflowUtils.getDataFiles(context, dataPackage);
        } catch (SQLException ex) {
            log.error("Unable to get data files for package with ID: " + dataPackage.getID(), ex);
        }

    }
}
