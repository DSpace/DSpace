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

import org.dspace.app.cris.model.Project;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

@Entity
@Table(name="cris_pj_prop", 
		uniqueConstraints = {@UniqueConstraint(columnNames={"positionDef","typo_id","parent_id"})})
@NamedQueries( {
    @NamedQuery(name = "ProjectProperty.findPropertyByPropertiesDefinition", query = "from ProjectProperty where typo = ? order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "ProjectProperty.findAll", query = "from ProjectProperty order by id", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "ProjectProperty.findPropertyByParentAndTypo", query = "from ProjectProperty  where (parent.id = ? and typo.id = ?) order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "ProjectProperty.deleteAllPropertyByPropertiesDefinition", query = "delete from ProjectProperty property where typo = ?)")
})
public class ProjectProperty extends Property<ProjectPropertiesDefinition> {
	
	@ManyToOne(fetch=FetchType.EAGER)
	@Fetch(FetchMode.SELECT)	
	private ProjectPropertiesDefinition typo;
	
	
	@ManyToOne
	@Index(name = "cris_pj_pprop_idx")
	private Project parent;
	

	@Override
	public ProjectPropertiesDefinition getTypo() {
		return typo;
	}

	@Override
	public void setTypo(ProjectPropertiesDefinition propertyDefinition) {
		this.typo = propertyDefinition;		
	}

    @Override
    public void setParent(
            AnagraficaSupport<? extends Property<ProjectPropertiesDefinition>, ProjectPropertiesDefinition> parent)
    {
        if(parent!=null) {
            this.parent = ((ProjectAdditionalFieldStorage)parent).getProject();
        }
        else {
            this.parent = null;
        }       
                
    }

    @Override
    public AnagraficaSupport<ProjectProperty, ProjectPropertiesDefinition> getParent()
    {
        return this.parent;

    }

   
}
