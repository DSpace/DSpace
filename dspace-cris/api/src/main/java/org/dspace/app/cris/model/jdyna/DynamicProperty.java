/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.Property;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.dspace.app.cris.model.ResearchObject;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

@Entity
@Table(name="cris_do_prop", 
        uniqueConstraints = {@UniqueConstraint(columnNames={"positionDef","typo_id","parent_id"})})
@NamedQueries( {
    @NamedQuery(name = "DynamicProperty.findPropertyByPropertiesDefinition", query = "from DynamicProperty where typo = ? order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "DynamicProperty.findAll", query = "from DynamicProperty order by id", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "DynamicProperty.findPropertyByParentAndTypo", query = "from DynamicProperty  where (parent.id = ? and typo.id = ?) order by positionDef", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "DynamicProperty.deleteAllPropertyByPropertiesDefinition", query = "delete from DynamicProperty property where typo = ?)")
})
public class DynamicProperty extends Property<DynamicPropertiesDefinition>
{

    @ManyToOne(fetch=FetchType.EAGER)
    @Fetch(FetchMode.SELECT)    
    private DynamicPropertiesDefinition typo;
    
    
    @ManyToOne  
    @Index(name = "cris_do_pprop_idx")
    private ResearchObject parent;
    
    @Override
    public DynamicPropertiesDefinition getTypo()
    {     
        return typo;
    }

    @Override
    public void setTypo(DynamicPropertiesDefinition propertyDefinition)
    {
        this.typo = propertyDefinition;        
    }

    @Override
    public void setParent(
            AnagraficaSupport<? extends Property<DynamicPropertiesDefinition>, DynamicPropertiesDefinition> parent)
    {
        if(parent!=null) {
            this.parent = ((DynamicAdditionalFieldStorage)parent).getDynamicObject();
        }
        else {
            this.parent = null;
        }
    }

    @Override
    public AnagraficaSupport<? extends Property<DynamicPropertiesDefinition>, DynamicPropertiesDefinition> getParent()
    {        
        return parent;
    }
}
