/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.ATypeWithTypeNestedObjectSupport;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * 
 * 
 * @author pascarelli
 * 
 */
@Entity
@Table(name = "cris_do_tp")
@NamedQueries({
        @NamedQuery(name = "DynamicObjectType.findAll", query = "from DynamicObjectType order by id"),
        @NamedQuery(name = "DynamicObjectType.paginate.id.asc", query = "from DynamicObjectType order by id asc"),
        @NamedQuery(name = "DynamicObjectType.paginate.id.desc", query = "from DynamicObjectType order by id desc"),
        @NamedQuery(name = "DynamicObjectType.uniqueByShortName", query = "from DynamicObjectType where shortName = ?"),
        @NamedQuery(name="DynamicObjectType.findMaskByShortName", query = "select dot.mask from DynamicObjectType dot where dot.shortName = ?" ),
        @NamedQuery(name="DynamicObjectType.findMaskById", query = "select dot.mask from DynamicObjectType dot where dot.id = ?" ),
        @NamedQuery(name="DynamicObjectType.findNestedMaskById", query = "select dot.typeNestedDefinitionMask from DynamicObjectType dot where dot.id = ?" )
})        
public class DynamicObjectType extends ATypeWithTypeNestedObjectSupport<DynamicPropertiesDefinition, DynamicTypeNestedObject, DynamicNestedPropertiesDefinition>
{

    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_TYPODYNAOBJ_SEQ")
    @SequenceGenerator(name = "CRIS_TYPODYNAOBJ_SEQ", sequenceName = "CRIS_TYPODYNAOBJ_SEQ", allocationSize = 1)    
    private Integer id;
    
    @ManyToMany    
    @JoinTable(name = "cris_do_tp2pdef", joinColumns = { 
            @JoinColumn(name = "cris_do_tp_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_do_pdef_id") })
    @Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<DynamicPropertiesDefinition> mask;

    @ManyToMany    
    @JoinTable(name = "cris_do_tp2notp", joinColumns = { 
            @JoinColumn(name = "cris_do_tp_id") }, 
            inverseJoinColumns = { @JoinColumn(name = "cris_do_no_tp_id") })
    @Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<DynamicTypeNestedObject> typeNestedDefinitionMask;
    
    @Override
    public List<DynamicPropertiesDefinition> getMask()
    {
        if(this.mask==null) {
            this.mask = new LinkedList<DynamicPropertiesDefinition>();
        }
        return mask;
    }

    public void setMask(List<DynamicPropertiesDefinition> mask) {
        this.mask = mask;
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    @Override
    public Class<DynamicPropertiesDefinition> getClassPropertyDefinition()
    {
        return DynamicPropertiesDefinition.class;
    }

    @Override
    public List<DynamicTypeNestedObject> getTypeNestedDefinitionMask()
    {
        if(this.typeNestedDefinitionMask==null) {
            this.typeNestedDefinitionMask = new LinkedList<DynamicTypeNestedObject>();
        }
        return this.typeNestedDefinitionMask;
    }

    @Override
    public Class<DynamicTypeNestedObject> getClassTypeNestedObject()
    {
        return DynamicTypeNestedObject.class;
    }

    @Override
    public Class<DynamicNestedPropertiesDefinition> getClassNestedPropertyDefinition()
    {
        return DynamicNestedPropertiesDefinition.class;
    }

    public void setTypeNestedDefinitionMask(
            List<DynamicTypeNestedObject> typeNestedDefinitionMask)
    {
        this.typeNestedDefinitionMask = typeNestedDefinitionMask;
    }
}
