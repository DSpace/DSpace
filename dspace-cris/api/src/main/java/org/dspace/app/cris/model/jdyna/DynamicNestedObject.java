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
import java.util.Random;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.math.RandomUtils;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.OrderBy;

/**
 * @author pascarelli
 * 
 */
@Entity
@Table(name = "cris_do_no", uniqueConstraints = { @UniqueConstraint(columnNames = {
        "positionDef", "typo_id", "parent_id" }) })
@NamedQueries({
        @NamedQuery(name = "DynamicNestedObject.findAll", query = "from DynamicNestedObject order by id"),
        @NamedQuery(name = "DynamicNestedObject.paginate.id.asc", query = "from DynamicNestedObject order by id asc"),
        @NamedQuery(name = "DynamicNestedObject.paginate.id.desc", query = "from DynamicNestedObject order by id desc"),
        @NamedQuery(name = "DynamicNestedObject.findNestedObjectsByParentIDAndTypoID", query = "from DynamicNestedObject where parent.id = ? and typo.id = ?"),
        @NamedQuery(name = "DynamicNestedObject.paginateNestedObjectsByParentIDAndTypoID.asc.asc", query = "from DynamicNestedObject where parent.id = ? and typo.id = ?"),
        @NamedQuery(name = "DynamicNestedObject.countNestedObjectsByParentIDAndTypoID", query = "select count(*) from DynamicNestedObject where parent.id = ? and typo.id = ?"),
        @NamedQuery(name = "DynamicNestedObject.findActiveNestedObjectsByParentIDAndTypoID", query = "from DynamicNestedObject where parent.id = ? and typo.id = ? and status = true"),
        @NamedQuery(name = "DynamicNestedObject.paginateActiveNestedObjectsByParentIDAndTypoID.asc.asc", query = "from DynamicNestedObject where parent.id = ? and typo.id = ? and status = true"),
        @NamedQuery(name = "DynamicNestedObject.countActiveNestedObjectsByParentIDAndTypoID", query = "select count(*) from DynamicNestedObject where parent.id = ? and typo.id = ? and status = true"),
        @NamedQuery(name = "DynamicNestedObject.findNestedObjectsByTypoID", query = "from DynamicNestedObject where typo.id = ?"),
        @NamedQuery(name = "DynamicNestedObject.findNestedObjectsByParentIDAndTypoShortname", query = "from DynamicNestedObject where parent.id = ? and typo.shortName = ?"),
        @NamedQuery(name = "DynamicNestedObject.deleteNestedObjectsByTypoID", query = "delete from DynamicNestedObject where typo.id = ?"),
        @NamedQuery(name = "DynamicNestedObject.maxPositionNestedObjectsByTypoID", query = "select max(positionDef) as max from DynamicNestedObject where typo.id = ?"),
        @NamedQuery(name = "DynamicNestedObject.uniqueNestedObjectsByParentIdAndTypoIDAndSourceReference", query = "from DynamicNestedObject where parent.id = ? and typo.id = ? and sourceReference.sourceRef = ? and sourceReference.sourceID = ?"),
        @NamedQuery(name = "DynamicNestedObject.uniqueByUUID", query = "from DynamicNestedObject where uuid = ?")        
})

public class DynamicNestedObject
        extends
        ACrisNestedObject<DynamicNestedProperty, DynamicNestedPropertiesDefinition, DynamicProperty, DynamicPropertiesDefinition>
{

    @OneToMany(mappedBy = "parent")
    @Cascade(value = { CascadeType.ALL, CascadeType.DELETE_ORPHAN })
    @OrderBy(clause = "positionDef asc")
    private List<DynamicNestedProperty> anagrafica;

    @ManyToOne
    private DynamicTypeNestedObject typo;

    @ManyToOne(targetEntity = ResearchObject.class)
    private ResearchObject parent;

    @Override
    public List<DynamicNestedProperty> getAnagrafica()
    {
        if (this.anagrafica == null)
        {
            this.anagrafica = new LinkedList<DynamicNestedProperty>();
        }
        return anagrafica;
    }

    @Override
    public Class getClassProperty()
    {
        return DynamicNestedProperty.class;
    }

    @Override
    public Class getClassPropertiesDefinition()
    {
        return DynamicNestedPropertiesDefinition.class;
    }

    @Override
    public DynamicTypeNestedObject getTypo()
    {
        return typo;
    }

    @Override
    public void setTypo(AType<DynamicNestedPropertiesDefinition> typo)
    {
        this.typo = (DynamicTypeNestedObject) typo;
    }

    public ResearchObject getParent()
    {
        return parent;
    }

    @Override
    public Class getClassParent()
    {
        return ResearchObject.class;
    }

    @Override
    public void setParent(
            AnagraficaSupport<? extends Property<DynamicPropertiesDefinition>, DynamicPropertiesDefinition> parent)
    {
        this.parent = (ResearchObject) parent;
    }

    @Override
    public String getPublicPath()
    {
        return parent.getPublicPath();
    }

    @Override
    public int getType()
    {
        return getTypo().getId() + CrisConstants.CRIS_NDYNAMIC_TYPE_ID_START;
    }

    public DynamicNestedObject clone() throws CloneNotSupportedException
    {
        DynamicNestedObject clone = (DynamicNestedObject) super.clone();
        clone.duplicaAnagrafica(this);        
        return clone;
    }
}
