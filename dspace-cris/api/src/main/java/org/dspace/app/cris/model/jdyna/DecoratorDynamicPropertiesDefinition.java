/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.IContainable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.dspace.app.cris.model.ResearchObject;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@NamedQueries( {
    @NamedQuery(name = "DecoratorDynamicPropertiesDefinition.findAll", query = "from DecoratorDynamicPropertiesDefinition order by id"),
    @NamedQuery(name = "DecoratorDynamicPropertiesDefinition.uniqueContainableByDecorable", query = "from DecoratorDynamicPropertiesDefinition where real.id = ?"),
    @NamedQuery(name = "DecoratorDynamicPropertiesDefinition.uniqueContainableByShortName", query = "from DecoratorDynamicPropertiesDefinition where real.shortName = ?")
    
})
@DiscriminatorValue(value="propertiesdefinitiondynaobj")
public class DecoratorDynamicPropertiesDefinition extends
        ADecoratorPropertiesDefinition<DynamicPropertiesDefinition>
{

    
    @OneToOne(optional=true)
    @JoinColumn(name="cris_do_pdef_fk")
    @Cascade(value = {CascadeType.ALL,CascadeType.DELETE_ORPHAN})
    private DynamicPropertiesDefinition real;
    
    
    @Override
    public Class getAnagraficaHolderClass()
    {
       return ResearchObject.class;
    }

    @Override
    public Class getPropertyHolderClass()
    {
        return DynamicNestedProperty.class;
    }

    @Override
    public Class getDecoratorClass()
    {
        return DecoratorDynamicPropertiesDefinition.class;
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
        DynamicPropertiesDefinition oo = null;
        if(o instanceof DecoratorDynamicPropertiesDefinition) {
            oo = (DynamicPropertiesDefinition)o.getObject();
            return this.real.compareTo(oo);
        }
        return 0;
    }

    @Override
    public void setReal(DynamicPropertiesDefinition object)
    {
       this.real = object;        
    }

    @Override
    public DynamicPropertiesDefinition getObject()
    {
        return this.real;
    }   

}
