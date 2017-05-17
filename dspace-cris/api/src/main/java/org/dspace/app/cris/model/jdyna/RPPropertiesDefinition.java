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
@Table(name="cris_rp_pdef")
@NamedQueries( {
    @NamedQuery(name = "RPPropertiesDefinition.findAll", query = "from RPPropertiesDefinition order by id", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "RPPropertiesDefinition.findValoriOnCreation", query = "from RPPropertiesDefinition where onCreation=true", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "RPPropertiesDefinition.findSimpleSearch", query = "from RPPropertiesDefinition where simpleSearch=true", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "RPPropertiesDefinition.findAdvancedSearch", query = "from RPPropertiesDefinition where advancedSearch=true", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "RPPropertiesDefinition.uniqueIdByShortName", query = "select id from RPPropertiesDefinition where shortName = ?", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "RPPropertiesDefinition.uniqueByShortName", query = "from RPPropertiesDefinition where shortName = ?", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "RPPropertiesDefinition.findValoriDaMostrare", query = "from RPPropertiesDefinition where showInList = true", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") }),
    @NamedQuery(name = "RPPropertiesDefinition.findAllWithPolicySingle", query = "from RPPropertiesDefinition where rendering in (from WidgetEPerson)"),
    @NamedQuery(name = "RPPropertiesDefinition.findAllWithPolicyGroup", query = "from RPPropertiesDefinition where rendering in (from WidgetGroup)"),
    @NamedQuery(name = "RPPropertiesDefinition.likeAllWithPolicySingle", query = "from RPPropertiesDefinition where shortName = ? and rendering in (from WidgetEPerson)"),
    @NamedQuery(name = "RPPropertiesDefinition.likeAllWithPolicyGroup", query = "from RPPropertiesDefinition where shortName = ? and rendering in (from WidgetGroup)"),
    @NamedQuery(name = "RPPropertiesDefinition.findAllWithCheckRadioDropdown", query = "from RPPropertiesDefinition where rendering in (from WidgetCheckRadio)"),
    @NamedQuery(name = "RPPropertiesDefinition.likeByShortName", query = "from RPPropertiesDefinition where shortName LIKE :par0", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") })
})
public class RPPropertiesDefinition extends PropertiesDefinition {
	
		
	@Transient
	public Class<RPAdditionalFieldStorage> getAnagraficaHolderClass() {
		return RPAdditionalFieldStorage.class;
	}

	@Transient
	public Class<RPProperty> getPropertyHolderClass() {
		return RPProperty.class;
	}
	
	@Override
	public Class<DecoratorRPPropertiesDefinition> getDecoratorClass() {
		return DecoratorRPPropertiesDefinition.class;
	}

}
