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

@Entity
@Table(name="cris_do_pdef")
@NamedQueries( {
    @NamedQuery(name = "DynamicPropertiesDefinition.findAll", query = "from DynamicPropertiesDefinition order by id"),    
    @NamedQuery(name = "DynamicPropertiesDefinition.findValoriOnCreation", query = "from DynamicPropertiesDefinition where onCreation=true"),
    @NamedQuery(name = "DynamicPropertiesDefinition.findSimpleSearch", query = "from DynamicPropertiesDefinition where simpleSearch=true"),
    @NamedQuery(name = "DynamicPropertiesDefinition.findAdvancedSearch", query = "from DynamicPropertiesDefinition where advancedSearch=true"),
    @NamedQuery(name = "DynamicPropertiesDefinition.uniqueIdByShortName", query = "select id from DynamicPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "DynamicPropertiesDefinition.uniqueByShortName", query = "from DynamicPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "DynamicPropertiesDefinition.findValoriDaMostrare", query = "from DynamicPropertiesDefinition where showInList = true")    
})
public class DynamicPropertiesDefinition extends PropertiesDefinition
{
    
    @Override
    public Class<DynamicAdditionalFieldStorage> getAnagraficaHolderClass()
    {
        return DynamicAdditionalFieldStorage.class;
    }

    @Override
    public Class<DynamicProperty> getPropertyHolderClass()
    {
        return DynamicProperty.class;
    }

    @Override
    public Class<DecoratorDynamicPropertiesDefinition> getDecoratorClass()
    {
        return DecoratorDynamicPropertiesDefinition.class;
    }

   

}
