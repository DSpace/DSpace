package org.dspace.content;

public abstract class DSpaceObject
{
    /**
     * Get the type of this object, found in Constants
     *
     * @return type of the object
     */
    public abstract int getType();
    
    /**
     * Get the internal ID (database primary key) of this object
     *
     * @return internal ID of object
     */
    public abstract int getID();

    /**
     * Get the Handle of the object.  This may return <code>null</code>
     *
     * @return  Handle of the object, or <code>null</code> if it doesn't have
     *          one
     */
    public abstract String getHandle();
}
