/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Terry Brady, Georgetown University.
 * Metadata schema representation
 */
@XmlRootElement(name = "schema")
public class MetadataSchema {
    private static Logger log = Logger.getLogger(MetadataField.class);

    private String prefix;
    private String namespace;
    
    @XmlElement(name = "fields", required = true)
    private List<MetadataField> fields = new ArrayList<MetadataField>();

    public MetadataSchema(){}

    public MetadataSchema(org.dspace.content.MetadataSchema schema, Context context) throws SQLException, WebApplicationException{
        setup(schema, context);
    }

    private void setup(org.dspace.content.MetadataSchema schema, Context context) throws SQLException{
        this.setPrefix(schema.getName());
        this.setNamespace(schema.getNamespace());
        org.dspace.content.MetadataField[] fields = org.dspace.content.MetadataField.findAllInSchema(context, schema.getSchemaID());
        for(org.dspace.content.MetadataField field: fields) {
            this.fields.add(new MetadataField(schema, field, context));
        }
    
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
    public String getPrefix() {
        return prefix;
    }    
    public String getNamespace() {
        return namespace;
    }
    public List<MetadataField> getMetadataFields() {
        return fields;
    }
}
