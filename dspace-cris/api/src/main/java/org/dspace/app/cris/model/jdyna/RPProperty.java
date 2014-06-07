/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.Property;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.dspace.app.cris.model.ResearcherPage;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

@Entity
@Table(name="cris_rp_prop", 
        uniqueConstraints = {@UniqueConstraint(columnNames={"positionDef","typo_id","parent_id"})})
@NamedQueries( {
	@NamedQuery(name = "RPProperty.findPropertyByPropertiesDefinition", query = "from RPProperty where typo = ? order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
	@NamedQuery(name = "RPProperty.findAll", query = "from RPProperty order by id", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
	@NamedQuery(name = "RPProperty.findPropertyByParentAndTypo", query = "from RPProperty  where (parent.id = ? and typo.id = ?) order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
	@NamedQuery(name = "RPProperty.deleteAllPropertyByPropertiesDefinition", query = "delete from RPProperty property where typo = ?)")
})
public class RPProperty extends Property<RPPropertiesDefinition> {
	
	@ManyToOne(fetch=FetchType.EAGER)
	@Fetch(FetchMode.SELECT)	
	private RPPropertiesDefinition typo;
	
	
	@ManyToOne	
	@Index(name = "cris_rp_pprop_idx")
	private ResearcherPage parent;
	
	@Override
	public RPPropertiesDefinition getTypo() {
		return typo;
	}

	@Override
	public void setTypo(RPPropertiesDefinition propertyDefinition) {
		this.typo = propertyDefinition;		
	}

   

	@Override
	public AnagraficaSupport<RPProperty, RPPropertiesDefinition> getParent() {
		return parent;
	}

	@Override
	public void setParent(
			AnagraficaSupport<? extends Property<RPPropertiesDefinition>, RPPropertiesDefinition> parent) {
		if(parent!=null) {
		    this.parent = ((RPAdditionalFieldStorage)parent).getResearcherPage();
		}
		else {
		    this.parent = null;
		}		
	}

}
