/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest.stubs;

import java.io.Serializable;

/**
 *
 * @author moubarik
 */
public class StubSchema implements Serializable{
    private int id;
    private String name;
    private String namespace;

    public StubSchema(int id, String name, String namespace) {
        this.id = id;
        this.name = name;
        this.namespace = namespace;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
}
