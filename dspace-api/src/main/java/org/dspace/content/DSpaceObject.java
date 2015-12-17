/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.Handle;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

import javax.persistence.*;

/**
 * Abstract base class for DSpace objects
 */
@Entity
@Inheritance(strategy= InheritanceType.JOINED)
@Table(name = "dspaceobject")
public abstract class DSpaceObject implements Serializable
{
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "uuid", unique = true, nullable = false, insertable = true, updatable = false)
    protected java.util.UUID id;

    // accumulate information to add to "detail" element of content Event,
    // e.g. to document metadata fields touched, etc.
    @Transient
    private StringBuffer eventDetails = null;
    
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dSpaceObject", cascade={CascadeType.PERSIST}, orphanRemoval = true)
    @OrderBy("metadataField, place")
    private List<MetadataValue> metadata = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dso")
    // Order by is here to ensure that the oldest handle is retrieved first,
    // multiple handles are assigned to the latest version of an item the original handle will have the lowest identifier
    // This handle is the prefered handle.
    @OrderBy("handle_id ASC")
    private List<Handle> handles = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dSpaceObject", cascade={CascadeType.PERSIST}, orphanRemoval = false)
    private List<ResourcePolicy> resourcePolicies = new ArrayList<>();

    /**
     * True if anything else was changed since last update()
     * (to drive event mechanism)
     */
    @Transient
    private boolean modifiedMetadata = false;

    /** Flag set when data is modified, for events */
    @Transient
    private boolean modified = false;

    protected DSpaceObject()
    {

    }

    /**
     * Reset the cache of event details.
     */
    public void clearDetails()
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
    public String getDetails()
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
    public UUID getID(){
        return id;
    }

    public abstract String getName();

    /**
     * Get the Handle of the object. This may return <code>null</code>
     * 
     * @return Handle of the object, or <code>null</code> if it doesn't have
     *         one
     */
    public String getHandle()
    {
        return (CollectionUtils.isNotEmpty(handles) ? handles.get(0).getHandle() : null);
    }

    void setHandle(List<Handle> handle)
    {
        this.handles = handle;
    }

    public void addHandle(Handle handle)
    {
        this.handles.add(handle);
    }

    public List<Handle> getHandles() {
        return handles;
    }

    protected List<MetadataValue> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataValue> metadata) {
        this.metadata = metadata;
    }

    protected void removeMetadata(MetadataValue metadataValue)
    {
        setMetadataModified();
        getMetadata().remove(metadataValue);
    }

    protected void removeMetadata(List<MetadataValue> metadataValues)
    {
        setMetadataModified();
        getMetadata().removeAll(metadataValues);
    }


    protected void addMetadata(MetadataValue metadataValue) {
        setMetadataModified();
        getMetadata().add(metadataValue);
        addDetails(metadataValue.getMetadataField().toString());
    }

    public List<ResourcePolicy> getResourcePolicies() {
        return resourcePolicies;
    }

    public boolean isMetadataModified() {
        return modifiedMetadata;
    }

    protected void setMetadataModified() {
        this.modifiedMetadata = true;
    }

    public boolean isModified() {
        return modified;
    }
    public void clearModified() {
        this.modified = false;
    }
    protected void setModified() {
        this.modified = true;
    }

    public String toString() {
        String ident = getHandle();
        if (ident == null)
            ident = "" + getID();
        return Constants.typeText[getType()] + "." + ident;
    }

    /**
     * expects a handle or a two part string as parameter: TYPE.IDENT,
     * where TYPE is a DSpaceObject type and
     * IDENT is a UUID, handle,  group name, or Eperson's netid
     *
     * returns DSpaceObject that corresponds to string or null
     * @param str    must have format TYPE.ID
     * @return
     */
    public static DSpaceObject fromString(Context c, String str) throws SQLException {
        if (str == null) {
            return null;
        }
        int doti = str.indexOf('.');
        HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
        if (doti == -1) {
            // no '.' ==> assume its a handle
            return handleService.resolveToObject(c, str);
        }

        if (doti > 0 && doti < str.length()-1) {
            String left = str.substring(0,doti);
            String right = str.substring(doti+1);
            int typeId = Constants.getTypeID(left.toUpperCase());
            if (typeId == -1) {
                // not one of the DSPaceObject types
                return null;
            }

            try {
                UUID id = UUID.fromString(right);
                DSpaceObjectService dSpaceObjectService = ContentServiceFactory.getInstance().getDSpaceObjectService(typeId);
                return dSpaceObjectService.find(c, id);
            } catch (IllegalArgumentException ne) {
                switch (typeId) {
                    case Constants.EPERSON: {
                        EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
                        DSpaceObject person = epersonService.findByNetid(c, right);
                        if (person == null) {
                            person = epersonService.findByEmail(c, right);
                        }
                        return person;
                    }
                    case Constants.GROUP:
                        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
                        return groupService.findByName(c, right);
                    default:
                        DSpaceObject obj = handleService.resolveToObject(c, right);
                        return (obj != null && obj.getType() == typeId) ? obj : null;
                }
            }
        }
        return null;
    }




}