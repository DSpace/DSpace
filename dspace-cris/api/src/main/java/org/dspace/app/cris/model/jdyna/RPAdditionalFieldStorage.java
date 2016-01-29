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
		@NamedQuery(name = "RPAdditionalFieldStorage.findAll", query = "from RPAdditionalFieldStorage order by id", cacheable=true),
    @NamedQuery(name = "RPAdditionalFieldStorage.paginate.id.asc", query = "from RPAdditionalFieldStorage order by id asc"),
    @NamedQuery(name = "RPAdditionalFieldStorage.paginate.id.desc", query = "from RPAdditionalFieldStorage order by id desc"),  
    @NamedQuery(name = "RPAdditionalFieldStorage.paginateByTipologiaProprieta.value.asc", query = "select rpdyn from RPAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? order by anagrafica.value.sortValue asc"),
    @NamedQuery(name = "RPAdditionalFieldStorage.paginateByTipologiaProprieta.value.desc", query = "select rpdyn from RPAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? order by anagrafica.value.sortValue desc"),
    @NamedQuery(name = "RPAdditionalFieldStorage.paginateEmptyById.value.asc", query = "select rpdyn from RPAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from RPAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?) order by id asc"),
    @NamedQuery(name = "RPAdditionalFieldStorage.paginateEmptyById.value.desc", query = "select rpdyn from RPAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from RPAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?) order by id desc"),
		@NamedQuery(name = "RPAdditionalFieldStorage.countNotEmptyByTipologiaProprieta", query = "select count(rpdyn) from RPAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ? ", cacheable=true),
		@NamedQuery(name = "RPAdditionalFieldStorage.countEmptyByTipologiaProprieta", query = "select count(rpdyn) from RPAdditionalFieldStorage rpdyn where rpdyn NOT IN (select rpdyn from RPAdditionalFieldStorage rpdyn left outer join rpdyn.anagrafica anagrafica where anagrafica.positionDef = 0 and anagrafica.typo.id = ?)", cacheable=true),
		@NamedQuery(name = "RPAdditionalFieldStorage.count", query = "select count(*) from RPAdditionalFieldStorage", cacheable=true) 
})
public class RPAdditionalFieldStorage extends
		AnagraficaObject<RPProperty, RPPropertiesDefinition> {
    
    @OneToOne
    @JoinColumn(name = "id")    
    private ResearcherPage researcherPage;    
    
	@OneToMany(mappedBy = "parent", orphanRemoval=true)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Cascade(value = { CascadeType.ALL })    
	@OrderBy(clause = "positionDef asc")
    private List<RPProperty> anagrafica;
    
    public List<RPProperty> getAnagrafica() {
		if (this.anagrafica == null) {
            this.anagrafica = new LinkedList<RPProperty>();
        }
        return anagrafica;
    }

    public Class<RPProperty> getClassProperty() {
        return RPProperty.class;
    }

    public Class<RPPropertiesDefinition> getClassPropertiesDefinition() {
        return RPPropertiesDefinition.class;
    }

    public Integer getId() {
        return researcherPage.getId();
    }

    public void setAnagraficaLazy(List<RPProperty> pp) {
        this.anagrafica = pp;       
    }

	public ResearcherPage getResearcherPage() {
        return researcherPage;
    }

	public void setResearcherPage(ResearcherPage researcherPage) {
        this.researcherPage = researcherPage;
    }

}
