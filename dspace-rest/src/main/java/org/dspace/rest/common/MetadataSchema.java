/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.dspace.core.Context;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Metadata schema representation
 * @author Terry Brady, Georgetown University.
 */
@XmlRootElement(name = "schema")
public class MetadataSchema {

	private int schemaID;
    private String prefix;
    private String namespace;
    
    @XmlElement(required = true)
    private ArrayList<String> expand = new ArrayList<String>();

    @XmlElement(name = "fields", required = true)
    private List<MetadataField> fields = new ArrayList<MetadataField>();

    public MetadataSchema(){}

    public MetadataSchema(org.dspace.content.MetadataSchema schema, String expand, Context context) throws SQLException, WebApplicationException{
        setup(schema, expand, context);
    }

    private void setup(org.dspace.content.MetadataSchema schema, String expand, Context context) throws SQLException{
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }
        this.setSchemaID(schema.getSchemaID());
        this.setPrefix(schema.getName());
        this.setNamespace(schema.getNamespace());
        if (expandFields.contains("fields") || expandFields.contains("all")) {
            org.dspace.content.MetadataField[] fields = org.dspace.content.MetadataField.findAllInSchema(context, schema.getSchemaID());
            this.addExpand("fields");
            for(org.dspace.content.MetadataField field: fields) {
                this.fields.add(new MetadataField(schema, field, "", context));
            }        	
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
    public int getSchemaID()
    {
        return this.schemaID;
    }

    public void setSchemaID(int schemaID)
    {
        this.schemaID = schemaID;
    }
    public List<MetadataField> getMetadataFields() {
        return fields;
    }
    public List<String> getExpand() {
        return expand;
    }

    public void setExpand(ArrayList<String> expand) {
        this.expand = expand;
    }

    public void addExpand(String expandableAttribute) {
        this.expand.add(expandableAttribute);
    }
}
