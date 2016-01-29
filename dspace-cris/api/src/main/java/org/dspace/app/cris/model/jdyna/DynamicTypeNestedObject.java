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
@Table(name = "cris_do_no_tp")
@NamedQueries ({
    @NamedQuery(name="DynamicTypeNestedObject.findAll", query = "from DynamicTypeNestedObject order by id" ),
    @NamedQuery(name="DynamicTypeNestedObject.uniqueByShortName", query = "from DynamicTypeNestedObject where shortName = ?" ),
    @NamedQuery(name="DynamicTypeNestedObject.findMaskByShortName", query = "select dot.mask from DynamicTypeNestedObject dot where dot.shortName = ?" ),
    @NamedQuery(name="DynamicTypeNestedObject.findMaskById", query = "select dot.mask from DynamicTypeNestedObject dot where dot.id = ?" )
})
public class DynamicTypeNestedObject extends ATypeNestedObject<DynamicNestedPropertiesDefinition>
{
    
    @ManyToMany    
    @JoinTable(name = "cris_do_no_tp2pdef", joinColumns = { 
            @JoinColumn(name = "cris_do_no_tp_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_do_no_pdef_id") })    
    @Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<DynamicNestedPropertiesDefinition> mask;

    @Override
    public List<DynamicNestedPropertiesDefinition> getMask()
    {
        if(this.mask == null) {
            this.mask = new LinkedList<DynamicNestedPropertiesDefinition>();
        }
        return mask;
    }

    public void setMask(List<DynamicNestedPropertiesDefinition> mask) {
        this.mask = mask;
    }


    @Override
    public Class getDecoratorClass()
    {
        return DecoratorDynamicTypeNested.class;
    }

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
    public AWidget getRendering()
    {        
        return null;
    }

    @Override
    public Class<DynamicNestedPropertiesDefinition> getClassPropertyDefinition()
    {
        return DynamicNestedPropertiesDefinition.class;
    }
}
