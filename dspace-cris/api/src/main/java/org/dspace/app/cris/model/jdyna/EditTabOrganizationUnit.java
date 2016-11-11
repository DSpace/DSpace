/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.common.service.IPersistenceService;
import it.cilea.osd.jdyna.web.AbstractEditTab;
import it.cilea.osd.jdyna.web.ITabService;

import java.util.ArrayList;
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
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.core.ConfigurationManager;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "cris_ou_etab")
@NamedQueries({
		@NamedQuery(name = "EditTabOrganizationUnit.findAll", query = "from EditTabOrganizationUnit order by priority asc"),
		@NamedQuery(name = "EditTabOrganizationUnit.findPropertyHolderInTab", query = "from BoxOrganizationUnit box where box in (select m from EditTabOrganizationUnit tab join tab.mask m where tab.id = ?) order by priority"),
		@NamedQuery(name = "EditTabOrganizationUnit.findTabsByHolder", query = "from EditTabOrganizationUnit tab where :par0 in elements(tab.mask)"),
		@NamedQuery(name = "EditTabOrganizationUnit.uniqueByDisplayTab", query = "from EditTabOrganizationUnit tab where displayTab.id = ?"),
		@NamedQuery(name = "EditTabOrganizationUnit.uniqueTabByShortName", query = "from EditTabOrganizationUnit tab where shortName = ?"), 
		@NamedQuery(name = "EditTabOrganizationUnit.findByAccessLevel", query = "from EditTabOrganizationUnit tab where visibility = ? order by priority"),
		@NamedQuery(name = "EditTabOrganizationUnit.findByAdmin", query = "from EditTabOrganizationUnit tab where visibility = 1 or visibility = 2 or visibility = 3 order by priority"),
	    @NamedQuery(name = "EditTabOrganizationUnit.findByOwner", query = "from EditTabOrganizationUnit tab where visibility = 0 or visibility = 2 or visibility = 3 order by priority"),
	    @NamedQuery(name = "EditTabOrganizationUnit.findByAnonimous", query = "from EditTabOrganizationUnit tab where visibility = 3 order by priority"),
        @NamedQuery(name = "EditTabOrganizationUnit.findAuthorizedGroupById", query = "select tab.authorizedGroup from EditTabOrganizationUnit tab where tab.id = ?"),
        @NamedQuery(name = "EditTabOrganizationUnit.findAuthorizedGroupByShortname", query = "select tab.authorizedGroup from EditTabOrganizationUnit tab where tab.shortName = ?"),
        @NamedQuery(name = "EditTabOrganizationUnit.findAuthorizedSingleById", query = "select tab.authorizedSingle from EditTabOrganizationUnit tab where tab.id = ?"),
        @NamedQuery(name = "EditTabOrganizationUnit.findAuthorizedSingleByShortname", query = "select tab.authorizedSingle  from EditTabOrganizationUnit tab where tab.shortName = ?")	    
})
public class EditTabOrganizationUnit extends
		AbstractEditTab<BoxOrganizationUnit,TabOrganizationUnit> {

	/** Showed holder in this tab */
	@ManyToMany	
	@JoinTable(name = "cris_ou_etab2box", joinColumns = { 
            @JoinColumn(name = "cris_ou_etab_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_ou_box_id") })
	@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<BoxOrganizationUnit> mask;

	@OneToOne
	private TabOrganizationUnit displayTab;

	@ManyToMany
	@JoinTable(
          name="cris_ou_etab2policysingle",
          joinColumns=@JoinColumn(name="etab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<OUPropertiesDefinition> authorizedSingle;
    
	@ManyToMany
	@JoinTable(
          name="cris_ou_etab2policygroup",
          joinColumns=@JoinColumn(name="etab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<OUPropertiesDefinition> authorizedGroup;
    
	public EditTabOrganizationUnit() {
		this.visibility = VisibilityTabConstant.ADMIN;
	}
	
	@Override
	public List<BoxOrganizationUnit> getMask() {
		if (this.mask == null) {
			this.mask = new LinkedList<BoxOrganizationUnit>();
		}
		return this.mask;
	}

	@Override
	public void setMask(List<BoxOrganizationUnit> mask) {
		this.mask = mask;
	}

	public void setDisplayTab(TabOrganizationUnit displayTab) {
		this.displayTab = displayTab;
	}

	public TabOrganizationUnit getDisplayTab() {
		return displayTab;
	}


	@Override
	public Class<TabOrganizationUnit> getDisplayTabClass() {
		return TabOrganizationUnit.class;
	}
	
	
    @Override
    public String getFileSystemPath()
    {
        return ConfigurationManager.getProperty(CrisConstants.CFG_MODULE,"organizationunit.file.path");
    }
    
    @Override
    public List<OUPropertiesDefinition> getAuthorizedSingle()
    {
        if(this.authorizedSingle==null) {
            this.authorizedSingle = new ArrayList<OUPropertiesDefinition>();
        }
        return this.authorizedSingle;
    }

    public void setAuthorizedSingle(List<OUPropertiesDefinition> authorizedSingle)
    {
        this.authorizedSingle = authorizedSingle;
    }

    @Override
    public List<OUPropertiesDefinition> getAuthorizedGroup()
    {
        if(this.authorizedGroup==null) {
            this.authorizedGroup = new ArrayList<OUPropertiesDefinition>();
        }
        return this.authorizedGroup;
    }

    public void setAuthorizedGroup(List<OUPropertiesDefinition> authorizedGroup)
    {
        this.authorizedGroup = authorizedGroup;
    }

}
