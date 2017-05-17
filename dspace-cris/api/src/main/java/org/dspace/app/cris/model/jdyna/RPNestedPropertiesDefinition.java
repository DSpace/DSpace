/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
/**
 * @author pascarelli
 *
 */
@Entity
@Table(name="cris_rp_no_pdef")
@NamedQueries( {
    @NamedQuery(name = "RPNestedPropertiesDefinition.findAll", query = "from RPNestedPropertiesDefinition order by id"),    
    @NamedQuery(name = "RPNestedPropertiesDefinition.findValoriOnCreation", query = "from RPNestedPropertiesDefinition where onCreation=true"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.findSimpleSearch", query = "from RPNestedPropertiesDefinition where simpleSearch=true"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.findAdvancedSearch", query = "from RPNestedPropertiesDefinition where advancedSearch=true"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.uniqueIdByShortName", query = "select id from RPNestedPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.uniqueByShortName", query = "from RPNestedPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.findValoriDaMostrare", query = "from RPNestedPropertiesDefinition where showInList = true"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.findAllWithPolicySingle", query = "from RPNestedPropertiesDefinition where rendering in (from WidgetEPerson)"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.findAllWithPolicyGroup", query = "from RPNestedPropertiesDefinition where rendering in (from WidgetGroup)"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.likeAllWithPolicySingle", query = "from RPNestedPropertiesDefinition where shortName = ? and rendering in (from WidgetEPerson)"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.likeAllWithPolicyGroup", query = "from RPNestedPropertiesDefinition where shortName = ? and rendering in (from WidgetGroup)"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.findAllWithCheckRadioDropdown", query = "from RPNestedPropertiesDefinition where rendering in (from WidgetCheckRadio)"),
    @NamedQuery(name = "RPNestedPropertiesDefinition.likeByShortName", query = "from RPNestedPropertiesDefinition where shortName LIKE :par0", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") })
})
public class RPNestedPropertiesDefinition extends ANestedPropertiesDefinition
{

    @Override
    public Class getAnagraficaHolderClass()
    {
        return RPNestedObject.class;
    }

    @Override
    public Class getPropertyHolderClass()
    {
        return RPNestedProperty.class;
    }

    @Override
    public Class getDecoratorClass()
    {       
        return DecoratorRPNestedPropertiesDefinition.class;
    }
}
