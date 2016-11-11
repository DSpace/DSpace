/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import it.cilea.osd.common.service.IPersistenceService;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.web.ITabService;
import it.cilea.osd.jdyna.web.TypedBox;

@Entity
@Table(name = "cris_do_box")
@org.hibernate.annotations.NamedQueries({
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findAll", query = "from BoxDynamicObject order by priority asc"),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findContainableByHolder", query = "from Containable containable where containable in (select m from BoxDynamicObject box join box.mask m where box.id = ?)", cacheable = true),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findHolderByContainable", query = "from BoxDynamicObject box where :par0 in elements(box.mask)", cacheable = true),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.uniqueBoxByShortName", query = "from BoxDynamicObject box where shortName = ?"),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findBoxByType", query = "from BoxDynamicObject box where box.typeDef = ?", cacheable=true),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findAuthorizedGroupById", query = "select box.authorizedGroup from BoxDynamicObject box where box.id = ?"),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findAuthorizedGroupByShortname", query = "select box.authorizedGroup from BoxDynamicObject box where box.shortName = ?"),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findAuthorizedSingleById", query = "select box.authorizedSingle from BoxDynamicObject box where box.id = ?"),
        @org.hibernate.annotations.NamedQuery(name = "BoxDynamicObject.findAuthorizedSingleByShortname", query = "select box.authorizedSingle from BoxDynamicObject box where box.shortName = ?")
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

    @ManyToMany
    @JoinTable(
          name="cris_do_box2policysingle",
          joinColumns=@JoinColumn(name="box_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<DynamicPropertiesDefinition> authorizedSingle;
    
    @ManyToMany
    @JoinTable(
          name="cris_do_box2policygroup",
          joinColumns=@JoinColumn(name="box_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<DynamicPropertiesDefinition> authorizedGroup;
    
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

    
    public List<DynamicPropertiesDefinition> getAuthorizedSingle()
    {
        if(this.authorizedSingle==null) {
            this.authorizedSingle = new ArrayList<DynamicPropertiesDefinition>();
        }
        return authorizedSingle;
    }

    public void setAuthorizedSingle(List<DynamicPropertiesDefinition> authorizedSingle)
    {
        this.authorizedSingle = authorizedSingle; 
    }

    public List<DynamicPropertiesDefinition> getAuthorizedGroup()
    {
        if(this.authorizedGroup==null) {
            this.authorizedGroup = new ArrayList<DynamicPropertiesDefinition>();
        }
        return authorizedGroup;
    }

    public void setAuthorizedGroup(List<DynamicPropertiesDefinition> authorizedGroup)
    {
        this.authorizedGroup = authorizedGroup;
    }
    
}
