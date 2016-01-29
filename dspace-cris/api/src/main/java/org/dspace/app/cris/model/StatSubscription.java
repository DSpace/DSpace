/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import it.cilea.osd.common.model.IdentifiableObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * This class models a single subscription to receive mail notification of the
 * newly added item to ResearcherPage.
 * 
 * @author cilea
 * 
 */
@Entity
@Table(name = "cris_statsubscription")
@NamedQueries( {
        // WARNING: the findAll and findByFreq query MUST return StatSubscription ordered by eperson and typeDef as it is needed by the batch script
        @NamedQuery(name = "StatSubscription.findAll", query = "from StatSubscription order by epersonID"),
        @NamedQuery(name = "StatSubscription.findByFreq", query = "from StatSubscription where freq = ? order by epersonID, typeDef"),
        @NamedQuery(name = "StatSubscription.findByFreqAndType", query = "from StatSubscription where freq = ? and typeDef = ? order by epersonID"),        
        @NamedQuery(name = "StatSubscription.findByType", query = "from StatSubscription where typeDef = ? order by id"),
        @NamedQuery(name = "StatSubscription.findByUID", query = "from StatSubscription where uid = ? order by id"),
        @NamedQuery(name = "StatSubscription.findByEPersonID", query = "from StatSubscription where epersonID = ? order by uid"),        
        @NamedQuery(name = "StatSubscription.findByEPersonIDandType", query = "from StatSubscription where epersonID = ? and typeDef = ? order by freq"),
        @NamedQuery(name = "StatSubscription.findByEPersonIDandUID", query = "from StatSubscription where epersonID = ? and uid = ? order by freq"),
        @NamedQuery(name = "StatSubscription.deleteByEPersonID", query = "delete from StatSubscription where epersonID = ?")
})   
public class StatSubscription extends IdentifiableObject {

    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_STATSUBSCRIPTION_SEQ")
    @SequenceGenerator(name = "CRIS_STATSUBSCRIPTION_SEQ", sequenceName = "CRIS_STATSUBSCRIPTION_SEQ", allocationSize = 1)
    private Integer id;
	
    /** Type of the DSpace resource */
	private Integer typeDef;
	
	/**
	 * the handle of the DSpace resource and the uuid of the DSpace-Cris resource to monitor
	 */
	@Column(name = "handle_or_uuid")
	private String uid;
	
	/**
	 * the eperson ID of the subscriber
	 */
	private int epersonID;

	/**
	 * See constant FREQUENCY_*
	 */
	private int freq;
	
	@Transient
	public static final int FREQUENCY_DAILY = 1;
	@Transient
	public static final int FREQUENCY_WEEKLY = 7;
	@Transient
	public static final int FREQUENCY_MONTHLY = 30;
    @Transient
    public static final int FREQUENCY_YEAR = 365;
    
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

    public int getFreq()
    {
        return freq;
    }

    public void setFreq(int freq)
    {
        this.freq = freq;
    }

    public String getUid()
    {
        return uid;
    }

    public void setUid(String uid)
    {
        this.uid = uid;
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
