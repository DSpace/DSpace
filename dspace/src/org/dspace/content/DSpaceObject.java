package org.dspace.content;

public interface DSpaceObject
{
    /**
     * Get the type of this object, found in Constants
     */
    int getType();
    
    /**
     * Get the ID of this object
     */
    int getID();
}
