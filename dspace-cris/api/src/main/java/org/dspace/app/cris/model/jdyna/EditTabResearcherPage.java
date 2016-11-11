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
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.core.ConfigurationManager;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import it.cilea.osd.common.service.IPersistenceService;
import it.cilea.osd.jdyna.web.AbstractEditTab;
import it.cilea.osd.jdyna.web.ITabService;

@Entity
@Table(name = "cris_rp_etab")
@NamedQueries({
		@NamedQuery(name = "EditTabResearcherPage.findAll", query = "from EditTabResearcherPage order by priority asc"),
		@NamedQuery(name = "EditTabResearcherPage.findPropertyHolderInTab", query = "from BoxResearcherPage box where box in (select m from EditTabResearcherPage tab join tab.mask m where tab.id = ?) order by priority"),
		@NamedQuery(name = "EditTabResearcherPage.findTabsByHolder", query = "from EditTabResearcherPage tab where :par0 in elements(tab.mask)"),
		@NamedQuery(name = "EditTabResearcherPage.uniqueByDisplayTab", query = "from EditTabResearcherPage tab where displayTab.id = ?"),
		@NamedQuery(name = "EditTabResearcherPage.uniqueTabByShortName", query = "from EditTabResearcherPage tab where shortName = ?"), 
		@NamedQuery(name = "EditTabResearcherPage.findByAccessLevel", query = "from EditTabResearcherPage tab where visibility = ? order by priority"),
        @NamedQuery(name = "EditTabResearcherPage.findByAdmin", query = "from EditTabResearcherPage tab where visibility = 1 or visibility = 2 or visibility = 3 order by priority"),
        @NamedQuery(name = "EditTabResearcherPage.findByOwner", query = "from EditTabResearcherPage tab where visibility = 0 or visibility = 2 or visibility = 3 order by priority"),
        @NamedQuery(name = "EditTabResearcherPage.findByAnonimous", query = "from EditTabResearcherPage tab where visibility = 3 order by priority"),
        @NamedQuery(name = "EditTabResearcherPage.findAuthorizedGroupById", query = "select tab.authorizedGroup from EditTabResearcherPage tab where tab.id = ?"),
        @NamedQuery(name = "EditTabResearcherPage.findAuthorizedGroupByShortname", query = "select tab.authorizedGroup from EditTabResearcherPage tab where tab.shortName = ?"),
        @NamedQuery(name = "EditTabResearcherPage.findAuthorizedSingleById", query = "select tab.authorizedSingle from EditTabResearcherPage tab where tab.id = ?"),
        @NamedQuery(name = "EditTabResearcherPage.findAuthorizedSingleByShortname", query = "select tab.authorizedSingle  from EditTabResearcherPage tab where tab.shortName = ?")        
})
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EditTabResearcherPage extends
		AbstractEditTab<BoxResearcherPage,TabResearcherPage> {

	/** Showed holder in this tab */
	@ManyToMany	
	@JoinTable(name = "cris_rp_etab2box", joinColumns = { 
            @JoinColumn(name = "cris_rp_etab_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_rp_box_id") })
	@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<BoxResearcherPage> mask;

	@OneToOne
	private TabResearcherPage displayTab;
	
	@ManyToMany
	@JoinTable(
          name="cris_rp_etab2policysingle",
          joinColumns=@JoinColumn(name="etab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<RPPropertiesDefinition> authorizedSingle;
    
    @ManyToMany
    @JoinTable(
          name="cris_rp_etab2policygroup",
          joinColumns=@JoinColumn(name="etab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<RPPropertiesDefinition> authorizedGroup;

	public EditTabResearcherPage() {
		this.visibility = VisibilityTabConstant.ADMIN;
	}
	
	@Override
	public List<BoxResearcherPage> getMask() {
		if (this.mask == null) {
			this.mask = new LinkedList<BoxResearcherPage>();
		}
		return this.mask;
	}

	@Override
	public void setMask(List<BoxResearcherPage> mask) {
		this.mask = mask;
	}

	public void setDisplayTab(TabResearcherPage displayTab) {
		this.displayTab = displayTab;
	}

	public TabResearcherPage getDisplayTab() {
		return displayTab;
	}


	@Override
	public Class<TabResearcherPage> getDisplayTabClass() {
		return TabResearcherPage.class;
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
