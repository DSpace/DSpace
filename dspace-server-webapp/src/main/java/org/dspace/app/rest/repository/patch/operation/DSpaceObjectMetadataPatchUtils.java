/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.io.IOException;
import java.sql.SQLException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Util class for shared methods between the Metadata Operations
 * @author Maria Verdonck (Atmire) on 18/11/2019
 */
@Component
public final class DSpaceObjectMetadataPatchUtils {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MetadataFieldService metadataFieldService;

    /**
     * Path in json body of patch that uses these metadata operations
     */
    protected static final String OPERATION_METADATA_PATH = "/metadata";

    private DSpaceObjectMetadataPatchUtils() {
    }

    /**
     * Extract metadataValue from Operation by parsing the json and mapping it to a MetadataValueRest
     * @param operation     Operation whose value is begin parsed
     * @return MetadataValueRest extracted from json in operation value
     */
    protected MetadataValueRest extractMetadataValueFromOperation(Operation operation) {
        MetadataValueRest metadataValue = null;
        try {
            if (operation.getValue() != null) {
                if (operation.getValue() instanceof JsonValueEvaluator) {
                    JsonNode valueNode = ((JsonValueEvaluator) operation.getValue()).getValueNode();
                    if (valueNode.isArray()) {
                        metadataValue = objectMapper.treeToValue(valueNode.get(0), MetadataValueRest.class);
                    } else {
                        metadataValue = objectMapper.treeToValue(valueNode, MetadataValueRest.class);
                    }
                }
                if (operation.getValue() instanceof String) {
                    String valueString = (String) operation.getValue();
                    metadataValue = new MetadataValueRest();
                    metadataValue.setValue(valueString);
                }
            }
        } catch (IOException e) {
            throw new DSpaceBadRequestException("IOException in " +
                    "DspaceObjectMetadataOperation.extractMetadataValueFromOperation trying to map json from " +
                    "operation.value to MetadataValue class.", e);
        }
        if (metadataValue == null) {
            throw new DSpaceBadRequestException("Could not extract MetadataValue Object from Operation");
        }
        return metadataValue;
    }

    /**
     * Extracts the mdField String (schema.element.qualifier) from the operation and returns it
     * @param operation The patch operation
     * @return The mdField (schema.element.qualifier) patch is being performed on
     */
    protected String extractMdFieldStringFromOperation(Operation operation) {
        String mdElement = StringUtils.substringBetween(operation.getPath(), OPERATION_METADATA_PATH + "/", "/");
        if (mdElement == null) {
            mdElement = StringUtils.substringAfter(operation.getPath(), OPERATION_METADATA_PATH + "/");
            if (mdElement == null) {
                throw new DSpaceBadRequestException("No metadata field string found in path: " + operation.getPath());
            }
        }
        return mdElement;
    }

    /**
     * Converts a metadataValue (database entity) to a REST equivalent of it
     * @param md    Original metadataValue
     * @return The REST equivalent
     */
    protected MetadataValueRest convertMdValueToRest(MetadataValue md) {
        MetadataValueRest dto = new MetadataValueRest();
        dto.setAuthority(md.getAuthority());
        dto.setConfidence(md.getConfidence());
        dto.setLanguage(md.getLanguage());
        dto.setPlace(md.getPlace());
        dto.setValue(md.getValue());
        return dto;
    }

    /**
     * Extracts which property of the metadata is being changed in the replace patch operation
     * @param partsOfPath   Parts of the path of the operation, separated with /
     * @return The property that is begin replaced of the metadata
     */
    protected String extractPropertyOfMdFromPath(String[] partsOfPath) {
        return (partsOfPath.length > 4) ? partsOfPath[4] : null;
    }

    /**
     * Extracts the new value of the metadata from the operation for the replace patch operation
     * @param operation     The patch operation
     * @return The new value of the metadata being replaced in the patch operation
     */
    protected String extractNewValueOfMd(Operation operation) {
        if (operation.getValue() instanceof String) {
            return (String) operation.getValue();
        }
        return null;
    }

    /**
     * Retrieves metadataField based on the metadata element found in the operation
     * @param context       Context the retrieve metadataField from service with string
     * @param operation     Operation of the patch
     * @return              The metadataField corresponding to the md element string of the operation
     */
    protected MetadataField getMetadataField(Context context, Operation operation) throws SQLException {
        String mdElement = this.extractMdFieldStringFromOperation(operation);
        return metadataFieldService.findByString(context, mdElement, '.');
    }

    /**
     * Retrieved the index from the path of the patch operation, if one can be found
     * @param path          The string from the operation
     * @return              The index in the path if there is one (path ex: /metadata/dc.title/1 (1 being the index))
     */
    protected String getIndexFromPath(String path) {
        String[] partsOfPath = path.split("/");
        // Index of md being patched
        return (partsOfPath.length > 3) ? partsOfPath[3] : null;
    }

    protected void checkMetadataFieldNotNull(MetadataField metadataField) {
        if (metadataField == null) {
            throw new DSpaceBadRequestException("There was no metadataField found in path of operation");
        }
    }
}
