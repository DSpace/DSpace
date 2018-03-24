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
@NamedQueries({
        @NamedQuery(name = "DecoratorDynamicTypeNested.findAll", query = "from DecoratorDynamicTypeNested order by id"),
        @NamedQuery(name = "DecoratorDynamicTypeNested.uniqueContainableByDecorable", query = "from DecoratorDynamicTypeNested where real.id = ?"),
        @NamedQuery(name = "DecoratorDynamicTypeNested.uniqueContainableByShortName", query = "from DecoratorDynamicTypeNested where real.shortName = ?")

})
@DiscriminatorValue(value = "typedonestedobject")
public class DecoratorDynamicTypeNested
        extends
        ADecoratorTypeDefinition<DynamicTypeNestedObject, DynamicNestedPropertiesDefinition>
{

    @OneToOne(optional = true)
    @JoinColumn(name = "cris_do_no_tp_fk")
    @Cascade(value = { CascadeType.ALL, CascadeType.DELETE_ORPHAN })
    private DynamicTypeNestedObject real;

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
    public int compareTo(IContainable o)
    {
        DynamicTypeNestedObject oo = null;
        if (o instanceof DecoratorDynamicTypeNested)
        {
            oo = (DynamicTypeNestedObject) o.getObject();
            return this.real.compareTo(oo);
        }
        return 0;
    }

    @Override
    public void setReal(DynamicTypeNestedObject object)
    {
        this.real = object;
    }

    @Override
    public DynamicTypeNestedObject getObject()
    {
        return real;
    }

    public boolean isNewline()
    {
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
