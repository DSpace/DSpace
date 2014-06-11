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
@Table(name="cris_pj_no_pdef")
@NamedQueries( {
    @NamedQuery(name = "ProjectNestedPropertiesDefinition.findAll", query = "from ProjectNestedPropertiesDefinition order by id"),    
    @NamedQuery(name = "ProjectNestedPropertiesDefinition.findValoriOnCreation", query = "from ProjectNestedPropertiesDefinition where onCreation=true"),
    @NamedQuery(name = "ProjectNestedPropertiesDefinition.findSimpleSearch", query = "from ProjectNestedPropertiesDefinition where simpleSearch=true"),
    @NamedQuery(name = "ProjectNestedPropertiesDefinition.findAdvancedSearch", query = "from ProjectNestedPropertiesDefinition where advancedSearch=true"),
    @NamedQuery(name = "ProjectNestedPropertiesDefinition.uniqueIdByShortName", query = "select id from ProjectNestedPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "ProjectNestedPropertiesDefinition.uniqueByShortName", query = "from ProjectNestedPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "ProjectNestedPropertiesDefinition.findValoriDaMostrare", query = "from ProjectNestedPropertiesDefinition where showInList = true")
})
public class ProjectNestedPropertiesDefinition extends
        ANestedPropertiesDefinition
{

    @Override
    public Class getAnagraficaHolderClass()
    {
        return ProjectNestedObject.class;
    }

    @Override
    public Class getPropertyHolderClass()
    {
        return ProjectNestedProperty.class;
    }

    @Override
    public Class getDecoratorClass()
    {       
        return DecoratorProjectNestedPropertiesDefinition.class;
    }

}
