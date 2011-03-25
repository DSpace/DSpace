/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.configuration;

/**
 * This class represents an Aspect in the XMLUI system.
 * 
 * @author Scott Phillips
 */

public class Aspect
{

    /** A unique name for this Aspect */
    private final String name;
    /** The directory path */
    private final String path;
 
    /**
     * Create a new Aspect with the given name, and path.
     * 
     * @param name A unique name for the Aspect.
     * @param path The directory path to the Aspect.
     */
    public Aspect(String name,String path) {
        this.path= path;
        this.name = name;
    }
    
    /**
     * 
     * @return The Aspects unique name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * 
     * @return The Aspect's directory path.
     */
    public String getPath() {
        return path;
    }
    
  
    
}
