/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Use GSON to export the metadata schema registry to JSON
 * @author terry.brady@georgetown.edu
 */
public class Schema {
    private String prefix;
    private String namespace;
    private List<Field> fields;

    public String prefix() {return prefix;}
    public String namespace() {return namespace;}
    public List<Field> getFields() {return fields;}

    public void setPrefix(String prefix) {this.prefix = prefix;}
    public void setNamespace(String namespace) {this.namespace = namespace;}
    public void setFields(List<Field> fields) {this.fields = fields;}
    
    public Schema() {
    }
    public Schema(String prefix, String namespace) {
        this.prefix = prefix;
        this.namespace = namespace;
        this.fields = new ArrayList<Field>();
    }
    public void addField(String name, String element, String qualifier, String description) {
        fields.add(new Field(name, element, qualifier, description));
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("prefix:%s,namespace:%s", prefix, namespace));
        for(Field f: fields) {
            sb.append(String.format("%n\t%s",f));
        }
        return sb.toString();
    }
    
    static public HashMap<String,Field> getFields(Schema[] schemas) {
        HashMap<String,Field> fields = new HashMap<String,Field>();
        for(Schema schema: schemas) {
            for(Field field: schema.getFields()) {
                fields.put(field.name(), field);
            }
        }
        return fields;
    }
}
