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

import org.dspace.app.cris.model.OrganizationUnit;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.OrderBy;

@Embeddable
@NamedQueries( {
    @NamedQuery(name = "OUAdditionalFieldStorage.findAll", query = "from OUAdditionalFieldStorage order by id"),
    @NamedQuery(name = "OUAdditionalFieldStorage.paginate.id.asc", query = "from OUAdditionalFieldStorage order by id asc"),
    @NamedQuery(name = "OUAdditionalFieldStorage.paginate.id.desc", query = "from OUAdditionalFieldStorage order by id desc"),  
    @NamedQuery(name = "OUAdditionalFieldStorage.paginateByTipologiaProprieta.value.asc", query = "select rpdyn from OUAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? order by anagrafica.value.sortValue asc"),
    @NamedQuery(name = "OUAdditionalFieldStorage.paginateByTipologiaProprieta.value.desc", query = "select rpdyn from OUAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? order by anagrafica.value.sortValue desc"),
    @NamedQuery(name = "OUAdditionalFieldStorage.paginateEmptyById.value.asc", query = "select rpdyn from OUAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from OUAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?) order by id asc"),
    @NamedQuery(name = "OUAdditionalFieldStorage.paginateEmptyById.value.desc", query = "select rpdyn from OUAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from OUAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?) order by id desc"),
    @NamedQuery(name = "OUAdditionalFieldStorage.countNotEmptyByTipologiaProprieta", query = "select count(rpdyn) from OUAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? "),
    @NamedQuery(name = "OUAdditionalFieldStorage.countEmptyByTipologiaProprieta", query = "select count(rpdyn) from OUAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from OUAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?)"),
    @NamedQuery(name = "OUAdditionalFieldStorage.count", query = "select count(*) from OUAdditionalFieldStorage")
})
public class OUAdditionalFieldStorage extends AnagraficaObject<OUProperty, OUPropertiesDefinition> {
    
    @OneToOne
    @JoinColumn(name = "id")    
    private OrganizationUnit organizationUnit;    

    @OneToMany(mappedBy = "parent")
    @LazyCollection(LazyCollectionOption.FALSE)
    @Cascade(value = { CascadeType.ALL, CascadeType.DELETE_ORPHAN })    
    @OrderBy(clause="positionDef asc")
    private List<OUProperty> anagrafica;
    
    public List<OUProperty> getAnagrafica() {
        if(this.anagrafica == null) {
            this.anagrafica = new LinkedList<OUProperty>();
        }
        return anagrafica;
    }

    public Class<OUProperty> getClassProperty() {
        return OUProperty.class;
    }

    public Class<OUPropertiesDefinition> getClassPropertiesDefinition() {
        return OUPropertiesDefinition.class;
    }

    public Integer getId() {
        return organizationUnit.getId();
    }

    public void setAnagraficaLazy(List<OUProperty> pp) {
        this.anagrafica = pp;       
    }

    public OrganizationUnit getOrganizationUnit()
    {
        return organizationUnit;
    }

    public void setOrganizationUnit(OrganizationUnit organizationUnit)
    {
        this.organizationUnit = organizationUnit;
    }

}
