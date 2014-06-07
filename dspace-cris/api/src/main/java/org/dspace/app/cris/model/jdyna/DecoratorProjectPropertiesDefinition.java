/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.IContainable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@NamedQueries( {
	@NamedQuery(name = "DecoratorProjectPropertiesDefinition.findAll", query = "from DecoratorProjectPropertiesDefinition order by id"),
	@NamedQuery(name = "DecoratorProjectPropertiesDefinition.uniqueContainableByDecorable", query = "from DecoratorProjectPropertiesDefinition where real.id = ?"),
	@NamedQuery(name = "DecoratorProjectPropertiesDefinition.uniqueContainableByShortName", query = "from DecoratorProjectPropertiesDefinition where real.shortName = ?")
})
@DiscriminatorValue(value="propertiesdefinitionproject")
public class DecoratorProjectPropertiesDefinition extends ADecoratorPropertiesDefinition<ProjectPropertiesDefinition>  {
	
	@OneToOne(optional=true)
	@JoinColumn(name="cris_pj_pdef_fk")
	@Cascade(value = {CascadeType.ALL,CascadeType.DELETE_ORPHAN})
	private ProjectPropertiesDefinition real;
	

	@Override
	public void setReal(ProjectPropertiesDefinition real) {
		this.real = real;
	}
	
	@Override
	public ProjectPropertiesDefinition getObject() {
		return real;
	}

	@Transient
	public Class<ProjectAdditionalFieldStorage> getAnagraficaHolderClass() {
		return real.getAnagraficaHolderClass();
	}

	@Transient
	public Class<ProjectProperty> getPropertyHolderClass() {
		return real.getPropertyHolderClass();
	}

	public Class<DecoratorProjectPropertiesDefinition> getDecoratorClass() {
		return real.getDecoratorClass();
	}
	
	@Transient
	public AWidget getRendering() {
		return this.real.getRendering();
	}

	@Transient
	public String getShortName() {
		return this.real.getShortName();
	}

	@Transient
	public boolean isMandatory() {
		return this.real.isMandatory();
	}

	@Transient
	public String getLabel() {
		return this.real.getLabel();
	}

	@Transient
	public int getPriority() {
		return this.real.getPriority();
	}

	@Transient
	public Integer getAccessLevel() {
		return this.real.getAccessLevel();
	}

	@Override
	public boolean getRepeatable() {
		return this.real.isRepeatable();
	}

	@Override
	public int compareTo(IContainable o) {
		ProjectPropertiesDefinition oo = null;
		if(o instanceof DecoratorProjectPropertiesDefinition) {
			oo = (ProjectPropertiesDefinition)o.getObject();
			return this.real.compareTo(oo);
		}
		return 0;
	}
}
