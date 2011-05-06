/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * This interface represents all structural wing elements.
 * 
 * There are two broad types of wing elements: structural and metadata. The
 * metadata elements describe information about the page, while structural
 * elements describe how to layout the page.
 * 
 * @author Scott Phillips
 */
public interface StructuralElement
{
    /** The name of the 'name' attribute */
    public static final String A_NAME = "n";
    
    /** The name of the id attribute */
    public static final String A_ID = "id";
    
    /** The name of the render attribute */
    public static final String A_RENDER = "rend";
}
