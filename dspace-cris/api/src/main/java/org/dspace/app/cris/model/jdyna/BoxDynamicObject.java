/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.web.Box;
import it.cilea.osd.jdyna.web.TypedBox;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "cris_do_box")
@org.hibernate.annotations.NamedQueries({
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findAll", query = "from BoxDynamicObject order by priority asc"),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findContainableByHolder", query = "from Containable containable where containable in (select m from BoxDynamicObject box join box.mask m where box.id = ?)", cacheable = true),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findHolderByContainable", query = "from BoxDynamicObject box where :par0 in elements(box.mask)", cacheable = true),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.uniqueBoxByShortName", query = "from BoxDynamicObject box where shortName = ?"),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findBoxByType", query = "from BoxDynamicObject box where box.typeDef = ?", cacheable=true)        
})
public class BoxDynamicObject extends TypedBox<Containable, DynamicObjectType, DynamicPropertiesDefinition>
{

    @ManyToMany
    @JoinTable(name = "cris_do_box2con", joinColumns = {
            @JoinColumn(name = "cris_do_box_id") },
            inverseJoinColumns = { @JoinColumn(name = "jdyna_containable_id") })
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<Containable> mask;
    
    @ManyToOne
    private DynamicObjectType typeDef;

    public BoxDynamicObject()
    {
        setVisibility(VisibilityTabConstant.ADMIN);
    }

    @Override
    public List<Containable> getMask()
    {
        if (this.mask == null)
        {
            this.mask = new LinkedList<Containable>();
        }
        return mask;
    }

    @Override
    public void setMask(List<Containable> mask)
    {
        if (mask != null)
        {
            Collections.sort(mask);
        }
        this.mask = mask;
    }

    public DynamicObjectType getTypeDef()
    {
        return typeDef;
    }

    public void setTypeDef(DynamicObjectType typeDef)
    {
        this.typeDef = typeDef;
    }

  

}
