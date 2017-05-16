/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.PropertiesDefinition;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="cris_ou_pdef")
@NamedQueries( {
    @NamedQuery(name = "OUPropertiesDefinition.findAll", query = "from OUPropertiesDefinition order by id"),    
    @NamedQuery(name = "OUPropertiesDefinition.findValoriOnCreation", query = "from OUPropertiesDefinition where onCreation=true"),
    @NamedQuery(name = "OUPropertiesDefinition.findSimpleSearch", query = "from OUPropertiesDefinition where simpleSearch=true"),
    @NamedQuery(name = "OUPropertiesDefinition.findAdvancedSearch", query = "from OUPropertiesDefinition where advancedSearch=true"),
    @NamedQuery(name = "OUPropertiesDefinition.uniqueIdByShortName", query = "select id from OUPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "OUPropertiesDefinition.uniqueByShortName", query = "from OUPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "OUPropertiesDefinition.findValoriDaMostrare", query = "from OUPropertiesDefinition where showInList = true"),
    @NamedQuery(name = "OUPropertiesDefinition.findAllWithPolicySingle", query = "from OUPropertiesDefinition where rendering in (from WidgetEPerson)"),
    @NamedQuery(name = "OUPropertiesDefinition.findAllWithPolicyGroup", query = "from OUPropertiesDefinition where rendering in (from WidgetGroup)"),
    @NamedQuery(name = "OUPropertiesDefinition.likeAllWithPolicySingle", query = "from OUPropertiesDefinition where shortName = ? and rendering in (from WidgetEPerson)"),
    @NamedQuery(name = "OUPropertiesDefinition.likeAllWithPolicyGroup", query = "from OUPropertiesDefinition where shortName = ? and rendering in (from WidgetGroup)"),
    @NamedQuery(name = "OUPropertiesDefinition.findAllWithCheckRadioDropdown", query = "from OUPropertiesDefinition where rendering in (from WidgetCheckRadio)"),
    @NamedQuery(name = "OUPropertiesDefinition.likeByShortName", query = "from OUPropertiesDefinition where shortName LIKE :par0", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") })
})
public class OUPropertiesDefinition extends PropertiesDefinition {
	
		
	@Transient
	public Class<OUAdditionalFieldStorage> getAnagraficaHolderClass() {
		return OUAdditionalFieldStorage.class;
	}

	@Transient
	public Class<OUProperty> getPropertyHolderClass() {
		return OUProperty.class;
	}
	
	@Override
	public Class<DecoratorOUPropertiesDefinition> getDecoratorClass() {
		return DecoratorOUPropertiesDefinition.class;
	}

}
