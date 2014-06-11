/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.Property;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

/**
 * @author pascarelli
 *
 */
@Entity
@Table(name="cris_do_no_prop", 
        uniqueConstraints = {@UniqueConstraint(columnNames={"positionDef","typo_id","parent_id"})})
@NamedQueries( {
    @NamedQuery(name = "DynamicNestedProperty.findPropertyByPropertiesDefinition", query = "from DynamicNestedProperty where typo = ? order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "DynamicNestedProperty.findAll", query = "from DynamicNestedProperty order by id", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "DynamicNestedProperty.findPropertyByParentAndTypo", query = "from DynamicNestedProperty  where (parent.id = ? and typo.id = ?) order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "DynamicNestedProperty.deleteAllPropertyByPropertiesDefinition", query = "delete from DynamicNestedProperty property where typo = ?)")
})
public class DynamicNestedProperty extends ANestedProperty<DynamicNestedPropertiesDefinition>
{
    

    @ManyToOne(fetch=FetchType.EAGER)
    @Fetch(FetchMode.SELECT)    
    private DynamicNestedPropertiesDefinition typo;
    
    
    @ManyToOne  
    @Index(name = "cris_do_no_pprop_idx")
    private DynamicNestedObject parent;

    @Override
    public DynamicNestedPropertiesDefinition getTypo()
    {
        return this.typo;
    }

    @Override
    public void setTypo(DynamicNestedPropertiesDefinition propertyDefinition)
    {
        this.typo = propertyDefinition;
    }

    @Override
    public void setParent(
            AnagraficaSupport<? extends Property<DynamicNestedPropertiesDefinition>, DynamicNestedPropertiesDefinition> parent)
    {
        this.parent = (DynamicNestedObject)parent;
    }

    @Override
    public AnagraficaSupport<DynamicNestedProperty, DynamicNestedPropertiesDefinition> getParent()
    {
        return parent;
    }

}
