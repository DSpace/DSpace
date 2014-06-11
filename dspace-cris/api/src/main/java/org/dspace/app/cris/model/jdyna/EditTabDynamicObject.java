/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.web.TypedAbstractEditTab;

import java.util.LinkedList;
import java.util.List;

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

@Entity
@Table(name = "cris_do_etab")
@NamedQueries({
		@NamedQuery(name = "EditTabDynamicObject.findAll", query = "from EditTabDynamicObject order by priority asc"),
		@NamedQuery(name = "EditTabDynamicObject.findPropertyHolderInTab", query = "from BoxDynamicObject box where box in (select m from EditTabDynamicObject tab join tab.mask m where tab.id = ?) order by priority"),
		@NamedQuery(name = "EditTabDynamicObject.findTabsByHolder", query = "from EditTabDynamicObject tab where :par0 in elements(tab.mask)"),
		@NamedQuery(name = "EditTabDynamicObject.uniqueByDisplayTab", query = "from EditTabDynamicObject tab where displayTab.id = ?"),
		@NamedQuery(name = "EditTabDynamicObject.uniqueTabByShortName", query = "from EditTabDynamicObject tab where shortName = ?"), 
		@NamedQuery(name = "EditTabDynamicObject.findByAccessLevel", query = "from EditTabDynamicObject tab where visibility = ? order by priority"),
        @NamedQuery(name = "EditTabDynamicObject.findByAdmin", query = "from EditTabDynamicObject tab where visibility = 1 or visibility = 2 or visibility = 3 order by priority"),
        @NamedQuery(name = "EditTabDynamicObject.findByOwner", query = "from EditTabDynamicObject tab where visibility = 0 or visibility = 2 or visibility = 3 order by priority"),
        @NamedQuery(name = "EditTabDynamicObject.findByAnonimous", query = "from EditTabDynamicObject tab where visibility = 3 order by priority"),
        @NamedQuery(name = "EditTabDynamicObject.findTabByType", query = "from EditTabDynamicObject where typeDef = ?"),
        @NamedQuery(name = "EditTabDynamicObject.findByAdminAndTypoDef", query = "from EditTabDynamicObject tab where ((visibility = 1 or visibility = 2 or visibility = 3) and typeDef = ?) order by priority"),
        @NamedQuery(name = "EditTabDynamicObject.findByOwnerAndTypoDef", query = "from EditTabDynamicObject tab where ((visibility = 0 or visibility = 2 or visibility = 3) and typeDef = ?) order by priority"),
        @NamedQuery(name = "EditTabDynamicObject.findByAnonimousAndTypoDef", query = "from EditTabDynamicObject tab where (visibility = 3 and typeDef = ?) order by priority")        
})
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EditTabDynamicObject extends
		TypedAbstractEditTab<BoxDynamicObject,DynamicObjectType, DynamicPropertiesDefinition, TabDynamicObject> {

	/** Showed holder in this tab */
	@ManyToMany	
	@JoinTable(name = "cris_do_etab2box", joinColumns = { 
            @JoinColumn(name = "cris_do_etab_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_do_box_id") })
	@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<BoxDynamicObject> mask;

	@OneToOne
	private TabDynamicObject displayTab;

	@ManyToOne
	private DynamicObjectType typeDef;
	
	public EditTabDynamicObject() {
		this.visibility = VisibilityTabConstant.ADMIN;
	}
	
	@Override
	public List<BoxDynamicObject> getMask() {
		if (this.mask == null) {
			this.mask = new LinkedList<BoxDynamicObject>();
		}
		return this.mask;
	}

	@Override
	public void setMask(List<BoxDynamicObject> mask) {
		this.mask = mask;
	}

	public void setDisplayTab(TabDynamicObject displayTab) {
		this.displayTab = displayTab;
	}

	public TabDynamicObject getDisplayTab() {
		return displayTab;
	}


	@Override
	public Class<TabDynamicObject> getDisplayTabClass() {
		return TabDynamicObject.class;
	}
	
    @Override
    public String getFileSystemPath()
    {
        return ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "otherresearchobject.file.path");
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
