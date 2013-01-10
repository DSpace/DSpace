/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.services.auth.ResourcePolicyType;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */


@Entity
@Table(name = "resourcepolicy")
@SequenceGenerator(name="resourcepolicy_gen", sequenceName="resourcepolicy_seq")
@Configurable
public class ResourcePolicy extends DSpaceObject{
    private Integer resourceType;
    private Integer resource;
    private Integer action;
    private Eperson eperson;
    private EpersonGroup epersongroup;
    private Date startDate;
    private Date endDate;
    private String rpName;
    private String rpType;
    private String rpDescription;
    
    @Id
    @Column(name = "policy_id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="resourcepolicy_gen")
    public int getID() {
        return id;
    }
    
    @Override
    @Transient
    public DSpaceObjectType getType()
    {
    	return DSpaceObjectType.RESOURCE_POLICY;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epersongroup_id", nullable = true)
	public EpersonGroup getEpersonGroup() {
		return epersongroup;
	}

	public void setEpersonGroup(EpersonGroup epersongroup) {
		this.epersongroup = epersongroup;
	}

   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_id", nullable = true)
	public Eperson getEperson() {
		return eperson;
	}

	public void setEperson(Eperson owner) {
		this.eperson = owner;
	}

    @Column(name = "resource_type_id", nullable = true)
	public Integer getResourceType() {
		return resourceType;
	}

	public void setResourceType(Integer resourceType) {
		this.resourceType = resourceType;
	}

    @Column(name = "resource_id", nullable = true)
	public Integer getResource() {
		return resource;
	}
	

	public void setResource(Integer resource) {
		this.resource = resource;
	}

    @Column(name = "action_id", nullable = true)
	public Integer getAction() {
		return action;
	}

	public void setAction(Integer action) {
		this.action = action;
	}
	
    @Column(name = "start_date", nullable = true)
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

    @Column(name = "end_date", nullable = true)
	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
    @Column(name = "rpname", nullable = true)
	public String getRpName() {
		return rpName;
	}

	public void setRpName(String rpname) {
		this.rpName = rpname;
	}

    @Column(name = "rptype", nullable = true)
	public String getRpType() {
		return rpType;
	}

	public void setRpType(String rptype) {
		this.rpType = rptype;
	}

    @Column(name = "rpdescription", nullable = true)
	public String getRpDescription() {
		return rpDescription;
	}

	public void setRpDescription(String rpdescription) {
		this.rpDescription = rpdescription;
	}
	
	@Transient 
	public ResourcePolicyType getResourcePolicyType () {
		return ResourcePolicyType.valueOf(this.getRpType());
	}
	
	@Transient
	public DSpaceObjectType getDSpaceObjectType () {
		return DSpaceObjectType.getById(this.getResource());
	}
	
	@Transient
	public boolean isDateValid () {
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
}
