/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.lang.StringUtils;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Required;

import java.sql.SQLException;

/**
 * Class that can be used as a bean to use a certain metadata field in spring
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class MetadataFieldDescriptor {

    private String schema;

    private String element;

    private String qualifier;

    public String getSchema() {
        return schema;
    }

    @Required
    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getElement() {
        return element;
    }

    @Required
    public void setElement(String element) {
        this.element = element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public MetadataField getMetadataField(Context context) throws SQLException {
        MetadataField metadataField = MetadataField.findByElement(context, getMetadataSchema(context).getSchemaID(), getElement(), getQualifier());
        if(metadataField == null)
        {
            throw new IllegalStateException("Invalid metadata field: " + schema + "." + element + "." + qualifier);
        }
        return metadataField;
    }

    public MetadataSchema getMetadataSchema(Context context) throws SQLException {
        MetadataSchema metadataSchema = MetadataSchema.find(context, schema);
        if(metadataSchema == null)
        {
            throw new IllegalStateException("Invalid metadata schema: " + schema);
        }
        return metadataSchema;

    }

    @Override
    public String toString() {
        String result = schema + "." + element;
        if(StringUtils.isNotBlank(qualifier)){
            result += "." + qualifier;
        }
        return result;
    }
}
