/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Class representing a ResourcePolicy
 * 
 * @author David Stuve
 * @version $Revision$
 */
public class ResourcePolicy
{

    public static String TYPE_SUBMISSION = "TYPE_SUBMISSION";
    public static String TYPE_WORKFLOW = "TYPE_WORKFLOW";
    public static String TYPE_CUSTOM= "TYPE_CUSTOM";
    public static String TYPE_INHERITED= "TYPE_INHERITED";



    /** log4j logger */
    private static Logger log = Logger.getLogger(ResourcePolicy.class);

    /** Our context */
    private Context myContext;

    /** The row in the table representing this object */
    private TableRow myRow;

    /**
     * Construct an ResourcePolicy
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    ResourcePolicy(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;
    }

    /**
     * Return true if this object equals obj, false otherwise.
     * 
     * @param obj
     * @return true if ResourcePolicy objects are equal
     */
    @Override
    public boolean equals(Object obj)
    {
        try
        {
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final ResourcePolicy other = (ResourcePolicy) obj;         
            if (this.getAction() != other.getAction())
            {
                return false;
            }
            if (this.getEPerson() != other.getEPerson() && (this.getEPerson() == null || !this.getEPerson().equals(other.getEPerson())))
            {
                return false;
            }
            if (this.getGroup() != other.getGroup() && (this.getGroup() == null || !this.getGroup().equals(other.getGroup())))
            {
                return false;
            }
            if (this.getStartDate() != other.getStartDate() && (this.getStartDate() == null || !this.getStartDate().equals(other.getStartDate())))
            {
                return false;
            }
            if (this.getEndDate() != other.getEndDate() && (this.getEndDate() == null || !this.getEndDate().equals(other.getEndDate())))
            {
                return false;
            }    
            return true;
        }
        catch (SQLException ex)
        {
            log.error("Error while comparing ResourcePolicy objects", ex);
        }
        return false;
    }

    /**
     * Return a hash code for this object.
     *
     * @return int hash of object
     */
    @Override
    public int hashCode()
    {
        int hash = 7;
        try
        {
            hash = 19 * hash + this.getAction();
            hash = 19 * hash + (this.getEPerson() != null? this.getEPerson().hashCode():0);
            hash = 19 * hash + (this.getGroup() != null? this.getGroup().hashCode():0);
            hash = 19 * hash + (this.getStartDate() != null? this.getStartDate().hashCode():0);
            hash = 19 * hash + (this.getEndDate() != null? this.getEndDate().hashCode():0);
            hash = 19 * hash + (this.getEPerson() != null? this.getEPerson().hashCode():0);
        }
        catch (SQLException ex)
        {
            log.error("Error generating hascode of ResourcePolicy", ex);
        }
        return hash;
    }

    /**
     * Get an ResourcePolicy from the database.
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the ResourcePolicy
     * 
     * @return the ResourcePolicy format, or null if the ID is invalid.
     */
    public static ResourcePolicy find(Context context, int id)
            throws SQLException
    {
        TableRow row = DatabaseManager.find(context, "ResourcePolicy", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new ResourcePolicy(context, row);
        }
    }

    /**
     * Create a new ResourcePolicy
     * 
     * @param context
     *            DSpace context object
     */
    public static ResourcePolicy create(Context context) throws SQLException,
            AuthorizeException
    {
        // FIXME: Check authorisation
        // Create a table row
        TableRow row = DatabaseManager.create(context, "ResourcePolicy");

        return new ResourcePolicy(context, row);
    }

    /**
     * Delete an ResourcePolicy
     *  
     */
    public void delete() throws SQLException
    {
        if(getResourceID() != -1 && getResourceType() != -1)
        {
            //A policy for a DSpace Object has been modified, fire a modify event on the DSpace object
            DSpaceObject dso = DSpaceObject.find(myContext, getResourceType(), getResourceID());
            if(dso != null)
            {
                dso.updateLastModified();
            }
        }

        // FIXME: authorizations
        // Remove ourself
        DatabaseManager.delete(myContext, myRow);
    }

    /**
     * Get the ResourcePolicy's internal identifier
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("policy_id");
    }

    /**
     * Get the type of the objects referred to by policy
     * 
     * @return type of object/resource
     */
    public int getResourceType()
    {
        return myRow.getIntColumn("resource_type_id");
    }

    /**
     * set both type and id of resource referred to by policy
     *  
     */
    public void setResource(DSpaceObject o)
    {
        setResourceType(o.getType());
        setResourceID(o.getID());
    }

    /**
     * Set the type of the resource referred to by the policy
     * 
     * @param mytype
     *            type of the resource
     */
    public void setResourceType(int mytype)
    {
        myRow.setColumn("resource_type_id", mytype);
    }

    /**
     * Get the ID of a resource pointed to by the policy (is null if policy
     * doesn't apply to a single resource.)
     * 
     * @return resource_id
     */
    public int getResourceID()
    {
        return myRow.getIntColumn("resource_id");
    }

    /**
     * If the policy refers to a single resource, this is the ID of that
     * resource.
     * 
     * @param myid   id of resource (database primary key)
     */
    public void setResourceID(int myid)
    {
        myRow.setColumn("resource_id", myid);
    }

    /**
     * @return get the action this policy authorizes
     */
    public int getAction()
    {
        return myRow.getIntColumn("action_id");
    }

    /**
     * @return action text or 'null' if action row empty
     */
    public String getActionText()
    {
        int myAction = myRow.getIntColumn("action_id");

        if (myAction == -1)
        {
            return "...";
        }
        else
        {
            return Constants.actionText[myAction];
        }
    }

    /**
     * set the action this policy authorizes
     * 
     * @param myid  action ID from <code>org.dspace.core.Constants</code>
     */
    public void setAction(int myid)
    {
        myRow.setColumn("action_id", myid);
    }

    /**
     * @return eperson ID, or -1 if EPerson not set
     */
    public int getEPersonID()
    {
        return myRow.getIntColumn("eperson_id");
    }

    /**
     * get EPerson this policy relates to
     * 
     * @return EPerson, or null
     */
    public EPerson getEPerson() throws SQLException
    {
        int eid = myRow.getIntColumn("eperson_id");

        if (eid == -1)
        {
            return null;
        }

        return EPerson.find(myContext, eid);
    }

    /**
     * assign an EPerson to this policy
     * 
     * @param e EPerson
     */
    public void setEPerson(EPerson e)
    {
        if (e != null)
        {
            myRow.setColumn("eperson_id", e.getID());
        }
        else
        {
            myRow.setColumnNull("eperson_id");
        }
    }

    /**
     * gets ID for Group referred to by this policy
     * 
     * @return groupID, or -1 if no group set
     */
    public int getGroupID()
    {
        return myRow.getIntColumn("epersongroup_id");
    }

    /**
     * gets Group for this policy
     * 
     * @return Group, or -1 if no group set
     */
    public Group getGroup() throws SQLException
    {
        int gid = myRow.getIntColumn("epersongroup_id");

        if (gid == -1)
        {
            return null;
        }
        else
        {
            return Group.find(myContext, gid);
        }
    }

    /**
     * set Group for this policy
     * 
     * @param g group
     */
    public void setGroup(Group g)
    {
        if (g != null)
        {
            myRow.setColumn("epersongroup_id", g.getID());
        }
        else
        {
            myRow.setColumnNull("epersongroup_id");
        }
    }

    /**
     * figures out if the date is valid for the policy
     * 
     * @return true if policy has begun and hasn't expired yet (or no dates are
     *         set)
     */
    public boolean isDateValid()
    {
        Date sd = getStartDate();
        Date ed = getEndDate();

        // if no dates set, return true (most common case)
        if ((sd == null) && (ed == null))
        {
            return true;
        }

        // one is set, now need to do some date math
        Date now = new Date();

        // check start date first
        if (sd != null && now.before(sd))
        {
            // start date is set, return false if we're before it
            return false;
        }

        // now expiration date
        if (ed != null && now.after(ed))
        {
            // end date is set, return false if we're after it
            return false;
        }

        // if we made it this far, start < now < end
        return true; // date must be okay
    }

    /**
     * Get the start date of the policy
     * 
     * @return start date, or null if there is no start date set (probably most
     *         common case)
     */
    public java.util.Date getStartDate()
    {
        return myRow.getDateColumn("start_date");
    }

    /**
     * Set the start date for the policy
     * 
     * @param d
     *            date, or null for no start date
     */
    public void setStartDate(java.util.Date d)
    {
        myRow.setColumn("start_date", d);
    }

    /**
     * Get end date for the policy
     * 
     * @return end date or null for no end date
     */
    public java.util.Date getEndDate()
    {
        return myRow.getDateColumn("end_date");
    }

    /**
     * Set end date for the policy
     * 
     * @param d
     *            end date, or null
     */
    public void setEndDate(java.util.Date d)
    {
        myRow.setColumn("end_date", d);
    }

    /**
     * Update the ResourcePolicy
     */
    public void update() throws SQLException, AuthorizeException {
        if(getResourceID() != -1 && getResourceType() != -1){
            //A policy for a DSpace Object has been modified, fire a modify event on the DSpace object
            DSpaceObject dso = DSpaceObject.find(myContext, getResourceType(), getResourceID());
            if(dso != null){
                dso.updateLastModified();
            }
        }

        // FIXME: Check authorisation
        DatabaseManager.update(myContext, myRow);
    }


    public String getRpName(){
        return myRow.getStringColumn("rpname");
    }
    public void setRpName(String name){
        myRow.setColumn("rpname", name);
    }

    public String getRpType(){
        return myRow.getStringColumn("rptype");
    }
    public void setRpType(String type){
        myRow.setColumn("rptype", type);
    }

    public String getRpDescription(){
        return myRow.getStringColumn("rpdescription");
    }
    public void setRpDescription(String description){
        myRow.setColumn("rpdescription", description);
    }
}
