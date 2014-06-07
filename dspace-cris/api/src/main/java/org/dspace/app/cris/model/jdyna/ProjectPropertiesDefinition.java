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
@Table(name="cris_pj_pdef")
@NamedQueries( {
    @NamedQuery(name = "ProjectPropertiesDefinition.findAll", query = "from ProjectPropertiesDefinition order by id"),    
    @NamedQuery(name = "ProjectPropertiesDefinition.findValoriOnCreation", query = "from ProjectPropertiesDefinition where onCreation=true"),
    @NamedQuery(name = "ProjectPropertiesDefinition.findSimpleSearch", query = "from ProjectPropertiesDefinition where simpleSearch=true"),
    @NamedQuery(name = "ProjectPropertiesDefinition.findAdvancedSearch", query = "from ProjectPropertiesDefinition where advancedSearch=true"),
    @NamedQuery(name = "ProjectPropertiesDefinition.uniqueIdByShortName", query = "select id from ProjectPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "ProjectPropertiesDefinition.uniqueByShortName", query = "from ProjectPropertiesDefinition where shortName = ?"),
    @NamedQuery(name = "ProjectPropertiesDefinition.findValoriDaMostrare", query = "from ProjectPropertiesDefinition where showInList = true")    
    
})
public class ProjectPropertiesDefinition extends PropertiesDefinition {
		
	@Transient
	public Class<ProjectAdditionalFieldStorage> getAnagraficaHolderClass() {
		return ProjectAdditionalFieldStorage.class;
	}

	@Transient
	public Class<ProjectProperty> getPropertyHolderClass() {
		return ProjectProperty.class;
	}
	
	@Override
	public Class<DecoratorProjectPropertiesDefinition> getDecoratorClass() {
		return DecoratorProjectPropertiesDefinition.class;
	}

}
