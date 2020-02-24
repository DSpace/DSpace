/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 Class for PATCH REPLACE operations on Dspace Objects' metadata
 * Usage: (can be done on other dso than Item also):
 * - REPLACE metadata (with schema.identifier.qualifier) value of a dso (here: Item)
 *      from existing value to new given value
 * <code>
 * curl -X PATCH http://${dspace.server.url}/api/core/items/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /metadata/schema.identifier.qualifier}", "value": "newMetadataValue"]'
 * </code>
 * @author Maria Verdonck (Atmire) on 18/11/2019
 */
@Component
public class DSpaceObjectMetadataReplaceOperation<R extends DSpaceObject> extends PatchOperation<R> {

    @Autowired
    DSpaceObjectMetadataPatchUtils metadataPatchUtils;

    @Override
    public R perform(Context context, R resource, Operation operation) throws SQLException {
        DSpaceObjectService dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(resource);
        MetadataField metadataField = metadataPatchUtils.getMetadataField(context, operation);
        String[] partsOfPath = operation.getPath().split("/");
        // Index of md being patched
        String indexInPath = (partsOfPath.length > 3) ? partsOfPath[3] : null;
        MetadataValueRest metadataValueToReplace = metadataPatchUtils.extractMetadataValueFromOperation(operation);
        // Property of md being altered
        String propertyOfMd = metadataPatchUtils.extractPropertyOfMdFromPath(partsOfPath);
        String newValueMdAttribute = metadataPatchUtils.extractNewValueOfMd(operation);

        replace(context, resource, dsoService, metadataField, metadataValueToReplace, indexInPath, propertyOfMd,
                newValueMdAttribute);
        return resource;
    }

    /**
     * Replaces metadata in the dso; 4 cases:
     *      * - If we replace everything: clears all metadata
     *      * - If we replace for a single field: clearMetadata on the field & add the new ones
     *      * - A single existing metadata value:
     *      * Retrieve the metadatavalue object & make alterations directly on this object
     *      * - A single existing metadata property:
     *      * Retrieve the metadatavalue object & make alterations directly on this object
     * @param context           context patch is being performed in
     * @param dso               dso being patched
     * @param dsoService        service doing the patch in db
     * @param metadataField     possible md field being patched (if null all md gets cleared)
     * @param metadataValue     value of md element
     * @param index             possible index of md being replaced
     * @param propertyOfMd      possible property of md being replaced
     * @param valueMdProperty   possible new value of property of md being replaced
     */
    private void replace(Context context, DSpaceObject dso, DSpaceObjectService dsoService, MetadataField metadataField,
                         MetadataValueRest metadataValue, String index, String propertyOfMd, String valueMdProperty) {
        // replace entire set of metadata
        if (metadataField == null) {
            this.replaceAllMetadata(context, dso, dsoService);
            return;
        }

        // replace all metadata for existing key
        if (index == null) {
            this.replaceMetadataFieldMetadata(context, dso, dsoService, metadataField, metadataValue);
            return;
        }
        // replace single existing metadata value
        if (propertyOfMd == null) {
            this.replaceSingleMetadataValue(dso, dsoService, metadataField, metadataValue, index);
            return;
        }
        // replace single property of exiting metadata value
        this.replaceSinglePropertyOfMdValue(dso, dsoService, metadataField, index, propertyOfMd, valueMdProperty);
    }

    /**
     * Clears all metadata of dso
     * @param context           context patch is being performed in
     * @param dso               dso being patched
     * @param dsoService        service doing the patch in db
     */
    private void replaceAllMetadata(Context context, DSpaceObject dso, DSpaceObjectService dsoService) {
        try {
            dsoService.clearMetadata(context, dso, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        } catch (SQLException e) {
            throw new DSpaceBadRequestException("SQLException in DspaceObjectMetadataOperation.replace trying to " +
                    "remove and replace metadata from dso.", e);
        }
    }

    /**
     * Replaces all metadata for an existing single mdField with new value(s)
     * @param context           context patch is being performed in
     * @param dso               dso being patched
     * @param dsoService        service doing the patch in db
     * @param metadataField     md field being patched
     * @param metadataValue     value of md element
     */
    private void replaceMetadataFieldMetadata(Context context, DSpaceObject dso, DSpaceObjectService dsoService,
                                              MetadataField metadataField, MetadataValueRest metadataValue) {
        try {
            dsoService.clearMetadata(context, dso, metadataField.getMetadataSchema().getName(),
                    metadataField.getElement(), metadataField.getQualifier(), Item.ANY);
            dsoService.addAndShiftRightMetadata(context, dso, metadataField.getMetadataSchema().getName(),
                    metadataField.getElement(), metadataField.getQualifier(), metadataValue.getLanguage(),
                    metadataValue.getValue(), metadataValue.getAuthority(), metadataValue.getConfidence(), -1);
        } catch (SQLException e) {
            throw new DSpaceBadRequestException("SQLException in DspaceObjectMetadataOperation.replace trying to " +
                    "remove and replace metadata from dso.", e);
        }
    }

    /**
     * Replaces metadata value of a single metadataValue object
     *      Retrieve the metadatavalue object & make alerations directly on this object
     * @param dso               dso being patched
     * @param dsoService        service doing the patch in db
     * @param metadataField     md field being patched
     * @param metadataValue     new value of md element
     * @param index             index of md being replaced
     */
    // replace single existing metadata value
    private void replaceSingleMetadataValue(DSpaceObject dso, DSpaceObjectService dsoService,
                                            MetadataField metadataField, MetadataValueRest metadataValue,
                                            String index) {
        try {
            List<MetadataValue> metadataValues = dsoService.getMetadata(dso,
                    metadataField.getMetadataSchema().getName(), metadataField.getElement(),
                    metadataField.getQualifier(), Item.ANY);
            int indexInt = Integer.parseInt(index);
            if (indexInt >= 0 && metadataValues.size() > indexInt
                    && metadataValues.get(indexInt) != null) {
                // Alter this existing md
                MetadataValue existingMdv = metadataValues.get(indexInt);
                existingMdv.setAuthority(metadataValue.getAuthority());
                existingMdv.setConfidence(metadataValue.getConfidence());
                existingMdv.setLanguage(metadataValue.getLanguage());
                existingMdv.setValue(metadataValue.getValue());
                dsoService.setMetadataModified(dso);
            } else {
                throw new UnprocessableEntityException("There is no metadata of this type at that index");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("This index (" + index + ") is not valid number.", e);
        }
    }

    /**
     * Replaces single property of a specific mdValue object
     * @param dso               dso being patched
     * @param dsoService        service doing the patch in db
     * @param metadataField     md field being patched
     * @param index             index of md being replaced
     * @param propertyOfMd      property of md being replaced
     * @param valueMdProperty   new value of property of md being replaced
     */
    private void replaceSinglePropertyOfMdValue(DSpaceObject dso, DSpaceObjectService dsoService,
                                                MetadataField metadataField,
                                                String index, String propertyOfMd, String valueMdProperty) {
        try {
            List<MetadataValue> metadataValues = dsoService.getMetadata(dso,
                    metadataField.getMetadataSchema().getName(), metadataField.getElement(),
                    metadataField.getQualifier(), Item.ANY);
            int indexInt = Integer.parseInt(index);
            if (indexInt >= 0 && metadataValues.size() > indexInt && metadataValues.get(indexInt) != null) {
                // Alter only asked propertyOfMd
                MetadataValue existingMdv = metadataValues.get(indexInt);
                if (propertyOfMd.equals("authority")) {
                    existingMdv.setAuthority(valueMdProperty);
                }
                if (propertyOfMd.equals("confidence")) {
                    existingMdv.setConfidence(Integer.valueOf(valueMdProperty));
                }
                if (propertyOfMd.equals("language")) {
                    existingMdv.setLanguage(valueMdProperty);
                }
                if (propertyOfMd.equals("value")) {
                    existingMdv.setValue(valueMdProperty);
                }
                dsoService.setMetadataModified(dso);
            } else {
                throw new UnprocessableEntityException("There is no metadata of this type at that index");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not all numbers are valid numbers. " +
                    "(Index and confidence should be nr)", e);
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return ((operation.getPath().startsWith(metadataPatchUtils.OPERATION_METADATA_PATH)
                || operation.getPath().equals(metadataPatchUtils.OPERATION_METADATA_PATH))
                && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && objectToMatch instanceof DSpaceObject);
    }
}
