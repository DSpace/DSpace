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
    /**
     * Replaces information in a metadata value.
     * @param context DSpace context.
     * @param dsoService Service of the DSO type having the metadata modified.
     * @param dso DSO of the metadata being modifid.
     * @param mdField Qualified name of the metadata field being modified.
     * @param existingMdv Current metadata value that will be modified.
     * @param newMdv REST object representing the modified version of the metadata.
     * @param index Place of the metadata value being modified.
     * @param propertyOfMd Name of the metadata value property to modify. All properties if null.
     * @param <DSO> Type of the DSO having the metadata modified.
     * @throws SQLException
     */
    public static <DSO extends DSpaceObject> void replaceValue(
        Context context,
        DSpaceObjectService<DSO> dsoService,
        DSO dso,
        String mdField,
        MetadataValue existingMdv,
        MetadataValueRest newMdv,
        int index,
        @Nullable String propertyOfMd
    ) throws SQLException {
        String[] metadata = Utils.tokenize(mdField);

        // Gets the value of each property.
        // Keeps the old one when just a single property is being modified (when 'propertyOfMd' is not null).
        String authority = propertyOfMd == null || "authority".equals(propertyOfMd)
            ? newMdv.getAuthority()
            : existingMdv.getAuthority();
        int confidence = propertyOfMd == null || "confidence".equals(propertyOfMd)
            ? newMdv.getConfidence()
            : existingMdv.getConfidence();
        String language = propertyOfMd == null || "language".equals(propertyOfMd)
            ? newMdv.getLanguage()
            : existingMdv.getLanguage();
        final var value = propertyOfMd == null || "value".equals(propertyOfMd)
            ? newMdv.getValue()
            : existingMdv.getValue();

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
     * Replaces information in a metadata value.
     * @see MetadataReplaceUtils#replaceValue(Context, DSpaceObjectService, DSpaceObject, String, MetadataValue, MetadataValueRest, int, String) Same as this method, with propertyOfMd null.
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
        replaceValue(context, dsoService, dso, mdField, existingMdv, newMdv, index, null);
    }
}
