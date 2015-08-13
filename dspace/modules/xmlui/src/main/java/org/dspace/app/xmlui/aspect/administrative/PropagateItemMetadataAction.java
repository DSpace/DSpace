/*
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Propagate metadata to data files
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PropagateItemMetadataAction extends AbstractAction {

    private static Logger log = Logger.getLogger(PropagateItemMetadataAction.class);
    private static final Integer NOT_FOUND = -1;
    private Request request;
    private Context context;

    private Integer dataPackageId = NOT_FOUND;
    private String metadataField;

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        this.request = ObjectModelHelper.getRequest(objectModel);
        this.context = ContextUtil.obtainContext(objectModel);

        // if submit_update is in the parameters, do the work
        if(isFormSubmission()) {
            handleFormSubmission();
        }
        return null;
    }

    private void handleFormSubmission() throws Exception {
        // get metadata field
        String raw_field = getMetadataField();
        if(raw_field == null) {
            throw new Exception("Metadata field not found");
        }
        this.metadataField = formatMetadataField(raw_field);
        // get data package id
        this.dataPackageId = getDataPackageId();
        if(this.dataPackageId == NOT_FOUND) {
            throw new Exception("Data package ID not found");
        }
        // get file id
        Integer[] dataFileIds = getDataFileIds();
        if(dataFileIds != null) {
            propagateMetadata(dataFileIds);
        }

    }

    private void propagateMetadata(Integer[] dataFileIds) throws SQLException, AuthorizeException {
        for(Integer dataFileId : dataFileIds) {
            try {
                propagateMetadata(dataFileId);
            } catch (SQLException ex) {
                log.error("Error propagating metadata to file " + dataFileId);
                throw ex;
            }
        }
    }

    private void propagateMetadata(Integer dataFileId) throws SQLException, AuthorizeException {
        // get the metadata value from the package
        Item dataPackage = Item.find(context, dataPackageId);
        Item dataFile = Item.find(context, dataFileId);
        DCValue[] values = dataPackage.getMetadata(metadataField);
        if(values.length > 0) {
            DCValue first = values[0];
            // Clear out any existing metadata for this field
            dataFile.clearMetadata(first.schema, first.element, first.qualifier, Item.ANY);
            for(DCValue value : values) {
                dataFile.addMetadata(value.schema, value.element, value.qualifier, value.language, value.value, value.authority, value.confidence);
            }
            dataFile.update();
        }
    }

    private Boolean isFormSubmission() {
        if(this.request == null) {
            return false;
        }

        if(this.request.getParameter("submit_update") != null) {
            return true;
        } else {
            return false;
        }
    }

    private String getMetadataField() {
        String metadataFieldString = request.getParameter("metadata_field_name");
        if(metadataFieldString == null) {
            return null;
        }
        return metadataFieldString;
    }

    private Integer getDataPackageId() {
        String dataPackageIdString = request.getParameter("package_item_id");
        if(dataPackageIdString == null) {
            return NOT_FOUND;
        }
        Integer dataPackageId = Integer.valueOf(dataPackageIdString);
        return dataPackageId;
    }

    private Integer[] getDataFileIds() {
        String[] dataFileIdStrings = request.getParameterValues("data_file_ids[]");
        if(dataFileIdStrings == null) {
            return null;
        }
        Integer dataFileIds[] = new Integer[dataFileIdStrings.length];
        for(int i=0;i<dataFileIdStrings.length;i++) {
            dataFileIds[i] = Integer.valueOf(dataFileIdStrings[i]);
        }
        return dataFileIds;
    }

    // converts _ to .
    private static String formatMetadataField(String field) {
        if(field != null) {
            return field.replace('_', '.');
        } else {
            return "";
        }
    }

}
