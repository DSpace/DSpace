/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import it.cilea.osd.common.service.IPersistenceService;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.web.Box;
import it.cilea.osd.jdyna.web.ITabService;

@Entity
@Table(name = "cris_pj_box")
@NamedQueries({
		@NamedQuery(name = "BoxProject.findAll", query = "from BoxProject order by priority asc"),
		@NamedQuery(name = "BoxProject.findContainableByHolder", query = "from Containable containable where containable in (select m from BoxProject box join box.mask m where box.id = ?)"),
		@NamedQuery(name = "BoxProject.findHolderByContainable", query = "from BoxProject box where :par0 in elements(box.mask)"),
		@NamedQuery(name = "BoxProject.uniqueBoxByShortName", query = "from BoxProject box where shortName = ?")
})		
public class BoxProject extends Box<Containable> {
    	
	@ManyToMany	
	@JoinTable(name = "cris_pj_box2con", joinColumns = { 
            @JoinColumn(name = "cris_pj_box_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "jdyna_containable_id") })
	@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<Containable> mask;

    @ElementCollection
    @CollectionTable(
          name="cris_pj_box2policysingle",
          joinColumns=@JoinColumn(name="box_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<String> authorizedSingle;
    
    @ElementCollection
    @CollectionTable(
          name="cris_pj_box2policygroup",
          joinColumns=@JoinColumn(name="box_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<String> authorizedGroup;
	
	public BoxProject() {
		this.visibility = VisibilityTabConstant.ADMIN;
	}
	
	@Override
	public List<Containable> getMask() {
		if(this.mask==null) {
			this.mask = new LinkedList<Containable>();
		}		
		return mask;
	}

	@Override
	public void setMask(List<Containable> mask) {
		if(mask!=null) {
			Collections.sort(mask);
		}
		this.mask = mask;
	}

    public List<String> getAuthorizedSingle()
    {
        if(this.authorizedSingle==null) {
            this.authorizedSingle = new ArrayList<String>();
        }
        return authorizedSingle;
    }

    public void setAuthorizedSingle(List<String> authorizedSingle)
    {
        this.authorizedSingle = authorizedSingle; 
    }

    public List<String> getAuthorizedGroup()
    {
        if(this.authorizedGroup==null) {
            this.authorizedGroup = new ArrayList<String>();
        }
        return authorizedGroup;
    }

    public void setAuthorizedGroup(List<String> authorizedGroup)
    {
        this.authorizedGroup = authorizedGroup;
    }
	
    @Override
    public <AS extends IPersistenceService> List<String> getMetadataWithPolicySingle(
            AS tabService, String specificPart)
    {  
        List<String> results = new ArrayList<String>();
        for(ProjectPropertiesDefinition pd : ((ITabService)tabService).getAllPropertiesDefinitionWithPolicySingle(ProjectPropertiesDefinition.class)) {
            results.add(pd.getShortName());
        }
        return results;
    }

    @Override
    public <AS extends IPersistenceService> List<String> getMetadataWithPolicyGroup(
            AS tabService, String specificPart)
    {
        List<String> results = new ArrayList<String>();
        for(ProjectPropertiesDefinition pd : ((ITabService)tabService).getAllPropertiesDefinitionWithPolicyGroup(ProjectPropertiesDefinition.class)) {
            results.add(pd.getShortName());
        }
        return results;
    }
}
