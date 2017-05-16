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
@Table(name="cris_ou_no_pdef")
@NamedQueries( {
    @NamedQuery(name = "OUNestedPropertiesDefinition.findAll", query = "from OUNestedPropertiesDefinition order by id"),    
    @NamedQuery(name = "OUNestedPropertiesDefinition.findValoriOnCreation", query = "from OUNestedPropertiesDefinition where onCreation=true"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.findSimpleSearch", query = "from OUNestedPropertiesDefinition where simpleSearch=true"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.findAdvancedSearch", query = "from OUNestedPropertiesDefinition where advancedSearch=true"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.uniqueIdByShortName", query = "select id from OUNestedPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.uniqueByShortName", query = "from OUNestedPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.findValoriDaMostrare", query = "from OUNestedPropertiesDefinition where showInList = true"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.findAllWithPolicySingle", query = "from OUNestedPropertiesDefinition where rendering in (from WidgetEPerson)"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.findAllWithPolicyGroup", query = "from OUNestedPropertiesDefinition where rendering in (from WidgetGroup)"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.likeAllWithPolicySingle", query = "from OUNestedPropertiesDefinition where shortName = ? and rendering in (from WidgetEPerson)"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.likeAllWithPolicyGroup", query = "from OUNestedPropertiesDefinition where shortName = ? and rendering in (from WidgetGroup)"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.findAllWithCheckRadioDropdown", query = "from OUNestedPropertiesDefinition where rendering in (from WidgetCheckRadio)"),
    @NamedQuery(name = "OUNestedPropertiesDefinition.likeByShortName", query = "from OUNestedPropertiesDefinition where shortName LIKE :par0", hints = { @javax.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true") })
})
public class OUNestedPropertiesDefinition extends
        ANestedPropertiesDefinition
{

    @Override
    public Class getAnagraficaHolderClass()
    {
        return OUNestedObject.class;
    }

    @Override
    public Class getPropertyHolderClass()
    {
        return OUNestedProperty.class;
    }

    @Override
    public Class getDecoratorClass()
    {       
        return DecoratorOUNestedPropertiesDefinition.class;
    }

}
