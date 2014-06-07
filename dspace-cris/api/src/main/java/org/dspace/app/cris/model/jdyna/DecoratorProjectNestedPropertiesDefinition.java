/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.ADecoratorNestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.IContainable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@NamedQueries( {
    @NamedQuery(name = "DecoratorProjectNestedPropertiesDefinition.findAll", query = "from DecoratorProjectNestedPropertiesDefinition order by id"),
    @NamedQuery(name = "DecoratorProjectNestedPropertiesDefinition.uniqueContainableByDecorable", query = "from DecoratorProjectNestedPropertiesDefinition where real.id = ?"),
    @NamedQuery(name = "DecoratorProjectNestedPropertiesDefinition.uniqueContainableByShortName", query = "from DecoratorProjectNestedPropertiesDefinition where real.shortName = ?")
    
})
@DiscriminatorValue(value="pdprojectnestedobject")
public class DecoratorProjectNestedPropertiesDefinition extends
    ADecoratorNestedPropertiesDefinition<ProjectNestedPropertiesDefinition>
{

    @OneToOne(optional=true)
    @JoinColumn(name="cris_pj_no_pdef_fk")
    @Cascade(value = {CascadeType.ALL,CascadeType.DELETE_ORPHAN})
    private ProjectNestedPropertiesDefinition real;
    
    @Override
    public Class getAnagraficaHolderClass()
    {
       return this.real.getAnagraficaHolderClass();
    }

    @Override
    public Class getPropertyHolderClass()
    {
        return this.real.getPropertyHolderClass();
    }

    @Override
    public Class getDecoratorClass()
    {
        return real.getDecoratorClass();
    }

    @Transient
    public AWidget getRendering() {
        return this.real.getRendering();
    }

    @Transient
    public String getShortName() {
        return this.real.getShortName();
    }

    @Transient
    public boolean isMandatory() {
        return this.real.isMandatory();
    }

    @Transient
    public String getLabel() {
        return this.real.getLabel();
    }

    @Transient
    public int getPriority() {
        return this.real.getPriority();
    }

    @Transient
    public Integer getAccessLevel() {
        return this.real.getAccessLevel();
    }

    @Override
    public boolean getRepeatable() {
        return this.real.isRepeatable();
    }

    @Override
    public int compareTo(IContainable o) {
        ProjectNestedPropertiesDefinition oo = null;
        if(o instanceof DecoratorProjectNestedPropertiesDefinition) {
            oo = (ProjectNestedPropertiesDefinition)o.getObject();
            return this.real.compareTo(oo);
        }
        return 0;
    }

    @Override
    public void setReal(ProjectNestedPropertiesDefinition object)
    {
       this.real = object;        
    }

    @Override
    public ProjectNestedPropertiesDefinition getObject()
    {
        return this.real;
    }   
    
}
