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
 * This class models a single subscription to receive mail notification of the
 * newly added item to an ACrisObject.
 * 
 * @author cilea
 * 
 */
@Entity
@Table(name="cris_subscription")
@NamedQueries({
        // WARNING: the findAll query MUST return CrisSubscription ordered by
        // eperson as it is needed by the batch script
        @NamedQuery(name = "CrisSubscription.findAll", query = "from CrisSubscription sub order by sub.epersonID"),
        @NamedQuery(name = "CrisSubscription.countByUUID", query = "select count(*) from CrisSubscription where uuid = ? order by id"),
        @NamedQuery(name = "CrisSubscription.findUUIDByEpersonID", query = "select uuid from CrisSubscription sub where sub.epersonID = ?"),        
        @NamedQuery(name = "CrisSubscription.deleteByEpersonID", query = "delete from CrisSubscription sub where sub.epersonID = ?"),
        @NamedQuery(name = "CrisSubscription.uniqueByEpersonIDandUUID", query = "from CrisSubscription sub where sub.epersonID = ? and uuid = ?"),
        @NamedQuery(name = "CrisSubscription.countByEpersonIDandUUID", query = "select count(*) from CrisSubscription sub where sub.epersonID = ? and uuid = ?") })
public class CrisSubscription extends IdentifiableObject {
   
    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_SUBSCRIPTION_SEQ")
    @SequenceGenerator(name = "CRIS_SUBSCRIPTION_SEQ", sequenceName = "CRIS_SUBSCRIPTION_SEQ", allocationSize = 1)
    private Integer id;

    private String uuid;
    
    private Integer typeDef;
    /**
     * the eperson ID of the subscriber
     */
    private int epersonID;

    public Integer getId()
    {
        return id;
    }
    
    public void setId(Integer id)
    {
        this.id = id;
    }
    
    public int getEpersonID()
    {
        return epersonID;
    }
    
    public void setEpersonID(int epersonID)
    {
        this.epersonID = epersonID;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setTypeDef(Integer type)
    {
        this.typeDef = type;
    }

    public Integer getTypeDef()
    {
        return typeDef;
    }

    		
}
