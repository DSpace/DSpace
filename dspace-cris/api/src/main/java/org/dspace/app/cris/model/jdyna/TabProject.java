/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.web.AbstractTab;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.core.ConfigurationManager;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="cris_pj_tab")
@NamedQueries( {
        @NamedQuery(name = "TabProject.findAll", query = "from TabProject order by priority asc"),
        @NamedQuery(name = "TabProject.findPropertyHolderInTab", query = "from BoxProject box where box in (select m from TabProject tab join tab.mask m where tab.id = ?) order by priority"),
        @NamedQuery(name = "TabProject.findTabsByHolder", query = "from TabProject tab where :par0 in elements(tab.mask)"),
        @NamedQuery(name = "TabProject.uniqueTabByShortName", query = "from TabProject tab where shortName = ?"),
		@NamedQuery(name = "TabProject.findByAccessLevel", query = "from TabProject tab where visibility = ? order by priority"),
        @NamedQuery(name = "TabProject.findByAdmin", query = "from TabProject tab where visibility = 1 or visibility = 2 or visibility = 3 order by priority"),
        @NamedQuery(name = "TabProject.findByOwner", query = "from TabProject tab where visibility = 0 or visibility = 2 or visibility = 3 order by priority"),
        @NamedQuery(name = "TabProject.findByAnonimous", query = "from TabProject tab where visibility = 3 order by priority")
		
})
public class TabProject extends AbstractTab<BoxProject> {

	/** Showed holder in this tab */
	@ManyToMany	
	@JoinTable(name = "cris_pj_tab2box", joinColumns = { 
            @JoinColumn(name = "cris_pj_tab_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_pj_box_id") })
	@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	private List<BoxProject> mask;

	
	public TabProject() {
		this.visibility = VisibilityTabConstant.ADMIN;
	}
	
	@Override
	public List<BoxProject> getMask() {
		if(this.mask == null) {
			this.mask = new LinkedList<BoxProject>();
		}
		return this.mask;
	}

	@Override
	public void setMask(List<BoxProject> mask) {
		this.mask = mask;
	}
	
    @Override
    public String getFileSystemPath()
    {
        return ConfigurationManager.getProperty(CrisConstants.CFG_MODULE,"project.file.path");
    }
}
