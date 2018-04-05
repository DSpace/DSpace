/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "object")
public class HierarchyObject
{
	//id may be a numeric id or a uuid depending on the version of DSpace
    private String id;
    private String name;
    private String handle;

    public HierarchyObject(){
    }
    
    public HierarchyObject(String id, String name, String handle){
    	setId(id);
    	setName(name);
    	setHandle(handle);
    }
        
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }
}
