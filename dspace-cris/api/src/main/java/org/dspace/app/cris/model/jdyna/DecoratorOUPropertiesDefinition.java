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

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@NamedQueries( {
    @NamedQuery(name = "DecoratorOUPropertiesDefinition.findAll", query = "from DecoratorOUPropertiesDefinition order by id"),
    @NamedQuery(name = "DecoratorOUPropertiesDefinition.uniqueContainableByDecorable", query = "from DecoratorOUPropertiesDefinition where real.id = ?"),
    @NamedQuery(name = "DecoratorOUPropertiesDefinition.uniqueContainableByShortName", query = "from DecoratorOUPropertiesDefinition where real.shortName = ?")    
})
@DiscriminatorValue(value="propertiesdefinitionou")
public class DecoratorOUPropertiesDefinition extends ADecoratorPropertiesDefinition<OUPropertiesDefinition>  
{
    @OneToOne(optional=true)
    @JoinColumn(name="cris_ou_pdef_fk")
    @Cascade(value = {CascadeType.ALL,CascadeType.DELETE_ORPHAN})
    private OUPropertiesDefinition real;
    

    @Override
    public void setReal(OUPropertiesDefinition real) {
        this.real = real;
    }
    
    @Override
    public OUPropertiesDefinition getObject() {
        return real;
    }

    @Transient
    public Class<OUAdditionalFieldStorage> getAnagraficaHolderClass() {
        return real.getAnagraficaHolderClass();
    }

    @Transient
    public Class<OUProperty> getPropertyHolderClass() {
        return real.getPropertyHolderClass();
    }

    public Class<DecoratorOUPropertiesDefinition> getDecoratorClass() {
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
        OUPropertiesDefinition oo = null;
        if(o instanceof DecoratorOUPropertiesDefinition) {
            oo = (OUPropertiesDefinition)o.getObject();
            return this.real.compareTo(oo);
        }
        return 0;
    }

}
