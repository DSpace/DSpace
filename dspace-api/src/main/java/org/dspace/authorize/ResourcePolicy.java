/*
 * ResourcePolicy.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.authorize;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.authorize.dao.ResourcePolicyDAOFactory;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.uri.Identifiable;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.SimpleIdentifier;
import org.dspace.uri.UnsupportedIdentifierException;
import org.dspace.uri.ExternalIdentifier;

import java.util.Date;
import java.util.List;

/**
 * Class representing a ResourcePolicy
 * 
 * @author David Stuve
 * @version $Revision$
 */
public class ResourcePolicy implements Identifiable
{
    private static Logger log = Logger.getLogger(ResourcePolicy.class);

    private Context context;
    private ResourcePolicyDAO dao;
    private EPersonDAO epersonDAO;
    private GroupDAO groupDAO;

    private int id;
    // private ObjectIdentifier oid;

    private SimpleIdentifier sid;

    // FIXME: Figure out a way to replace all of this using the
    // ObjectIdentifier class.
    private int resourceID;
    private int resourceTypeID;

    private int actionID;
    private int epersonID;
    private int groupID;

    private Date startDate;
    private Date endDate;

    public ResourcePolicy(Context context, int id)
    {
        this.context = context;
        this.id = id;

        dao = ResourcePolicyDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);
        groupDAO = GroupDAOFactory.getInstance(context);

        resourceID = -1;
        resourceTypeID = -1;
        actionID = -1;
        epersonID = -1;
        groupID = -1;

        context.cache(this, id);
    }

    public int getID()
    {
        return id;
    }

    public SimpleIdentifier getSimpleIdentifier()
    {
        return sid;
    }

    public void setSimpleIdentifier(SimpleIdentifier sid)
    {
        this.sid = sid;
    }

    public ObjectIdentifier getIdentifier()
    {
        return null;
    }

    public void setIdentifier(ObjectIdentifier oid)
    {
        this.sid = oid;
    }

    public List<ExternalIdentifier> getExternalIdentifiers()
    {
        return null;
    }

    public void setExternalIdentifiers(List<ExternalIdentifier> eids)
            throws UnsupportedIdentifierException
    {
        throw new UnsupportedIdentifierException("ResourcePolicy does not support the use of ExternalIdentifiers");
    }

    public void addExternalIdentifier(ExternalIdentifier eid)
            throws UnsupportedIdentifierException
    {
        throw new UnsupportedIdentifierException("ResourcePolicy does not support the use of ExternalIdentifiers");
    }

    /**
     * Get the type of the objects referred to by policy
     * 
     * @return type of object/resource
     */
    public int getResourceType()
    {
        return resourceTypeID;
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
     */
    public void setResourceType(int resourceTypeID)
    {
        this.resourceTypeID = resourceTypeID;
    }

    /**
     * Get the ID of a resource pointed to by the policy (is null if policy
     * doesn't apply to a single resource.)
     */
    public int getResourceID()
    {
        return resourceID;
    }

    /**
     * If the policy refers to a single resource, this is the ID of that
     * resource.
     */
    public void setResourceID(int resourceID)
    {
        this.resourceID = resourceID;
    }

    /**
     * Returns the action this policy authorizes.
     */
    public int getAction()
    {
        return actionID;
    }

    public String getActionText()
    {
        if (actionID == -1)
        {
            return "...";
        }
        else
        {
            return Constants.actionText[actionID];
        }
    }

    /**
     * set the action this policy authorizes
     * 
     * @param actionID action ID from <code>org.dspace.core.Constants</code>
     */
    public void setAction(int actionID)
    {
        this.actionID = actionID;
    }

    /**
     * @return eperson ID, or -1 if EPerson not set
     */
    public int getEPersonID()
    {
        return epersonID;
    }

    public void setEPersonID(int epersonID)
    {
        this.epersonID = epersonID;
    }

    /**
     * get EPerson this policy relates to
     * 
     * @return EPerson, or null
     */
    public EPerson getEPerson()
    {
        if (epersonID == -1)
        {
            return null;
        }

        return epersonDAO.retrieve(epersonID);
    }

    /**
     * assign an EPerson to this policy
     * 
     * @param e EPerson
     */
    public void setEPerson(EPerson eperson)
    {
        if (eperson != null)
        {
            epersonID = eperson.getID();
        }
        else
        {
            epersonID = -1;
        }
    }

    /**
     * gets ID for Group referred to by this policy
     * 
     * @return groupID, or -1 if no group set
     */
    public int getGroupID()
    {
        return groupID;
    }

    public void setGroupID(int groupID)
    {
        this.groupID = groupID;
    }

    /**
     * gets Group for this policy
     * 
     * @return Group, or -1 if no group set
     */
    public Group getGroup()
    {
        if (groupID == -1)
        {
            return null;
        }

        return groupDAO.retrieve(groupID);
    }

    /**
     * set Group for this policy
     */
    public void setGroup(Group group)
    {
        if (group != null)
        {
            groupID = group.getID();
        }
        else
        {
            groupID = -1;
        }
    }

    /**
     * Get the start date of the policy
     * 
     * @return start date, or null if there is no start date set (probably most
     *         common case)
     */
    public Date getStartDate()
    {
        return startDate;
    }

    /**
     * Set the start date for the policy
     * 
     * @param d
     *            date, or null for no start date
     */
    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    /**
     * Get end date for the policy
     * 
     * @return end date or null for no end date
     */
    public Date getEndDate()
    {
        return endDate;
    }

    /**
     * Set end date for the policy
     * 
     * @param d
     *            end date, or null
     */
    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

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
        if (sd != null)
        {
            // start date is set, return false if we're before it
            if (now.before(sd))
            {
                return false;
            }
        }

        // now expiration date
        if (ed != null)
        {
            // end date is set, return false if we're after it
            if (now.after(sd))
            {
                return false;
            }
        }

        // if we made it this far, start < now < end
        return true; // date must be okay
    }

    @Deprecated
    ResourcePolicy(Context context, org.dspace.storage.rdbms.TableRow row)
    {
        this(context, row.getIntColumn("policy_id"));
    }

    @Deprecated
    public static ResourcePolicy find(Context context, int id)
    {
        return ResourcePolicyDAOFactory.getInstance(context).retrieve(id);
    }

    @Deprecated
    public static ResourcePolicy create(Context context)
        throws AuthorizeException
    {
        return ResourcePolicyDAOFactory.getInstance(context).create();
    }

    @Deprecated
    public void delete()
    {
        dao.delete(getID());
    }

    @Deprecated
    public void update()
    {
        dao.update(this);
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public boolean equals(ResourcePolicy other)
    {
        if (this.getID() == other.getID())
        {
            return true;
        }

        return false;
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
