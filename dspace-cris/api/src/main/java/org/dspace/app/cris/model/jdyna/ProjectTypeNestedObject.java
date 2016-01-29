/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.AWidget;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
/**
*
* @author pascarelli
*
*/
@Entity
@Table(name = "cris_pj_no_tp")
@NamedQueries ({
    @NamedQuery(name="ProjectTypeNestedObject.findAll", query = "from ProjectTypeNestedObject order by id" ),
    @NamedQuery(name="ProjectTypeNestedObject.uniqueByShortName", query = "from ProjectTypeNestedObject where shortName = ?" ),
    @NamedQuery(name="ProjectTypeNestedObject.findMaskByShortName", query = "select dot.mask from ProjectTypeNestedObject dot where dot.shortName = ?" ),
    @NamedQuery(name="ProjectTypeNestedObject.findMaskById", query = "select dot.mask from ProjectTypeNestedObject dot where dot.id = ?" )
    
})
public class ProjectTypeNestedObject extends ATypeNestedObject<ProjectNestedPropertiesDefinition>
{
    @ManyToMany    
    @JoinTable(name = "cris_pj_no_tp2pdef", joinColumns = { 
            @JoinColumn(name = "cris_pj_no_tp_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_pj_no_pdef_id") })
    @Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<ProjectNestedPropertiesDefinition> mask;

    @Override
    public List<ProjectNestedPropertiesDefinition> getMask()
    {
        if(this.mask == null) {
            this.mask = new LinkedList<ProjectNestedPropertiesDefinition>();
        }
        return this.mask;
    }

    public void setMask(List<ProjectNestedPropertiesDefinition> mask) {
        this.mask = mask;
    }

    @Override
    public Class getDecoratorClass()
    {
        return DecoratorProjectTypeNested.class;
    }

    @Override
    public Class getAnagraficaHolderClass()
    {
        return ProjectNestedObject.class;
    }

    @Override
    public Class getPropertyHolderClass()
    {
        return ProjectNestedProperty.class;
    }

    @Override
    public AWidget getRendering()
    {        
        return null;
    }

    @Override
    public Class<ProjectNestedPropertiesDefinition> getClassPropertyDefinition()
    {
        return ProjectNestedPropertiesDefinition.class;
    }

}
