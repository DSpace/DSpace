/*
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;
import org.dspace.workflow.DryadWorkflowUtils;
import org.xml.sax.SAXException;


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

        Request request = ObjectModelHelper.getRequest(objectModel);
        String metadataFieldName = request.getParameter("metadata_field_name");
        if(metadataFieldName == null) {
            log.error("Metadata Field Name not found in parameters");
            body.addDivision("error").setHead("Error - Metadata field name not found in parameters");
            return;
        }

        loadMetadataField(metadataFieldName);

        // Get the metadata for the package item.
        String packageDoi = request.getParameter("package_doi");
        if(packageDoi == null) {
            log.error("Package DOI not found in parameters");
            body.addDivision("error").setHead("Error - package DOI not found in parameters");
            return;
        }

        loadDataPackage(packageDoi);
        loadDataFiles();

        // what metadata field?
        // get the metadata values for the file, including the file name

        Division main = body.addInteractiveDivision("propagate", contextPath + "/admin/item/propagate-metadata", Division.METHOD_POST, "primary administrative item");
        main.setHead(T_title);
        // Show results if we updated anything
        String[] parameterValues = request.getParameterValues("data_file_ids[]");
        Boolean disableControls = false;
        if(parameterValues != null && parameterValues.length > 0) {
            int updatedFiles = parameterValues.length;
            Division notice = main.addDivision("updated-files", "ds-notice-div notice success");
            String plural = updatedFiles != 1 ? "s" : "";
            notice.addPara("The update was successfully propagated to  " + updatedFiles + " file" + plural + ".");
            disableControls = true;
        }

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
            String[] fileStringValues = getStringValues(fileValues);
            if(fileStringValues != null) {
                cell.addContent(StringUtils.join(fileStringValues, ", "));
            } else {
                cell.addContent(T_cell_no_value);
            }

            // New value - from package
            cell = fileRow.addCell();
            DCValue[] packageValues = dataPackage.getMetadata(this.metadataSchema, this.metadataElement, this.metadataQualifier, Item.ANY);
            String[] packageStringValues = getStringValues(packageValues);
            if(packageStringValues != null) {
                cell.addContent(StringUtils.join(packageStringValues, ", "));
            } else {
                cell.addContent(T_cell_no_value);
            }

            // Select - checkbox
            cell = fileRow.addCell();
            CheckBox checkbox = cell.addCheckBox("data_file_ids[]");
            checkbox.setLabel("Propagate");
            if(disableControls == false) {
                checkbox.addOption(dataFile.getID());
                checkbox.setOptionSelected(dataFile.getID());
            }
        }
        main.addHidden("package_item_id").setValue(dataPackage.getID());
        main.addHidden("package_doi").setValue(packageDoi);
        main.addHidden("metadata_field_name").setValue(metadataFieldName);
        Para actions = main.addPara();
        actions.addButton("submit_update", disableControls ? "disabled" : "").setValue(T_button_update);
        actions.addButton("submit_return").setValue(T_button_return);
    }
    
    private String[] getStringValues(DCValue dcValues[]) {
        if(dcValues.length > 0) {
            String[] stringValues = new String[dcValues.length];
            for(int i=0;i<dcValues.length;i++) {
                stringValues[i] = dcValues[i].value;
            }
            return stringValues;
        } else {
            return null;
        }
    }


    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);

        // This invokes magic popup transformation in XSL - "framing.popup"
        pageMeta.addMetadata("framing","popup").addContent("true");
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
                this.metadataQualifier = null;
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
    }

    private void loadDataFiles() {
        if (dataPackage == null) {
            log.error("Unable to load files - no data package loaded");
            return;
        }
        try {
            this.dataFiles = DryadWorkflowUtils.getDataFiles(context, dataPackage);
            // Sort by Item ID so that order does not change
            Arrays.sort(this.dataFiles, new Comparator<Item>() {
                @Override
                public int compare(Item t, Item t1) {
                    return t.getID() - t1.getID();
                }
            });
        } catch (SQLException ex) {
            log.error("Unable to get data files for package with ID: " + dataPackage.getID(), ex);
        }

    }

    @Override
    public void recycle() {
        this.metadataElement = null;
        this.metadataQualifier = null;
        this.metadataSchema = null;
        this.dataPackage = null;
        this.dataFiles = new Item[] {};
        super.recycle();
    }

}
