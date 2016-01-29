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
@Table(name = "cris_ou_no_tp")
@NamedQueries ({
    @NamedQuery(name="OUTypeNestedObject.findAll", query = "from OUTypeNestedObject order by id" ),
    @NamedQuery(name="OUTypeNestedObject.uniqueByShortName", query = "from OUTypeNestedObject where shortName = ?" ),
    @NamedQuery(name="OUTypeNestedObject.findMaskByShortName", query = "select dot.mask from OUTypeNestedObject dot where dot.shortName = ?" ),
    @NamedQuery(name="OUTypeNestedObject.findMaskById", query = "select dot.mask from OUTypeNestedObject dot where dot.id = ?" )
})
public class OUTypeNestedObject extends ATypeNestedObject<OUNestedPropertiesDefinition>
{
    @ManyToMany    
    @JoinTable(name = "cris_ou_no_tp2pdef", joinColumns = { 
            @JoinColumn(name = "cris_ou_no_tp_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_ou_no_pdef_id") })
    @Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<OUNestedPropertiesDefinition> mask;

    @Override
    public List<OUNestedPropertiesDefinition> getMask()
    {
        if(this.mask == null) {
            this.mask = new LinkedList<OUNestedPropertiesDefinition>();
        }
        return this.mask;
    }

    public void setMask(List<OUNestedPropertiesDefinition> mask) {
        this.mask = mask;
    }

    @Override
    public Class getDecoratorClass()
    {
        return DecoratorOUTypeNested.class;
    }

    @Override
    public Class getAnagraficaHolderClass()
    {
        return OUNestedObject.class;
    }

    @Override
    public Class getPropertyHolderClass()
    {
        return OUNestedProperty.class;
    }

    @Override
    public AWidget getRendering()
    {        
        return null;
    }

    @Override
    public Class<OUNestedPropertiesDefinition> getClassPropertyDefinition()
    {
        return OUNestedPropertiesDefinition.class;
    }
}
