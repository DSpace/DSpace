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
 * Metadata field representation
 */
@XmlRootElement(name = "field")
public class MetadataField {
    private static Logger log = Logger.getLogger(MetadataField.class);

    private String name;
    private String element;
    private String qualifier;
    private String description;

    public MetadataField(){}

    public MetadataField(org.dspace.content.MetadataSchema schema, org.dspace.content.MetadataField field, Context context) throws SQLException, WebApplicationException{
        setup(schema, field, context);
    }

    private void setup(org.dspace.content.MetadataSchema schema, org.dspace.content.MetadataField field, Context context) throws SQLException{
        StringBuilder sb = new StringBuilder();
        sb.append(schema.getName());
        sb.append(".");
        sb.append(field.getElement());
        if (field.getQualifier()!=null) {
            sb.append(".");
            sb.append(field.getQualifier());            
        }
        
        this.setName(sb.toString());
        this.setElement(field.getElement());
        this.setQualifier(field.getQualifier());
        this.setDescription(field.getScopeNote());
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
}
