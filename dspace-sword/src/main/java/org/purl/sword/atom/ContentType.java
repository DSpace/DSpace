/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.atom;

/**
 * Represents a content type for a text element. 
 * 
 * @author Neil Taylor
 */
public enum ContentType
{
    TEXT ("text"),
    HTML ("html"),
    XHTML ("xhtml");
    
    /**
     * String representation of the type. 
     */
    private final String type; 
    
    /**
     * Create a new instance and set the string 
     * representation of the type. 
     * 
     * @param type The type, expressed as a string. 
     */
    private ContentType(String type)
    {
        this.type = type;   
    }
    
    /**
     * Retrieve a string representation of this object.
     * 
     *  @return A string. 
     */
    @Override
    public String toString() { return this.type; }
}
