/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

/**
 * Use GSON to export the metadata field registry to JSON
 * @author terry.brady@georgetown.edu
 */
public class Field {
    public static String makeName(String prefix, String element, String qualifier) {
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(".");
        sb.append(element);
        if (qualifier != null) {
            sb.append(".");
            sb.append(qualifier);
        }
        return sb.toString();
    }

    private String name;
    private String element;
    private String qualifier;
    private String description;
    
    public String name() {return name;}
    public String element() {return element;}
    public String qualifier() {return qualifier;}
    public String description() {return description;}
    
    public void setName(String name) {this.name = name;}
    public void setElement(String element) {this.name = element;}
    public void setQualifier(String qualifier) {this.name = qualifier;}
    public void setDescription(String description) {this.name = description;}

    public Field() {
    }
    public Field(String name, String element, String qualifier, String description) {
        this.name = name;
        this.element = element;
        this.qualifier = qualifier;
        this.description = description;
    }
    
    public String toString() {
        return String.format("name:%s,element:%s,qualifier:%s,description:%s", name, element, qualifier, description == null ? null : description.replaceAll("\\s+", " "));
    }
}
