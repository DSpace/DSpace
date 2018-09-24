/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import it.cilea.osd.common.core.TimeStampInfo;

import java.util.List;
import java.util.Map;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.dspace.app.cris.model.jdyna.OUAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.ProjectAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.ProjectNestedObject;
import org.dspace.app.cris.model.jdyna.ProjectNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectNestedProperty;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectProperty;
import org.dspace.app.cris.model.jdyna.ProjectTypeNestedObject;
import org.dspace.app.cris.model.jdyna.RPAdditionalFieldStorage;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;

@Entity
@Table(name = "cris_project", uniqueConstraints = @UniqueConstraint(columnNames={"sourceID","sourceRef"}))
@NamedQueries({
        @NamedQuery(name = "Project.findAll", query = "from Project order by id"),
        @NamedQuery(name = "Project.count", query = "select count(*) from Project"),
        @NamedQuery(name = "Project.paginate.id.asc", query = "from Project order by id asc"),
        @NamedQuery(name = "Project.paginate.id.desc", query = "from Project order by id desc"),
        @NamedQuery(name = "Project.paginate.status.asc", query = "from Project order by status asc"),
        @NamedQuery(name = "Project.paginate.status.desc", query = "from Project order by status desc"),
        @NamedQuery(name = "Project.paginate.sourceID.asc", query = "from Project order by sourceReference.sourceID asc"),
        @NamedQuery(name = "Project.paginate.sourceID.desc", query = "from Project order by sourceReference.sourceID desc"),
        @NamedQuery(name = "Project.paginate.uuid.asc", query = "from Project order by uuid asc"),
        @NamedQuery(name = "Project.paginate.uuid.desc", query = "from Project order by uuid desc"),
        @NamedQuery(name = "Project.uniqueBySourceID", query = "from Project where sourceReference.sourceRef = ? and sourceReference.sourceID = ?"),
        @NamedQuery(name = "Project.uniqueUUID", query = "from Project where uuid = ?"),
        @NamedQuery(name = "Project.uniqueByCrisID", query = "from Project where crisID = ?"),
		@NamedQuery(name = "Project.uniqueLastModifiedTimeStamp", query = "select timeStampInfo.timestampLastModified.timestamp from Project rp where rp.id = ?"),
        @NamedQuery(name = "Project.uniqueByUUID", query = "from Project where uuid = ?")
  })
public class Project extends ACrisObject<ProjectProperty, ProjectPropertiesDefinition, ProjectNestedProperty, ProjectNestedPropertiesDefinition, ProjectNestedObject, ProjectTypeNestedObject>
        implements                
        Cloneable
{

	private static final String NAME = "title";
	
    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_PROJECT_SEQ")
    @SequenceGenerator(name = "CRIS_PROJECT_SEQ", sequenceName = "CRIS_PROJECT_SEQ", allocationSize = 1)
    private Integer id;

    /** timestamp info for creation and last modify */
    @Embedded
    private TimeStampInfo timeStampInfo;

    /**
     * Map of additional custom data
     */
    @Embedded
    private ProjectAdditionalFieldStorage dynamicField;

    public Project()
    {
        this.dynamicField = new ProjectAdditionalFieldStorage();
        this.dynamicField.setProject(this);
    }

    /**
     * Getter method.
     * 
     * @return the timestamp of creation and last modify of this Project
     */
    public TimeStampInfo getTimeStampInfo()
    {
        if (timeStampInfo == null)
        {
            timeStampInfo = new TimeStampInfo();
        }
        return timeStampInfo;
    }
    
    public Project clone() throws CloneNotSupportedException
    {
    	Project clone = (Project) super.clone();
    	ProjectAdditionalFieldStorage additionalTemp = new ProjectAdditionalFieldStorage();
        additionalTemp.setProject(clone);
        additionalTemp.duplicaAnagrafica(this
                    .getDynamicField());
        clone.setDynamicField(additionalTemp);
        return clone;
    }



    public String getValueTypeIDAttribute()
    {
        return "" + CrisConstants.PROJECT_TYPE_ID;
    }

    
    public ProjectAdditionalFieldStorage getDynamicField()
    {
        return this.dynamicField;
    }

    public void setDynamicField(ProjectAdditionalFieldStorage dynamicField)
    {
        this.dynamicField = dynamicField;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getId()
    {
        return id;
    }

    
    public String getIdentifyingValue()
    {
        return this.dynamicField.getIdentifyingValue();
    }

    
    public String getDisplayValue()
    {
        return this.dynamicField.getDisplayValue();
    }

    
    public List<ProjectProperty> getAnagrafica()
    {
        return this.dynamicField.getAnagrafica();
    }

    
    public Map<String, List<ProjectProperty>> getAnagrafica4view()
    {
        return this.dynamicField.getAnagrafica4view();
    }

    
    public void setAnagrafica(List<ProjectProperty> anagrafica)
    {
        this.dynamicField.setAnagrafica(anagrafica);
    }

    
    public ProjectProperty createProprieta(
            ProjectPropertiesDefinition tipologiaProprieta)
            throws IllegalArgumentException
    {
        return this.dynamicField.createProprieta(tipologiaProprieta);
    }

    
    public ProjectProperty createProprieta(
            ProjectPropertiesDefinition tipologiaProprieta, Integer posizione)
            throws IllegalArgumentException
    {
        return this.dynamicField.createProprieta(tipologiaProprieta, posizione);
    }

    
    public boolean removeProprieta(ProjectProperty proprieta)
    {
        return this.dynamicField.removeProprieta(proprieta);
    }

    
    public List<ProjectProperty> getProprietaDellaTipologia(
            ProjectPropertiesDefinition tipologiaProprieta)
    {
        return this.dynamicField.getProprietaDellaTipologia(tipologiaProprieta);
    }

    
    public Class<ProjectProperty> getClassProperty()
    {
        return this.dynamicField.getClassProperty();
    }

    
    public Class<ProjectPropertiesDefinition> getClassPropertiesDefinition()
    {
        return this.dynamicField.getClassPropertiesDefinition();
    }

    
    public void inizializza()
    {
        this.dynamicField.inizializza();
    }

    
    public void invalidateAnagraficaCache()
    {
        this.dynamicField.invalidateAnagraficaCache();
    }

    
    public void pulisciAnagrafica()
    {
        this.dynamicField.pulisciAnagrafica();
    }


    public String getPublicPath()
    {        
        return "project";
    }

    @Override
    public String getName() {
        for (ProjectProperty title : this.getDynamicField()
                .getAnagrafica4view().get(NAME))
        {
            return title.toString();
        }
        return null;
    }
    
    @Override
    public int getType() {
    	return CrisConstants.PROJECT_TYPE_ID;
    }
 
    @Override
    public String getAuthorityPrefix()
    {
        return "pj";
    }
    

    @Override
    public Class<ProjectNestedObject> getClassNested()
    {
        return ProjectNestedObject.class;
    }

    @Override
    public  Class<ProjectTypeNestedObject> getClassTypeNested()
    {
        return ProjectTypeNestedObject.class;
    }

    public String getTypeText() {
        return CrisConstants.getEntityTypeText(CrisConstants.PROJECT_TYPE_ID);
    }

	@Override
	public boolean isDiscoverable() {
		return true;
	}
	
	@Override
	public String getMetadataFieldTitle() {
		return NAME;
	}

    @Override
    public Class<Project> getCRISTargetClass()
    {
        return Project.class;
    }

    @Override
    public boolean isOwner(EPerson eperson)
    {
    	String ownerProperty = ConfigurationManager.getProperty("cris", getTypeText() + ".owner");
		if (ownerProperty != null) {
			for (String propConf : ownerProperty.split("\\s*,\\s*")) {
	    		List<ProjectProperty> props = getAnagrafica4view().get(propConf);
				for (ProjectProperty p : props) {
	    			ResearcherPage rp = (ResearcherPage) p.getObject();
	    			if (eperson != null && rp.getEpersonID()!= null && eperson.getID() == rp.getEpersonID()) {
	    				return true;
	    			}
				}
			}
    	}
    	return false;
    }
}
