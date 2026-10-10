/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.utils.MetadataReplaceUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;

/**
 * Submission "replace" common PATCH operation.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public abstract class MetadataValueReplacePatchOperation<DSO extends DSpaceObject>
    extends ReplacePatchOperation<MetadataValueRest> {

    @Override
    protected Class<MetadataValueRest[]> getArrayClassForEvaluation() {
        return MetadataValueRest[].class;
    }

    @Override
    protected Class<MetadataValueRest> getClassForEvaluation() {
        return MetadataValueRest.class;
    }

    /**
     * Replaces values of an existing metadata value.
     * @param context DSpace context.
     * @param dso DSO that will have metadata modified.
     * @param mdField Qualified name of the metadata field to modify.
     * @param existingMdvs Current metadata values of {@code dso} for the field {@code mdField}.
     * @param newMdv New values for the metadata value being modified.
     * @param mdvIndex Index of the metadata value in {@code existingMdvs} to modify.
     * @throws SQLException
     */
    protected void replaceValue(Context context,
                                DSO dso,
                                String mdField,
                                List<MetadataValue> existingMdvs,
                                MetadataValueRest newMdv,
                                int mdvIndex
    ) throws SQLException {
        MetadataValue existingMdv = existingMdvs.get(mdvIndex);
        DSpaceObjectService<DSO> dsoService = getDSpaceObjectService();
        MetadataReplaceUtils.replaceValue(context, dsoService, dso, mdField, existingMdv, newMdv, mdvIndex);
    }

    protected void setDeclaredField(Context context,
                                    DSO source,
                                    Object value,
                                    String metadata,
                                    String namedField,
                                    List<MetadataValue> metadataByMetadataString,
                                    int index
    ) throws IllegalAccessException, SQLException {
        // check field
        String raw = (String) value;
        for (Field field : MetadataValueRest.class.getDeclaredFields()) {
            JsonProperty jsonP = field.getDeclaredAnnotation(JsonProperty.class);
            if (jsonP != null && jsonP.access().equals(Access.READ_ONLY)) {
                continue;
            } else {
                if (field.getName().equals(namedField)) {
                    int idx = 0;
                    MetadataValueRest obj = new MetadataValueRest();
                    for (MetadataValue mv : metadataByMetadataString) {

                        if (idx == index) {
                            obj.setAuthority(mv.getAuthority());
                            obj.setConfidence(mv.getConfidence());
                            obj.setLanguage(mv.getLanguage());
                            obj.setValue(mv.getValue());
                            if (field.getType().isAssignableFrom(Integer.class)) {
                                obj.setConfidence(Integer.parseInt(raw));
                            } else {
                                boolean accessible = field.isAccessible();
                                field.setAccessible(true);
                                field.set(obj, raw);
                                field.setAccessible(accessible);
                            }
                            break;
                        }

                        idx++;
                    }
                    replaceValue(context, source, metadata, metadataByMetadataString, obj, index);
                }
            }
        }
    }

    protected abstract DSpaceObjectService<DSO> getDSpaceObjectService();
}
