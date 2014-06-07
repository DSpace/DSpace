/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.web.TypedAbstractTab;

import java.util.LinkedList;
import java.util.List;

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
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByAdmin", query = "from TabDynamicObject tab where visibility = 1 or visibility = 2 or visibility = 3 order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByOwner", query = "from TabDynamicObject tab where visibility = 0 or visibility = 2 or visibility = 3 order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByAnonimous", query = "from TabDynamicObject tab where visibility = 3 order by priority", cacheable=true),
		@org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findTabByType", query = "from TabDynamicObject where typeDef = ?", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByAdminAndTypoDef", query = "from TabDynamicObject tab where ((visibility = 1 or visibility = 2 or visibility = 3) and typeDef = ?) order by priority", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByOwnerAndTypoDef", query = "from TabDynamicObject tab where ((visibility = 0 or visibility = 2 or visibility = 3) and typeDef = ?) order by priority", cacheable=true),                                                      
        @org.hibernate.annotations.NamedQuery(name = "TabDynamicObject.findByAnonimousAndTypoDef", query = "from TabDynamicObject tab where (visibility = 3 and typeDef = ?) order by priority", cacheable=true)
		
})
public class TabDynamicObject extends TypedAbstractTab<BoxDynamicObject, DynamicObjectType, DynamicPropertiesDefinition> {

	/** Showed holder in this tab */
	@ManyToMany	
	@JoinTable(name = "cris_do_tab2box", joinColumns = { 
            @JoinColumn(name = "cris_do_tab_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_do_box_id") })
	@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<BoxDynamicObject> mask;
	
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
    
   
}
