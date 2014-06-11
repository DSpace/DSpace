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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@NamedQueries( {
    @NamedQuery(name = "DecoratorDynamicNestedPropertiesDefinition.findAll", query = "from DecoratorDynamicNestedPropertiesDefinition order by id"),
    @NamedQuery(name = "DecoratorDynamicNestedPropertiesDefinition.uniqueContainableByDecorable", query = "from DecoratorDynamicNestedPropertiesDefinition where real.id = ?"),
    @NamedQuery(name = "DecoratorDynamicNestedPropertiesDefinition.uniqueContainableByShortName", query = "from DecoratorDynamicNestedPropertiesDefinition where real.shortName = ?")
    
})
@DiscriminatorValue(value="pddonestedobject")
public class DecoratorDynamicNestedPropertiesDefinition extends
        ADecoratorNestedPropertiesDefinition<DynamicNestedPropertiesDefinition>
{

    
    @OneToOne(optional=true)
    @JoinColumn(name="cris_do_no_pdef_fk")
    @Cascade(value = {CascadeType.ALL,CascadeType.DELETE_ORPHAN})
    private DynamicNestedPropertiesDefinition real;
    
    
    @Override
    public Class getAnagraficaHolderClass()
    {
       return DynamicNestedObject.class;
    }

    @Override
    public Class getPropertyHolderClass()
    {
        return DynamicNestedProperty.class;
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
        DynamicNestedPropertiesDefinition oo = null;
        if(o instanceof DecoratorDynamicNestedPropertiesDefinition) {
            oo = (DynamicNestedPropertiesDefinition)o.getObject();
            return this.real.compareTo(oo);
        }
        return 0;
    }

    @Override
    public void setReal(DynamicNestedPropertiesDefinition object)
    {
       this.real = object;        
    }

    @Override
    public DynamicNestedPropertiesDefinition getObject()
    {
        return this.real;
    }   

}
