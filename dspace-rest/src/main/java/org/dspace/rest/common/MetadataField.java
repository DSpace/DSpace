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
 * Metadata field representation
 * @author Terry Brady, Georgetown University.
 */
@XmlRootElement(name = "field")
public class MetadataField {
	private int fieldId;
    private String name;
    private String element;
    private String qualifier;
    private String description;
    
    private MetadataSchema parentSchema;

    @XmlElement(required = true)
    private ArrayList<String> expand = new ArrayList<String>();

    public MetadataField(){}

    public MetadataField(org.dspace.content.MetadataSchema schema, org.dspace.content.MetadataField field, String expand, Context context) throws SQLException, WebApplicationException{
        setup(schema, field, expand, context);
    }

    private void setup(org.dspace.content.MetadataSchema schema, org.dspace.content.MetadataField field, String expand, Context context) throws SQLException{
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(schema.getName());
        sb.append(".");
        sb.append(field.getElement());
        if (field.getQualifier()!=null) {
            sb.append(".");
            sb.append(field.getQualifier());            
        }
        
        this.setName(sb.toString());
        this.setFieldId(field.getID());
        this.setElement(field.getElement());
        this.setQualifier(field.getQualifier());
        this.setDescription(field.getScopeNote());
        
        if (expandFields.contains("parentSchema") || expandFields.contains("all")) {
        	this.addExpand("parentSchema");
        	parentSchema = new MetadataSchema(schema, "", context);
        }
    }

    public void setParentSchema(MetadataSchema schema) {
    	this.parentSchema = schema;
    }
    
    public MetadataSchema getParentSchema() {
    	return this.parentSchema;
    }
    
    public void setFieldId(int fieldId) {
        this.fieldId = fieldId;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setElement(String element) {
        this.element = element;
    }
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getFieldId() {
        return fieldId;
    }
    public String getName() {
        return name;
    }
    
    public String getQualifier() {
        return qualifier;
    }
    public String getElement() {
        return element;
    }
    public String getDescription() {
        return description;
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
