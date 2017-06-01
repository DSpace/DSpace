/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import it.cilea.osd.common.core.TimeStampInfo;
import it.cilea.osd.jdyna.model.AType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.dspace.app.cris.model.jdyna.DynamicAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.DynamicNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicNestedProperty;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicProperty;
import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;
import org.dspace.app.cris.model.jdyna.OUAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.ProjectAdditionalFieldStorage;
import org.dspace.eperson.EPerson;


@Entity
@Table(name = "cris_do", uniqueConstraints = @UniqueConstraint(columnNames={"sourceID","sourceRef"}))
@NamedQueries({
        @NamedQuery(name = "ResearchObject.findAll", query = "from ResearchObject order by id"),
        @NamedQuery(name = "ResearchObject.findByShortNameType", query = "from ResearchObject where typo.shortName = ? order by id"),
        @NamedQuery(name = "ResearchObject.findByIDType", query = "from ResearchObject where typo.id = ? order by id"),
        @NamedQuery(name = "ResearchObject.count", query = "select count(*) from ResearchObject"),
        @NamedQuery(name = "ResearchObject.countByType", query = "select count(*) from ResearchObject where typo = ?"),
        @NamedQuery(name = "ResearchObject.paginate.id.asc", query = "from ResearchObject order by id asc"),
        @NamedQuery(name = "ResearchObject.paginate.id.desc", query = "from ResearchObject order by id desc"),
        @NamedQuery(name = "ResearchObject.paginate.status.asc", query = "from ResearchObject order by status asc"),
        @NamedQuery(name = "ResearchObject.paginate.status.desc", query = "from ResearchObject order by status desc"),
        @NamedQuery(name = "ResearchObject.paginate.sourceID.asc", query = "from ResearchObject order by sourceReference.sourceID asc"),
        @NamedQuery(name = "ResearchObject.paginate.sourceID.desc", query = "from ResearchObject order by sourceReference.sourceID desc"),
        @NamedQuery(name = "ResearchObject.paginate.uuid.asc", query = "from ResearchObject order by uuid asc"),
        @NamedQuery(name = "ResearchObject.paginate.uuid.desc", query = "from ResearchObject order by uuid desc"),
        @NamedQuery(name = "ResearchObject.uniqueBySourceID", query = "from ResearchObject where sourceReference.sourceRef = ? and sourceReference.sourceID = ?"),
        @NamedQuery(name = "ResearchObject.uniqueUUID", query = "from ResearchObject where uuid = ?"),
        @NamedQuery(name = "ResearchObject.uniqueByCrisID", query = "from ResearchObject where crisID = ?"),
        @NamedQuery(name = "ResearchObject.paginateByType.id.asc", query = "from ResearchObject where (typo = :par0) order by id asc"),
        @NamedQuery(name = "ResearchObject.paginateByType.id.desc", query = "from ResearchObject where (typo = :par0) order by id desc"),
		@NamedQuery(name = "ResearchObject.uniqueLastModifiedTimeStamp", query = "select timeStampInfo.timestampLastModified.timestamp from ResearchObject rp where rp.id = ?"),
        @NamedQuery(name = "ResearchObject.uniqueByUUID", query = "from ResearchObject where uuid = ?")
  })
public class ResearchObject extends ACrisObjectWithTypeSupport<DynamicProperty, DynamicPropertiesDefinition, DynamicNestedProperty, DynamicNestedPropertiesDefinition, DynamicNestedObject, DynamicTypeNestedObject>
{
	
	private static final String NAME = "name";
	
    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_DYNAOBJ_SEQ")
    @SequenceGenerator(name = "CRIS_DYNAOBJ_SEQ", sequenceName = "CRIS_DYNAOBJ_SEQ", allocationSize = 1)
    private Integer id;
    
    /** timestamp info for creation and last modify */
    @Embedded
    private TimeStampInfo timeStampInfo;
    
    /**
     * Map of additional custom data
     */
    @Embedded
    private DynamicAdditionalFieldStorage dynamicField;

    @ManyToOne
    private DynamicObjectType typo; 
    
    public ResearchObject()
    {
        this.dynamicField = new DynamicAdditionalFieldStorage();
        dynamicField.setDynamicObject(this);
    }
    
    public void setDynamicField(DynamicAdditionalFieldStorage dynamicField)
    {
        this.dynamicField = dynamicField;
    }

    public DynamicAdditionalFieldStorage getDynamicField()
    {
        if (this.dynamicField == null)
        {
            this.dynamicField = new DynamicAdditionalFieldStorage();
        }
        return dynamicField;
    }
    
    @Override
    public List<DynamicProperty> getAnagrafica()
    {        
        return dynamicField.getAnagrafica();
    }

    @Override
    public Class<DynamicProperty> getClassProperty()
    {
        return this.dynamicField.getClassProperty();
    }

    @Override
    public Class<DynamicPropertiesDefinition> getClassPropertiesDefinition()
    {
        return this.dynamicField.getClassPropertiesDefinition();
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public DynamicObjectType getTypo()
    {        
        return typo;
    }

    @Override
    public void setTypo(AType<DynamicPropertiesDefinition> typo)
    {
        this.typo = (DynamicObjectType)typo;        
    }

    @Override
    public Map<String, List<DynamicProperty>> getAnagrafica4view()
    {
        return this.dynamicField.getAnagrafica4view();
    }

    @Override
    public void setAnagrafica(List<DynamicProperty> anagrafica)
    {
        this.dynamicField.setAnagrafica(anagrafica);
    }

    @Override
    public DynamicProperty createProprieta(
            DynamicPropertiesDefinition tipologiaProprieta)
            throws IllegalArgumentException
    {
        return this.dynamicField.createProprieta(tipologiaProprieta);
    }

    @Override
    public DynamicProperty createProprieta(
            DynamicPropertiesDefinition tipologiaProprieta, Integer posizione)
            throws IllegalArgumentException
    {
        return this.dynamicField.createProprieta(tipologiaProprieta, posizione);
    }

    @Override
    public boolean removeProprieta(DynamicProperty proprieta)
    {
        return this.dynamicField.removeProprieta(proprieta);
    }

    @Override
    public List<DynamicProperty> getProprietaDellaTipologia(
            DynamicPropertiesDefinition tipologiaProprieta)
    {
        return this.dynamicField.getProprietaDellaTipologia(tipologiaProprieta);
    }

    @Override
    public void inizializza()
    {
        this.dynamicField.inizializza();
        
    }

    @Override
    public void invalidateAnagraficaCache()
    {
        this.dynamicField.invalidateAnagraficaCache();
        
    }

    @Override
    public void pulisciAnagrafica()
    {
        this.dynamicField.pulisciAnagrafica();
    }

    @Override
    public String getIdentifyingValue()
    {
        return this.dynamicField.getIdentifyingValue();
    }

    @Override
    public String getDisplayValue()
    {
        return this.dynamicField.getDisplayValue();
    }

    @Override
    public String getPublicPath()
    {
        return CrisConstants.getAuthorityPrefix(this);
    }

    @Override
    public String getAuthorityPrefix()
    {
        return typo.getShortName();
    }

    @Override
    public TimeStampInfo getTimeStampInfo()
    {
        if (timeStampInfo == null)
        {
            timeStampInfo = new TimeStampInfo();
        }
        return timeStampInfo;
    }

    @Override
    public Class<DynamicNestedObject> getClassNested()
    {
        return DynamicNestedObject.class;
    }

    @Override
    public Class<DynamicTypeNestedObject> getClassTypeNested()
    {
        return DynamicTypeNestedObject.class;
    }

    @Override
    public int getType()
    {
        return getTypo().getId() + CrisConstants.CRIS_DYNAMIC_TYPE_ID_START;
    }

    @Override
    public String getName()
    {
        for (DynamicProperty title : this.getAnagrafica4view().get(getTypo().getShortName() + NAME))
        {
            return title.toString();
        }
        return null;
    }

    @Override
    public String getTypeText()
    {
        return CrisConstants.getEntityTypeText(getType());
    }

    public void setId(Integer id)
    {
        this.id = id;
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
	public ResearchObject clone()
			throws CloneNotSupportedException {
        ResearchObject clone = (ResearchObject) super.clone();
        DynamicAdditionalFieldStorage additionalTemp = new DynamicAdditionalFieldStorage();
        additionalTemp.setDynamicObject(clone);
        additionalTemp.duplicaAnagrafica(this
                    .getDynamicField());
        clone.setDynamicField(additionalTemp);
        return clone;
	}
	
    @Override
    public Class<ResearchObject> getCRISTargetClass()
    {
        return ResearchObject.class;
    }

    @Override
    public boolean isOwner(EPerson eperson)
    {
        // TODO not implemented
        return false;
    }

    
    public String getMetadataFieldName(Locale locale) {
        return getAuthorityPrefix()+ getMetadataFieldTitle() + locale.getLanguage();
    }
}
