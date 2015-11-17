/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used to handle/determine status of REST API.
 * Mainly to know your authentication status
 *
 */
@XmlRootElement(name = "collection")
public class HierarchyCollection extends HierarchyObject
{
    public HierarchyCollection(){
    }
    
    public HierarchyCollection(String id, String name, String handle){
    	super(id, name, handle);
    }
}
