/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.AnagraficaObject;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.OrderBy;

@Embeddable
@NamedQueries({
        @NamedQuery(name = "DynamicAdditionalFieldStorage.findAll", query = "from DynamicAdditionalFieldStorage order by id", cacheable = true),
        @NamedQuery(name = "DynamicAdditionalFieldStorage.paginate.id.asc", query = "from DynamicAdditionalFieldStorage order by id asc"),
        @NamedQuery(name = "DynamicAdditionalFieldStorage.paginate.id.desc", query = "from DynamicAdditionalFieldStorage order by id desc"),
        @NamedQuery(name = "DynamicAdditionalFieldStorage.paginateByTipologiaProprieta.value.asc", query = "select rpdyn from DynamicAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? order by anagrafica.value.sortValue asc"),
        @NamedQuery(name = "DynamicAdditionalFieldStorage.paginateByTipologiaProprieta.value.desc", query = "select rpdyn from DynamicAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? order by anagrafica.value.sortValue desc"),
        @NamedQuery(name = "DynamicAdditionalFieldStorage.paginateEmptyById.value.asc", query = "select rpdyn from DynamicAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from DynamicAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?) order by id asc"),
        @NamedQuery(name = "DynamicAdditionalFieldStorage.paginateEmptyById.value.desc", query = "select rpdyn from DynamicAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from DynamicAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?) order by id desc"),
        @NamedQuery(name = "DynamicAdditionalFieldStorage.countNotEmptyByTipologiaProprieta", query = "select count(rpdyn) from DynamicAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? ", cacheable = true),
        @NamedQuery(name = "DynamicAdditionalFieldStorage.countEmptyByTipologiaProprieta", query = "select count(rpdyn) from DynamicAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from DynamicAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?)", cacheable = true),
        @NamedQuery(name = "DynamicAdditionalFieldStorage.count", query = "select count(*) from DynamicAdditionalFieldStorage", cacheable = true) })
public class DynamicAdditionalFieldStorage extends
        AnagraficaObject<DynamicProperty, DynamicPropertiesDefinition>
{

    @OneToOne
    @JoinColumn(name = "id")
    private ResearchObject dynamicObject;

    @OneToMany(mappedBy = "parent")
    @LazyCollection(LazyCollectionOption.FALSE)
    @Cascade(value = { CascadeType.ALL, CascadeType.DELETE_ORPHAN })
    @OrderBy(clause = "positionDef asc")
    private List<DynamicProperty> anagrafica;

    public List<DynamicProperty> getAnagrafica()
    {
        if (this.anagrafica == null)
        {
            this.anagrafica = new LinkedList<DynamicProperty>();
        }
        return anagrafica;
    }

    public Class<DynamicProperty> getClassProperty()
    {
        return DynamicProperty.class;
    }

    public Class<DynamicPropertiesDefinition> getClassPropertiesDefinition()
    {
        return DynamicPropertiesDefinition.class;
    }

    public Integer getId()
    {
        return dynamicObject.getId();
    }

    public void setAnagraficaLazy(List<DynamicProperty> pp)
    {
        this.anagrafica = pp;
    }

    public ResearchObject getDynamicObject()
    {
        return dynamicObject;
    }

    public void setDynamicObject(ResearchObject dynamicObject)
    {
        this.dynamicObject = dynamicObject;
    }

}
