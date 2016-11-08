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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.ConfigurationManager;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import it.cilea.osd.common.service.IPersistenceService;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
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
		@org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findByAnonimous", query = "from TabResearcherPage tab where visibility = 3 order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findAuthorizedGroupById", query = "select tab.authorizedGroup from TabResearcherPage tab where tab.id = ?"),
		@org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findAuthorizedGroupByShortname", query = "select tab.authorizedGroup from TabResearcherPage tab where tab.shortName = ?"),
		@org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findAuthorizedSingleById", query = "select tab.authorizedSingle from TabResearcherPage tab where tab.id = ?"),
		@org.hibernate.annotations.NamedQuery(name = "TabResearcherPage.findAuthorizedSingleByShortname", query = "select tab.authorizedSingle from TabResearcherPage tab where tab.shortName = ?")
})
public class TabResearcherPage extends AbstractTab<BoxResearcherPage> {

	/** Showed holder in this tab */
	@ManyToMany	
	@JoinTable(name = "cris_rp_tab2box", joinColumns = { 
            @JoinColumn(name = "cris_rp_tab_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_rp_box_id") })
	@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<BoxResearcherPage> mask;

	@ManyToMany
    @JoinTable(
          name="cris_rp_tab2policysingle",
          joinColumns=@JoinColumn(name="tab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<RPPropertiesDefinition> authorizedSingle;
    
	@ManyToMany
    @JoinTable(
          name="cris_rp_tab2policygroup",
          joinColumns=@JoinColumn(name="tab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<RPPropertiesDefinition> authorizedGroup;
	
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
    

    public List<RPPropertiesDefinition> getAuthorizedSingle()
    {
        if(this.authorizedSingle==null) {
            this.authorizedSingle = new ArrayList<RPPropertiesDefinition>();
        }
        return authorizedSingle;
    }

    public void setAuthorizedSingle(List<RPPropertiesDefinition> authorizedSingle)
    {
        this.authorizedSingle = authorizedSingle; 
    }

    public List<RPPropertiesDefinition> getAuthorizedGroup()
    {
        if(this.authorizedGroup==null) {
            this.authorizedGroup = new ArrayList<RPPropertiesDefinition>();
        }
        return authorizedGroup;
    }

    public void setAuthorizedGroup(List<RPPropertiesDefinition> authorizedGroup)
    {
        this.authorizedGroup = authorizedGroup;
    }

}