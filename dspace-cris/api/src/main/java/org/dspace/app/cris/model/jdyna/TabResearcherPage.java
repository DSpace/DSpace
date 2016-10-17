/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.core.ConfigurationManager;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import it.cilea.osd.common.service.IPersistenceService;
import it.cilea.osd.jdyna.web.AbstractTab;
import it.cilea.osd.jdyna.web.ITabService;

@Entity
@Table(name="cris_rp_tab")
@org.hibernate.annotations.NamedQueries( {
        @org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findAll", query = "from TabResearcherPage order by priority asc"),
        @org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findPropertyHolderInTab", query = "from BoxResearcherPage box where box in (select m from TabResearcherPage tab join tab.mask m where tab.id = ?) order by priority", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findTabsByHolder", query = "from TabResearcherPage tab where :par0 in elements(tab.mask)", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.uniqueTabByShortName", query = "from TabResearcherPage tab where shortName = ?", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findByAccessLevel", query = "from TabResearcherPage tab where visibility = ? order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findByAdmin", query = "from TabResearcherPage tab where visibility = 1 or visibility = 2 or visibility = 3 order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findByOwner", query = "from TabResearcherPage tab where visibility = 0 or visibility = 2 or visibility = 3 order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findByAnonimous", query = "from TabResearcherPage tab where visibility = 3 order by priority", cacheable=true)
})
public class TabResearcherPage extends AbstractTab<BoxResearcherPage> {

	/** Showed holder in this tab */
	@ManyToMany	
	@JoinTable(name = "cris_rp_tab2box", joinColumns = { 
            @JoinColumn(name = "cris_rp_tab_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_rp_box_id") })
	@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<BoxResearcherPage> mask;

    @ElementCollection
    @CollectionTable(
          name="cris_rp_tab2policysingle",
          joinColumns=@JoinColumn(name="tab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<String> authorizedSingle;
    
    @ElementCollection
    @CollectionTable(
          name="cris_rp_tab2policygroup",
          joinColumns=@JoinColumn(name="tab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<String> authorizedGroup;
	
	public TabResearcherPage() {
		this.visibility = VisibilityTabConstant.ADMIN;
	}
	
	@Override
	public List<BoxResearcherPage> getMask() {
		if(this.mask == null) {
			this.mask = new LinkedList<BoxResearcherPage>();
		}
		return this.mask;
	}

	@Override
	public void setMask(List<BoxResearcherPage> mask) {
		this.mask = mask;
	}


    @Override
    public String getFileSystemPath()
    {
        return ConfigurationManager.getProperty(CrisConstants.CFG_MODULE,"researcherpage.file.path");
    }
    

    @Override
    public List<String> getAuthorizedSingle()
    {
        return authorizedSingle;
    }

    @Override
    public void setAuthorizedSingle(List<String> authorizedSingle)
    {
        this.authorizedSingle = authorizedSingle; 
    }

    @Override
    public List<String> getAuthorizedGroup()
    {
        return authorizedGroup;
    }

    @Override
    public void setAuthorizedGroup(List<String> authorizedGroup)
    {
        this.authorizedGroup = authorizedGroup;
    }
    
    @Override
    public <AS extends IPersistenceService> List<String> getMetadataWithPolicySingle(
            AS tabService)
    {       
        List<String> results = new ArrayList<String>();
        for(RPPropertiesDefinition pd : ((ITabService)tabService).getAllPropertiesDefinitionWithPolicySingle(RPPropertiesDefinition.class)) {
            results.add(pd.getShortName());
        }
        return results;
    }

    @Override
    public <AS extends IPersistenceService> List<String> getMetadataWithPolicyGroup(
            AS tabService)
    {
        List<String> results = new ArrayList<String>();
        for(RPPropertiesDefinition pd : ((ITabService)tabService).getAllPropertiesDefinitionWithPolicyGroup(RPPropertiesDefinition.class)) {
            results.add(pd.getShortName());
        }
        return results;
    }
}
