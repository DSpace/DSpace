/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.common.service.IPersistenceService;
import it.cilea.osd.jdyna.web.ITabService;
import it.cilea.osd.jdyna.web.TypedAbstractTab;

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
import javax.persistence.Table;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.core.ConfigurationManager;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="cris_do_tab")
@org.hibernate.annotations.NamedQueries( {
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findAll", query = "from TabDynamicObject order by priority asc"),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findPropertyHolderInTab", query = "from BoxDynamicObject box where box in (select m from TabDynamicObject tab join tab.mask m where tab.id = ?) order by priority", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findTabsByHolder", query = "from TabDynamicObject tab where :par0 in elements(tab.mask)", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.uniqueTabByShortName", query = "from TabDynamicObject tab where shortName = ?", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByAccessLevel", query = "from TabDynamicObject tab where visibility = ? order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByTypeAndAccessLevel", query = "from TabDynamicObject tab where typeDef = ? and visibility = ? order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByAdmin", query = "from TabDynamicObject tab where visibility = 1 or visibility = 2 or visibility = 3 order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByOwner", query = "from TabDynamicObject tab where visibility = 0 or visibility = 2 or visibility = 3 order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByAnonimous", query = "from TabDynamicObject tab where visibility = 3 order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findTabByType", query = "from TabDynamicObject where typeDef = ?", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByAdminAndTypoDef", query = "from TabDynamicObject tab where ((visibility = 1 or visibility = 2 or visibility = 3) and typeDef = ?) order by priority", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByOwnerAndTypoDef", query = "from TabDynamicObject tab where ((visibility = 0 or visibility = 2 or visibility = 3) and typeDef = ?) order by priority", cacheable=true),                                                      
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByAnonimousAndTypoDef", query = "from TabDynamicObject tab where (visibility = 3 and typeDef = ?) order by priority", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findAuthorizedGroupById", query = "select tab.authorizedGroup from TabDynamicObject tab where tab.id = ?"),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findAuthorizedGroupByShortname", query = "select tab.authorizedGroup from TabDynamicObject tab where tab.shortName = ?"),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findAuthorizedSingleById", query = "select tab.authorizedSingle from TabDynamicObject tab where tab.id = ?"),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findAuthorizedSingleByShortname", query = "select tab.authorizedSingle  from TabDynamicObject tab where tab.shortName = ?")
		
})
public class TabDynamicObject extends TypedAbstractTab<BoxDynamicObject, DynamicObjectType, DynamicPropertiesDefinition> {

	/** Showed holder in this tab */
	@ManyToMany	
	@JoinTable(name = "cris_do_tab2box", joinColumns = { 
            @JoinColumn(name = "cris_do_tab_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_do_box_id") })
	@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<BoxDynamicObject> mask;

	@ManyToMany
    @JoinTable(
          name="cris_do_tab2policysingle",
          joinColumns=@JoinColumn(name="tab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<DynamicPropertiesDefinition> authorizedSingle;
    
    @ManyToMany
    @JoinTable(
          name="cris_do_tab2policygroup",
          joinColumns=@JoinColumn(name="tab_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<DynamicPropertiesDefinition> authorizedGroup;
    
	@ManyToOne
	private DynamicObjectType typeDef;
	
	public TabDynamicObject() {
		this.visibility = VisibilityTabConstant.ADMIN;
	}
	
	@Override
	public List<BoxDynamicObject> getMask() {
		if(this.mask == null) {
			this.mask = new LinkedList<BoxDynamicObject>();
		}
		return this.mask;
	}

	@Override
	public void setMask(List<BoxDynamicObject> mask) {
		this.mask = mask;
	}


    @Override
    public String getFileSystemPath()
    {
        return ConfigurationManager.getProperty(CrisConstants.CFG_MODULE,"otherresearchobject.file.path");
    }

    public DynamicObjectType getTypeDef()
    {
        return typeDef;
    }

    public void setTypeDef(DynamicObjectType typeDef)
    {
        this.typeDef = typeDef;
    }
    
    public List<DynamicPropertiesDefinition> getAuthorizedSingle()
    {
        if(this.authorizedSingle==null) {
            this.authorizedSingle = new ArrayList<DynamicPropertiesDefinition>();
        }
        return authorizedSingle;
    }

    public void setAuthorizedSingle(List<DynamicPropertiesDefinition> authorizedSingle)
    {
        this.authorizedSingle = authorizedSingle; 
    }

    public List<DynamicPropertiesDefinition> getAuthorizedGroup()
    {
        if(this.authorizedGroup==null) {
            this.authorizedGroup = new ArrayList<DynamicPropertiesDefinition>();
        }
        return authorizedGroup;
    }

    public void setAuthorizedGroup(List<DynamicPropertiesDefinition> authorizedGroup)
    {
        this.authorizedGroup = authorizedGroup;
    }
    
}
