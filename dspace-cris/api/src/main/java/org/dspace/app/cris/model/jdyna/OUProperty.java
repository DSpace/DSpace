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

import org.dspace.app.cris.model.OrganizationUnit;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

@Entity
@Table(name="cris_ou_prop", 
        uniqueConstraints = {@UniqueConstraint(columnNames={"positionDef","typo_id","parent_id"})})
@NamedQueries( {
    @NamedQuery(name = "OUProperty.findPropertyByPropertiesDefinition", query = "from OUProperty where typo = ? order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "OUProperty.findAll", query = "from OUProperty order by id", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "OUProperty.findPropertyByParentAndTypo", query = "from OUProperty  where (parent.id = ? and typo.id = ?) order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "OUProperty.deleteAllPropertyByPropertiesDefinition", query = "delete from OUProperty property where typo = ?)")
})
public class OUProperty extends Property<OUPropertiesDefinition> {
	
	@ManyToOne(fetch=FetchType.EAGER)
	@Fetch(FetchMode.SELECT)	
	private OUPropertiesDefinition typo;
	
	
	@ManyToOne	
	@Index(name = "cris_ou_pprop_idx")
	private OrganizationUnit parent;
	
	@Override
	public OUPropertiesDefinition getTypo() {
		return typo;
	}

	@Override
	public void setTypo(OUPropertiesDefinition propertyDefinition) {
		this.typo = propertyDefinition;		
	}
   

	@Override
	public AnagraficaSupport<OUProperty, OUPropertiesDefinition> getParent() {
		return parent;
	}

	@Override
	public void setParent(
			AnagraficaSupport<? extends Property<OUPropertiesDefinition>, OUPropertiesDefinition> parent) {
		if(parent!=null) {
		    this.parent = ((OUAdditionalFieldStorage)parent).getOrganizationUnit();
		}
		else {
		    this.parent = null;
		}		
	}

}
