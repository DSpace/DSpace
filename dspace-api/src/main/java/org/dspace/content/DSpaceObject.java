/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;

/**
 * Abstract base class for DSpace objects
 */
public abstract class DSpaceObject
{
    // accumulate information to add to "detail" element of content Event,
    // e.g. to document metadata fields touched, etc.
    private StringBuffer eventDetails = null;

    /**
     * Reset the cache of event details.
     */
    protected void clearDetails()
    {
        eventDetails = null;
    }

    /**
     * Add a string to the cache of event details.  Automatically
     * separates entries with a comma.
     * Subclass can just start calling addDetails, since it creates
     * the cache if it needs to.
     * @param d detail string to add.
     */
    protected void addDetails(String d)
    {
        if (eventDetails == null)
        {
            eventDetails = new StringBuffer(d);
        }
        else
        {
            eventDetails.append(", ").append(d);
        }
    }

    /**
     * @return summary of event details, or null if there are none.
     */
    protected String getDetails()
    {
        return (eventDetails == null ? null : eventDetails.toString());
    }

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
     * Get the Handle of the object. This may return <code>null</code>
     * 
     * @return Handle of the object, or <code>null</code> if it doesn't have
     *         one
     */
    public abstract String getHandle();

    /**
     * Get a proper name for the object. This may return <code>null</code>.
     * Name should be suitable for display in a user interface.
     *
     * @return Name for the object, or <code>null</code> if it doesn't have
     *         one
     */
    public abstract String getName();

    /**
     * Generic find for when the precise type of a DSO is not known, just the
     * a pair of type number and database ID.
     *
     * @param context - the context
     * @param type - type number
     * @param id - id within table of type'd objects
     * @return the object found, or null if it does not exist.
     * @throws SQLException only upon failure accessing the database.
     */
    public static DSpaceObject find(Context context, int type, int id)
        throws SQLException
    {
        switch (type)
        {
            case Constants.BITSTREAM : return Bitstream.find(context, id);
            case Constants.BUNDLE    : return Bundle.find(context, id);
            case Constants.ITEM      : return Item.find(context, id);
            case Constants.COLLECTION: return Collection.find(context, id);
            case Constants.COMMUNITY : return Community.find(context, id);
            case Constants.GROUP     : return Group.find(context, id);
            case Constants.EPERSON   : return EPerson.find(context, id);
            case Constants.SITE      : return Site.find(context, id);
        }
        return null;
    }

    /**
     * Return the dspace object where an ADMIN action right is sufficient to
     * grant the initial authorize check.
     * <p>
     * Default behaviour is ADMIN right on the object grant right on all other
     * action on the object itself. Subclass should override this method as
     * need.
     * 
     * @param action
     *            ID of action being attempted, from
     *            <code>org.dspace.core.Constants</code>. The ADMIN action is
     *            not a valid parameter for this method, an
     *            IllegalArgumentException should be thrown
     * @return the dspace object, if any, where an ADMIN action is sufficient to
     *         grant the original action
     * @throws SQLException
     * @throws IllegalArgumentException
     *             if the ADMIN action is supplied as parameter of the method
     *             call
     */
    public DSpaceObject getAdminObject(int action) throws SQLException
    {
        if (action == Constants.ADMIN)
        {
            throw new IllegalArgumentException("Illegal call to the DSpaceObject.getAdminObject method");
        }
        return this;
    }

    /**
     * Return the dspace object that "own" the current object in the hierarchy.
     * Note that this method has a meaning slightly different from the
     * getAdminObject because it is independent of the action but it is in a way
     * related to it. It defines the "first" dspace object <b>OTHER</b> then the
     * current one, where allowed ADMIN actions imply allowed ADMIN actions on
     * the object self.
     * 
     * @return the dspace object that "own" the current object in
     *         the hierarchy
     * @throws SQLException
     */
    public DSpaceObject getParentObject() throws SQLException
    {
        return null;
    }

    public String toString() {
        return Constants.typeText[getType()] + "." + getID();
    }

    /**
     * expects a two part string as parameter: TYPE.ID, where TYPE is a DSpaceObject type and
     * ID is the object id, handle,  group name, or Eperson's netid
     *
     * returns DSpaceObject that corresponds to string or null
     * @param str    must have format TYPE.ID
     * @return
     */
    public static DSpaceObject fromString(Context c, String str) throws SQLException {
        if (str == null) {
            return null;
        }
        String[] splits = StringUtils.split(str, '.');
        if (splits.length != 2) {
            // bad format
            return null;
        }
        int typeId = Constants.getTypeID(splits[0].toUpperCase());
        if (typeId == -1) {
            // not one of the DSPaceObject types
            return null;
        }

        try {
            int id = Integer.parseInt(splits[1]);
            return DSpaceObject.find(c, typeId, id);
        } catch (NumberFormatException ne) {
            switch (typeId) {
                case Constants.ITEM:
                case Constants.COLLECTION:
                case Constants.COMMUNITY:
                    return HandleManager.resolveToObject(c, splits[1]);
                case Constants.EPERSON:
                    return EPerson.findByNetid(c, splits[1]);
                case Constants.GROUP:
                    return Group.findByName(c, splits[1]);
                default:
                    return null;
            }
        }
    }

}
