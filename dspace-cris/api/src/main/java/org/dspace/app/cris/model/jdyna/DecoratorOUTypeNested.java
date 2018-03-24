/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.ADecoratorTypeDefinition;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.IContainable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@NamedQueries( {
    @NamedQuery(name = "DecoratorOUTypeNested.findAll", query = "from DecoratorOUTypeNested order by id"),
    @NamedQuery(name = "DecoratorOUTypeNested.uniqueContainableByDecorable", query = "from DecoratorOUTypeNested where real.id = ?"),
    @NamedQuery(name = "DecoratorOUTypeNested.uniqueContainableByShortName", query = "from DecoratorOUTypeNested where real.shortName = ?")
    
})
@DiscriminatorValue(value="typeounestedobject")
public class DecoratorOUTypeNested extends
    ADecoratorTypeDefinition<OUTypeNestedObject, OUNestedPropertiesDefinition>
{

    @OneToOne(optional=true)
    @JoinColumn(name="cris_ou_no_tp_fk")
    @Cascade(value = {CascadeType.ALL,CascadeType.DELETE_ORPHAN})
    private OUTypeNestedObject real;

    @Override
    public String getShortName()
    {        
        return this.real.getShortName();
    }

    @Override
    public Integer getAccessLevel()
    {
        return this.real.getAccessLevel();
    }

    @Override
    public String getLabel()
    {
        return this.real.getLabel();
    }

    @Override
    public boolean getRepeatable()
    {
        return this.real.isRepeatable();
    }

    @Override
    public int getPriority()
    {
        return this.real.getPriority();
    }

    @Override
    public int compareTo(IContainable o) {
        OUTypeNestedObject oo = null;
        if(o instanceof DecoratorOUTypeNested) {
            oo = (OUTypeNestedObject)o.getObject();
            return this.real.compareTo(oo);
        }
        return 0;
    }
    
    @Override
    public void setReal(OUTypeNestedObject object)
    {
       this.real = object;        
    }

    @Override
    public OUTypeNestedObject getObject()
    {
        return real;
    }

    public boolean isNewline() {
        return real.isNewline();
    }

    @Override
    public Class getAnagraficaHolderClass()
    {        
        return getObject().getAnagraficaHolderClass();
    }

    @Override
    public Class getPropertyHolderClass()
    {
        return getObject().getPropertyHolderClass();
    }

    @Override
    public Class getDecoratorClass()
    {
        return getObject().getDecoratorClass();
    }

    @Override
    public AWidget getRendering()
    {
        return getObject().getRendering();
    }

    @Override
    public boolean isMandatory()
    {
        return getObject().isMandatory();
    }    
}
