/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.AType;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.Property;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OrderBy;

@Entity
@Table(name = "cris_rp_no", 
        uniqueConstraints = {@UniqueConstraint(columnNames={"positionDef","typo_id","parent_id"})})
@NamedQueries({
        @NamedQuery(name = "RPNestedObject.findAll", query = "from RPNestedObject order by id"),
        @NamedQuery(name = "RPNestedObject.paginate.id.asc", query = "from RPNestedObject order by id asc"),
        @NamedQuery(name = "RPNestedObject.paginate.id.desc", query = "from RPNestedObject order by id desc"),
        @NamedQuery(name = "RPNestedObject.findNestedObjectsByParentIDAndTypoID", query = "from RPNestedObject where parent.id = ? and typo.id = ?"),
        @NamedQuery(name = "RPNestedObject.paginateNestedObjectsByParentIDAndTypoID.asc.asc", query = "from RPNestedObject where parent.id = ? and typo.id = ?"),
        @NamedQuery(name = "RPNestedObject.countNestedObjectsByParentIDAndTypoID", query = "select count(*) from RPNestedObject where parent.id = ? and typo.id = ?"),
        @NamedQuery(name = "RPNestedObject.findActiveNestedObjectsByParentIDAndTypoID", query = "from RPNestedObject where parent.id = ? and typo.id = ? and status = true"),
        @NamedQuery(name = "RPNestedObject.paginateActiveNestedObjectsByParentIDAndTypoID.asc.asc", query = "from RPNestedObject where parent.id = ? and typo.id = ? and status = true"),
        @NamedQuery(name = "RPNestedObject.countActiveNestedObjectsByParentIDAndTypoID", query = "select count(*) from RPNestedObject where parent.id = ? and typo.id = ? and status = true"),
        @NamedQuery(name = "RPNestedObject.findNestedObjectsByTypoID", query = "from RPNestedObject where typo.id = ?"),
        @NamedQuery(name = "RPNestedObject.findNestedObjectsByParentIDAndTypoShortname",  query = "from RPNestedObject where parent.id = ? and typo.shortName = ?"),
        @NamedQuery(name = "RPNestedObject.deleteNestedObjectsByTypoID", query = "delete from RPNestedObject where typo.id = ?"),
        @NamedQuery(name = "RPNestedObject.maxPositionNestedObjectsByTypoID", query = "select max(positionDef) as max from RPNestedObject where typo.id = ?"),
        @NamedQuery(name = "RPNestedObject.uniqueNestedObjectsByParentIdAndTypoIDAndSourceReference", query = "from RPNestedObject where parent.id = ? and typo.id = ? and sourceReference.sourceRef = ? and sourceReference.sourceID = ?"),
        @NamedQuery(name = "RPNestedObject.uniqueByUUID", query = "from RPNestedObject where uuid = ?")
})
public class RPNestedObject
        extends
        ACrisNestedObject<RPNestedProperty, RPNestedPropertiesDefinition, RPProperty, RPPropertiesDefinition>        
{

    @OneToMany(mappedBy = "parent")
    @LazyCollection(LazyCollectionOption.FALSE)
    @Cascade(value = { CascadeType.ALL, CascadeType.DELETE_ORPHAN })
    @OrderBy(clause = "positionDef asc")
    private List<RPNestedProperty> anagrafica;

    @ManyToOne
    private RPTypeNestedObject typo;

    @ManyToOne(targetEntity = ResearcherPage.class)
    private ResearcherPage parent;

    @Override
    public List<RPNestedProperty> getAnagrafica()
    {
        if (this.anagrafica == null)
        {
            this.anagrafica = new LinkedList<RPNestedProperty>();
        }
        return anagrafica;
    }

    @Override
    public Class<RPNestedProperty> getClassProperty()
    {
        return RPNestedProperty.class;
    }

    @Override
    public Class<RPNestedPropertiesDefinition> getClassPropertiesDefinition()
    {
        return RPNestedPropertiesDefinition.class;
    }

    @Override
    public RPTypeNestedObject getTypo()
    {
        return typo;
    }
    
    public ResearcherPage getParent()
    {
        return parent;
    }

    @Override
    public void setTypo(AType<RPNestedPropertiesDefinition> typo)
    {
        this.typo = (RPTypeNestedObject)typo;
    }
  
    @Override
    public void setParent(
            AnagraficaSupport<? extends Property<RPPropertiesDefinition>, RPPropertiesDefinition> parent)
    {
        this.parent = (ResearcherPage) parent;
    }

    @Override
    public Class getClassParent()
    {
        return ResearcherPage.class;
    }

    @Override
    public int getType()
    {
        return CrisConstants.NRP_TYPE_ID;
    }

    @Override
    public String getPublicPath()
    {
        return parent.getPublicPath();
    }

    public RPNestedObject clone() throws CloneNotSupportedException
    {
        RPNestedObject clone = (RPNestedObject) super.clone();
        clone.duplicaAnagrafica(this);        
        return clone;
    }
}
