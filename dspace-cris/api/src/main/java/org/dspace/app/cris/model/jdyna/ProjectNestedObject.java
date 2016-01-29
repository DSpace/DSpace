/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.AType;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.Property;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.Project;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OrderBy;

/**
 * @author pascarelli
 *
 */
@Entity
@Table(name = "cris_pj_no", 
        uniqueConstraints = {@UniqueConstraint(columnNames={"positionDef","typo_id","parent_id"})})
@NamedQueries( {
        @NamedQuery(name = "ProjectNestedObject.findAll", query = "from ProjectNestedObject order by id"),
        @NamedQuery(name = "ProjectNestedObject.paginate.id.asc", query = "from ProjectNestedObject order by id asc"),
        @NamedQuery(name = "ProjectNestedObject.paginate.id.desc", query = "from ProjectNestedObject order by id desc"),
        @NamedQuery(name = "ProjectNestedObject.findNestedObjectsByParentIDAndTypoID", query = "from ProjectNestedObject where parent.id = ? and typo.id = ?"),
        @NamedQuery(name = "ProjectNestedObject.paginateNestedObjectsByParentIDAndTypoID.asc.asc", query = "from ProjectNestedObject where parent.id = ? and typo.id = ?"),
        @NamedQuery(name = "ProjectNestedObject.countNestedObjectsByParentIDAndTypoID", query = "select count(*) from ProjectNestedObject where parent.id = ? and typo.id = ?"),
        @NamedQuery(name = "ProjectNestedObject.findActiveNestedObjectsByParentIDAndTypoID", query = "from ProjectNestedObject where parent.id = ? and typo.id = ? and status = true"),
        @NamedQuery(name = "ProjectNestedObject.paginateActiveNestedObjectsByParentIDAndTypoID.asc.asc", query = "from ProjectNestedObject where parent.id = ? and typo.id = ? and status = true"),
        @NamedQuery(name = "ProjectNestedObject.countActiveNestedObjectsByParentIDAndTypoID", query = "select count(*) from ProjectNestedObject where parent.id = ? and typo.id = ? and status = true"),
        @NamedQuery(name = "ProjectNestedObject.findNestedObjectsByTypoID", query = "from ProjectNestedObject where typo.id = ?"),
        @NamedQuery(name = "ProjectNestedObject.findNestedObjectsByParentIDAndTypoShortname",  query = "from ProjectNestedObject where parent.id = ? and typo.shortName = ?"),
        @NamedQuery(name = "ProjectNestedObject.deleteNestedObjectsByTypoID", query = "delete from ProjectNestedObject where typo.id = ?"),
        @NamedQuery(name = "ProjectNestedObject.maxPositionNestedObjectsByTypoID", query = "select max(positionDef) from ProjectNestedObject where typo.id = ?"),
        @NamedQuery(name = "ProjectNestedObject.uniqueNestedObjectsByParentIdAndTypoIDAndSourceReference", query = "from ProjectNestedObject where parent.id = ? and typo.id = ? and sourceReference.sourceRef = ? and sourceReference.sourceID = ?"),
        @NamedQuery(name = "ProjectNestedObject.uniqueByUUID", query = "from ProjectNestedObject where uuid = ?")
        })
public class ProjectNestedObject extends ACrisNestedObject<ProjectNestedProperty, ProjectNestedPropertiesDefinition, ProjectProperty, ProjectPropertiesDefinition> 
{
    
    @OneToMany(mappedBy = "parent")
    @LazyCollection(LazyCollectionOption.FALSE)
    @Cascade(value = { CascadeType.ALL, CascadeType.DELETE_ORPHAN })    
    @OrderBy(clause="positionDef asc")
    private List<ProjectNestedProperty> anagrafica;

    @ManyToOne    
    private ProjectTypeNestedObject typo;

    @ManyToOne
    private Project parent;
    
    @Override
    public List<ProjectNestedProperty> getAnagrafica() {
        if(this.anagrafica == null) {
            this.anagrafica = new LinkedList<ProjectNestedProperty>();
        }
        return anagrafica;
    }
    

    @Override
    public Class<ProjectNestedProperty> getClassProperty()
    {
        return ProjectNestedProperty.class;
    }

    @Override
    public Class<ProjectNestedPropertiesDefinition> getClassPropertiesDefinition()
    {        
        return ProjectNestedPropertiesDefinition.class;
    }

    @Override
    public ProjectTypeNestedObject getTypo()
    {
        return typo;
    }


    @Override
    public Project getParent()
    {
        return parent;
    }


    @Override
    public void setTypo(AType<ProjectNestedPropertiesDefinition> typo)
    {
        this.typo = (ProjectTypeNestedObject)typo;
    }


    @Override
    public void setParent(
            AnagraficaSupport<? extends Property<ProjectPropertiesDefinition>, ProjectPropertiesDefinition> parent)
    {
        this.parent = (Project) parent;
    }

    @Override
    public Class getClassParent()
    {
        return Project.class;
    }


    @Override
    public int getType()
    {        
        return CrisConstants.NPROJECT_TYPE_ID;
    }

    @Override
    public String getPublicPath()
    {
        return parent.getPublicPath();
    }
    
    public ProjectNestedObject clone() throws CloneNotSupportedException
    {
        ProjectNestedObject clone = (ProjectNestedObject) super.clone();
        clone.duplicaAnagrafica(this);        
        return clone;
    }
}
