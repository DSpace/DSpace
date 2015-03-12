/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import it.cilea.osd.common.model.IdentifiableObject;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This class models a preference expressed for a specific relation between two
 * CRIS objects or between a CRIS object and a DSpace Item.
 * 
 * @author cilea
 * 
 */
@Entity
@Table(name = "cris_relpref")
@NamedQueries( {
    @NamedQuery(name = "RelationPreference.uniqueByUUIDItemID", query = "from RelationPreference where sourceUUID = ? and itemID = ? and relationType = ?"),
    @NamedQuery(name = "RelationPreference.uniqueByUUIDs", query = "from RelationPreference where sourceUUID = ? and targetUUID = ? and relationType = ?"),
    @NamedQuery(name = "RelationPreference.findByTargetUUID", query = "from RelationPreference where targetUUID = ?"),
    @NamedQuery(name = "RelationPreference.findByTargetItemID", query = "from RelationPreference where itemID = ?"),    
    // sorting is needed for selected preference 
    @NamedQuery(name = "RelationPreference.findBySourceUUIDAndRelationType", query = "from RelationPreference where sourceUUID = ? and relationType = ? order by priority asc"),
    @NamedQuery(name = "RelationPreference.findBySourceUUIDAndRelationTypeAndStatus", query = "from RelationPreference where sourceUUID = ? and relationType = ? and status = ? order by priority asc")
})   
public class RelationPreference extends IdentifiableObject
{
    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_RELPREF_SEQ")
    @SequenceGenerator(name = "CRIS_RELPREF_SEQ", sequenceName = "CRIS_RELPREF_SEQ", allocationSize = 1)
    private Integer id;
    
    private String sourceUUID;
    
    private String targetUUID;
    
    private Integer itemID;
    
    private String relationType;
    
    private String status;
    
    private int priority;

    public static final String PREFIX_RELATIONPREFERENCES = "relationpreferences.";
    
    public static final String SELECTED = "selected";
    public static final String HIDED = "hided";
    public static final String UNLINKED = "unlinked";
    
    public Integer getId()
    {
        return id;
    }
    public void setId(Integer id)
    {
        this.id = id;
    }
    public String getSourceUUID()
    {
        return sourceUUID;
    }
    public void setSourceUUID(String sourceUUID)
    {
        this.sourceUUID = sourceUUID;
    }
    public String getTargetUUID()
    {
        return targetUUID;
    }
    public void setTargetUUID(String targetUUID)
    {
        this.targetUUID = targetUUID;
    }
    public Integer getItemID()
    {
        return itemID;
    }
    public void setItemID(Integer itemID)
    {
        this.itemID = itemID;
    }
    public String getRelationType()
    {
        return relationType;
    }
    public void setRelationType(String type)
    {
        this.relationType = type;
    }
    public String getStatus()
    {
        return status;
    }
    public void setStatus(String status)
    {
        this.status = status;
    }
    
    public int getPriority()
    {
        return priority;
    }
    
    public void setPriority(int priority)
    {
        this.priority = priority;
    }
}

