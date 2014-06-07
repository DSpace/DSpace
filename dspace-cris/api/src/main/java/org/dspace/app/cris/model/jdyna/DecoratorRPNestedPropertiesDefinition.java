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
    @NamedQuery(name = "DecoratorRPNestedPropertiesDefinition.findAll", query = "from DecoratorRPNestedPropertiesDefinition order by id"),
    @NamedQuery(name = "DecoratorRPNestedPropertiesDefinition.uniqueContainableByDecorable", query = "from DecoratorRPNestedPropertiesDefinition where real.id = ?"),
    @NamedQuery(name = "DecoratorRPNestedPropertiesDefinition.uniqueContainableByShortName", query = "from DecoratorRPNestedPropertiesDefinition where real.shortName = ?")
    
})
@DiscriminatorValue(value="pdrpnestedobject")
public class DecoratorRPNestedPropertiesDefinition extends
    ADecoratorNestedPropertiesDefinition<RPNestedPropertiesDefinition>
{

    @OneToOne(optional=true)
    @JoinColumn(name="cris_rp_no_pdef_fk")
    @Cascade(value = {CascadeType.ALL,CascadeType.DELETE_ORPHAN})
    private RPNestedPropertiesDefinition real;
    
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
        RPNestedPropertiesDefinition oo = null;
        if(o instanceof DecoratorRPNestedPropertiesDefinition) {
            oo = (RPNestedPropertiesDefinition)o.getObject();
            return this.real.compareTo(oo);
        }
        return 0;
    }

    @Override
    public void setReal(RPNestedPropertiesDefinition object)
    {
       this.real = object;        
    }

    @Override
    public RPNestedPropertiesDefinition getObject()
    {
        return this.real;
    }   
    
}
