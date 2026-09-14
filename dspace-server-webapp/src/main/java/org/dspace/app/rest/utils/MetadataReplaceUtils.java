/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.sql.SQLException;

import jakarta.annotation.Nullable;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Utility class for replace operations performed on metadata.
 * <br><br>
 * Will use DSpaceObjectService#replaceMetadata in order to always register 2 audit logs of type MODIFY_METADATA, one
 * of type ADD and other of type REMOVE.
 */
public class MetadataReplaceUtils {
    private MetadataReplaceUtils() {}

    /**
     * Replaces a(all) property(ies) in a metadata value.
     * @param context DSpace context.
     * @param dsoService Service of the DSO type having the metadata modified.
     * @param dso DSO of the metadata being modified.
     * @param mdField Qualified name of the metadata field being modified.
     * @param existingMdv Current metadata value that will be modified.
     * @param newMdv REST object representing the modified version of the metadata.
     * @param index Place of the metadata value being modified in {@code existingMdv}.
     * @param propertyOfMd Name of the metadata value property to modify. All properties if null.
     * @param newPropertyValue Value of the specific property to modify. Only meaningful if propertyOfMd != null.
     * @param <DSO> Type of the DSO having the metadata modified.
     * @throws SQLException If replacement fails.
     */
    public static <DSO extends DSpaceObject> void replaceValue(
        Context context,
        DSpaceObjectService<DSO> dsoService,
        DSO dso,
        String mdField,
        MetadataValue existingMdv,
        MetadataValueRest newMdv,
        int index,
        @Nullable String propertyOfMd,
        @Nullable String newPropertyValue
    ) throws SQLException {
        String[] metadata = Utils.tokenize(mdField);

        // Gets the value of each property.
        // Keeps the old one when just a single property is being modified (when 'propertyOfMd' is not null).
        String authority = propertyOfMd == null
            ? newMdv.getAuthority()
            : ("authority".equals(propertyOfMd) ? newPropertyValue : existingMdv.getAuthority());
        int confidence = propertyOfMd == null
            ? newMdv.getConfidence()
            : (
            "confidence".equals(propertyOfMd) && newPropertyValue != null
            ? Integer.parseInt(newPropertyValue)
            : existingMdv.getConfidence()
        );
        String language = propertyOfMd == null
            ? newMdv.getLanguage()
            : ("language".equals(propertyOfMd) ? newPropertyValue : existingMdv.getLanguage());
        String value = propertyOfMd == null
            ? newMdv.getValue()
            : ("value".equals(propertyOfMd) ? newPropertyValue : existingMdv.getValue());

        if (newMdv.getSecurityLevel() == null) {
            dsoService.replaceMetadata(
                context, dso,
                metadata[0], metadata[1], metadata[2],
                language, value, authority, confidence, index
            );
        } else {
            dsoService.replaceSecuredMetadata(
                context, dso,
                metadata[0], metadata[1], metadata[2],
                language, value, authority, confidence, index, newMdv.getSecurityLevel()
            );
        }
    }

    /**
     * Replaces all properties in a metadata value.
     * @param context DSpace context.
     * @param dsoService Service of the DSO type having the metadata modified.
     * @param dso DSO of the metadata being modified.
     * @param mdField Qualified name of the metadata field being modified.
     * @param existingMdv Current metadata value that will be modified.
     * @param newMdv REST object representing the modified version of the metadata.
     * @param index Place of the metadata value being modified in {@code existingMdv}.
     * @param <DSO> Type of the DSO having the metadata modified.
     * @throws SQLException If replacement fails.
     */
    public static <DSO extends DSpaceObject> void replaceValue(
        Context context,
        DSpaceObjectService<DSO> dsoService,
        DSO dso,
        String mdField,
        MetadataValue existingMdv,
        MetadataValueRest newMdv,
        int index
    ) throws SQLException {
        replaceValue(context, dsoService, dso, mdField, existingMdv, newMdv, index, null, null);
    }
}
