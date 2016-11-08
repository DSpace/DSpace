/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.common.service.IPersistenceService;
import it.cilea.osd.jdyna.web.AbstractTab;
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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.ConfigurationManager;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "cris_ou_tab")
@NamedQueries({
        @NamedQuery(name = "TabOrganizationUnit.findAll", query = "from TabOrganizationUnit order by priority asc"),
        @NamedQuery(name = "TabOrganizationUnit.findPropertyHolderInTab", query = "from BoxOrganizationUnit box where box in (select m from TabOrganizationUnit tab join tab.mask m where tab.id = ?) order by priority"),
        @NamedQuery(name = "TabOrganizationUnit.findTabsByHolder", query = "from TabOrganizationUnit tab where :par0 in elements(tab.mask)"),
        @NamedQuery(name = "TabOrganizationUnit.uniqueTabByShortName", query = "from TabOrganizationUnit tab where shortName = ?"),
        @NamedQuery(name = "TabOrganizationUnit.findByAccessLevel", query = "from TabOrganizationUnit tab where visibility = ? order by priority"),
        @NamedQuery(name = "TabOrganizationUnit.findByAdmin", query = "from TabOrganizationUnit tab where visibility = 1 or visibility = 2 or visibility = 3 order by priority"),
        @NamedQuery(name = "TabOrganizationUnit.findByOwner", query = "from TabOrganizationUnit tab where visibility = 0 or visibility = 2 or visibility = 3 order by priority"),
        @NamedQuery(name = "TabOrganizationUnit.findByAnonimous", query = "from TabOrganizationUnit tab where visibility = 3 order by priority"),
        @NamedQuery(name = "TabOrganizationUnit.findAuthorizedGroupById", query = "select tab.authorizedGroup from TabOrganizationUnit tab where tab.id = ?"),
        @NamedQuery(name = "TabOrganizationUnit.findAuthorizedGroupByShortname", query = "select tab.authorizedGroup from TabOrganizationUnit tab where tab.shortName = ?"),
        @NamedQuery(name = "TabOrganizationUnit.findAuthorizedSingleById", query = "select tab.authorizedSingle from TabOrganizationUnit tab where tab.id = ?"),
        @NamedQuery(name = "TabOrganizationUnit.findAuthorizedSingleByShortname", query = "select tab.authorizedSingle  from TabOrganizationUnit tab where tab.shortName = ?")
})
public class TabOrganizationUnit extends AbstractTab<BoxOrganizationUnit>
{

    /** Showed holder in this tab */
    @ManyToMany    
    @JoinTable(name = "cris_ou_tab2box", joinColumns = { 
            @JoinColumn(name = "cris_ou_tab_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_ou_box_id") })
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<BoxOrganizationUnit> mask;

    @ManyToMany
    @JoinTable(
          name="cris_ou_tab2policysingle",
          joinColumns=@JoinColumn(name="tab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<OUPropertiesDefinition> authorizedSingle;
    
    @ManyToMany
    @JoinTable(
          name="cris_ou_tab2policygroup",
          joinColumns=@JoinColumn(name="tab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<OUPropertiesDefinition> authorizedGroup;
    
    public TabOrganizationUnit()
    {
        this.visibility = VisibilityTabConstant.ADMIN;
    }

    @Override
    public List<BoxOrganizationUnit> getMask()
    {
        if (this.mask == null)
        {
            this.mask = new LinkedList<BoxOrganizationUnit>();
        }
        return this.mask;
    }

    @Override
    public void setMask(List<BoxOrganizationUnit> mask)
    {
        this.mask = mask;
    }
    @Override
    public String getFileSystemPath()
    {
        return ConfigurationManager.getProperty(CrisConstants.CFG_MODULE,"organizationunit.file.path");
    }

    public List<OUPropertiesDefinition> getAuthorizedSingle()
    {
        if(this.authorizedSingle==null) {
            this.authorizedSingle = new ArrayList<OUPropertiesDefinition>();
        }
        return authorizedSingle;
    }

    public void setAuthorizedSingle(List<OUPropertiesDefinition> authorizedSingle)
    {
        this.authorizedSingle = authorizedSingle; 
    }

    public List<OUPropertiesDefinition> getAuthorizedGroup()
    {
        if(this.authorizedGroup==null) {
            this.authorizedGroup = new ArrayList<OUPropertiesDefinition>();
        }
        return authorizedGroup;
    }

    public void setAuthorizedGroup(List<OUPropertiesDefinition> authorizedGroup)
    {
        this.authorizedGroup = authorizedGroup;
    }

}
