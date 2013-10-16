/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest.stubs;

import java.io.Serializable;
import org.dspace.content.MetadataSchema;

/**
 *
 * @author moubarik
 */
public class StubMetadata implements Serializable{
    private int id;
    private String schema;
    private String element;
    private String qualifier;
    private String scopeNote;

    public StubMetadata(int id, String schema, String element, String qualifier, String scopeNote) {
        this.id = id;
        this.schema = schema;
        this.element = element;
        this.qualifier = qualifier;
        this.scopeNote = scopeNote;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public String getScopeNote() {
        return scopeNote;
    }

    public void setScopeNote(String scopeNote) {
        this.scopeNote = scopeNote;
    }
    
}
