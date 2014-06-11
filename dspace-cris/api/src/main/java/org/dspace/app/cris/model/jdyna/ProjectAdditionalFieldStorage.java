/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;


import it.cilea.osd.jdyna.model.AnagraficaObject;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.dspace.app.cris.model.Project;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.OrderBy;

@Embeddable
@NamedQueries( {
    @NamedQuery(name = "ProjectAdditionalFieldStorage.findAll", query = "from ProjectAdditionalFieldStorage order by id"),
    @NamedQuery(name = "ProjectAdditionalFieldStorage.paginate.id.asc", query = "from ProjectAdditionalFieldStorage order by id asc"),
    @NamedQuery(name = "ProjectAdditionalFieldStorage.paginate.id.desc", query = "from ProjectAdditionalFieldStorage order by id desc"),  
    @NamedQuery(name = "ProjectAdditionalFieldStorage.paginateByTipologiaProprieta.value.asc", query = "select rpdyn from ProjectAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? order by anagrafica.value.sortValue asc"),
    @NamedQuery(name = "ProjectAdditionalFieldStorage.paginateByTipologiaProprieta.value.desc", query = "select rpdyn from ProjectAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? order by anagrafica.value.sortValue desc"),
    @NamedQuery(name = "ProjectAdditionalFieldStorage.paginateEmptyById.value.asc", query = "select rpdyn from ProjectAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from ProjectAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?) order by id asc"),
    @NamedQuery(name = "ProjectAdditionalFieldStorage.paginateEmptyById.value.desc", query = "select rpdyn from ProjectAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from ProjectAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?) order by id desc"),
    @NamedQuery(name = "ProjectAdditionalFieldStorage.countNotEmptyByTipologiaProprieta", query = "select count(rpdyn) from ProjectAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? "),
    @NamedQuery(name = "ProjectAdditionalFieldStorage.countEmptyByTipologiaProprieta", query = "select count(rpdyn) from ProjectAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from ProjectAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?)"),
    @NamedQuery(name = "ProjectAdditionalFieldStorage.count", query = "select count(*) from ProjectAdditionalFieldStorage")
})
public class ProjectAdditionalFieldStorage extends AnagraficaObject<ProjectProperty, ProjectPropertiesDefinition> {
    
    @OneToOne
    @JoinColumn(name = "id")    
    private Project project;    

    @OneToMany(mappedBy = "parent")
    @LazyCollection(LazyCollectionOption.FALSE)
    @Cascade(value = { CascadeType.ALL, CascadeType.DELETE_ORPHAN })    
    @OrderBy(clause="positionDef asc")
    private List<ProjectProperty> anagrafica;
    
    public List<ProjectProperty> getAnagrafica() {
        if(this.anagrafica == null) {
            this.anagrafica = new LinkedList<ProjectProperty>();
        }
        return anagrafica;
    }

    public Class<ProjectProperty> getClassProperty() {
        return ProjectProperty.class;
    }

    public Class<ProjectPropertiesDefinition> getClassPropertiesDefinition() {
        return ProjectPropertiesDefinition.class;
    }

    public Integer getId() {
        return project.getId();
    }

    public void setAnagraficaLazy(List<ProjectProperty> pp) {
        this.anagrafica = pp;       
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }

  
 
}
