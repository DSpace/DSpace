/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import it.cilea.osd.common.core.SingleTimeStampInfo;
import it.cilea.osd.common.core.TimeStampInfo;
import it.cilea.osd.jdyna.value.FileValue;

import java.beans.PropertyEditor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.export.ExportConstants;
import org.dspace.app.cris.model.jdyna.RPAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.RPTypeNestedObject;
import org.dspace.app.cris.model.jdyna.value.OUPointer;
import org.dspace.app.cris.model.listener.RPListener;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * This class models the HKU Researcher Page concept. Almost all the field of
 * this class are {@link RestrictedField} or List of it, because the visibility
 * of its values can be turned on/off. A ResearcherPage maintains also a list of
 * related {@link ItemOutsideHub}
 * 
 * @author cilea
 * 
 */
@Entity
@Table(name = "cris_rpage", uniqueConstraints = @UniqueConstraint(columnNames={"sourceID","sourceRef"}))
@NamedQueries({
        @NamedQuery(name = "ResearcherPage.findAll", query = "from ResearcherPage order by id"),
        @NamedQuery(name = "ResearcherPage.paginate.id.asc", query = "from ResearcherPage order by id asc"),
        @NamedQuery(name = "ResearcherPage.paginate.id.desc", query = "from ResearcherPage order by id desc"),
        @NamedQuery(name = "ResearcherPage.paginate.status.asc", query = "from ResearcherPage order by status asc"),
        @NamedQuery(name = "ResearcherPage.paginate.status.desc", query = "from ResearcherPage order by status desc"),
        @NamedQuery(name = "ResearcherPage.paginate.sourceID.asc", query = "from ResearcherPage order by sourceReference.sourceID asc"),
        @NamedQuery(name = "ResearcherPage.paginate.sourceID.desc", query = "from ResearcherPage order by sourceReference.sourceID desc"),
        @NamedQuery(name = "ResearcherPage.paginate.uuid.asc", query = "from ResearcherPage order by uuid asc"),
        @NamedQuery(name = "ResearcherPage.paginate.uuid.desc", query = "from ResearcherPage order by uuid desc"),
        @NamedQuery(name = "ResearcherPage.count", query = "select count(*) from ResearcherPage"),
        @NamedQuery(name = "ResearcherPage.findAllResearcherPageByStatus", query = "from ResearcherPage where status = ? order by id"),
        // @NamedQuery(name = "ResearcherPage.findAllResearcherByField", query =
        // "select rp from ResearcherPage rp where :par0 in indices(rp.additionalFields)"),
        @NamedQuery(name = "ResearcherPage.findAllResearcherByName", query = "select distinct rp from ResearcherPage rp join rp.dynamicField.anagrafica vv where ((vv.typo.shortName = 'variants' or vv.typo.shortName = 'preferredName' or vv.typo.shortName = 'fullName' or vv.typo.shortName = 'translatedName') and vv.value = :par0)"),
        @NamedQuery(name = "ResearcherPage.countAllResearcherByName", query = "select count(*) from ResearcherPage rp join rp.dynamicField.anagrafica vv where ((vv.typo.shortName = 'variants' or vv.typo.shortName = 'preferredName' or vv.typo.shortName = 'fullName' or vv.typo.shortName = 'translatedName') and vv.value = :par0)"),
        @NamedQuery(name = "ResearcherPage.countAllResearcherByNameExceptResearcher", query = "select count(*) from ResearcherPage rp join rp.dynamicField.anagrafica vv where ((vv.typo.shortName = 'variants' or vv.typo.shortName = 'preferredName' or vv.typo.shortName = 'fullName' or vv.typo.shortName = 'translatedName') and vv.value = :par0) and rp.id != :par1 "),
        @NamedQuery(name = "ResearcherPage.findAllResearcherByNamesTimestampLastModified", query = "from ResearcherPage where namesModifiedTimeStamp.timestamp >= ?"),
        @NamedQuery(name = "ResearcherPage.uniqueBySourceID", query = "from ResearcherPage rp where rp.sourceReference.sourceRef = ? and rp.sourceReference.sourceID = ?"),
        @NamedQuery(name = "ResearcherPage.findAllResearcherInDateRange", query = "from ResearcherPage rp where rp.timeStampInfo.timestampCreated.timestamp between :par0 and :par1"),
        @NamedQuery(name = "ResearcherPage.findAllResearcherByCreationDateBefore", query = "from ResearcherPage rp where rp.timeStampInfo.timestampCreated.timestamp <= ?"),
        @NamedQuery(name = "ResearcherPage.findAllResearcherByCreationDateAfter", query = "from ResearcherPage rp where rp.timeStampInfo.timestampCreated.timestamp >= ?"),
        @NamedQuery(name = "ResearcherPage.findAllNextResearcherByIDStart", query = "from ResearcherPage rp where rp.id >= ?"),
        @NamedQuery(name = "ResearcherPage.findAllPrevResearcherByIDEnd", query = "from ResearcherPage rp where rp.id <= ?"),
        @NamedQuery(name = "ResearcherPage.findAllResearcherInIDRange", query = "from ResearcherPage rp where rp.id between :par0 and :par1"),
        @NamedQuery(name = "ResearcherPage.findAllNextResearcherBysourceIDStart", query = "from ResearcherPage rp where rp.sourceReference.sourceID >= ?"),
        @NamedQuery(name = "ResearcherPage.findAllPrevResearcherBysourceIDEnd", query = "from ResearcherPage rp where rp.sourceReference.sourceID <= ?"),
        @NamedQuery(name = "ResearcherPage.findAllResearcherInsourceIDRange", query = "from ResearcherPage rp where rp.sourceReference.sourceID between :par0 and :par1"),
        @NamedQuery(name = "ResearcherPage.uniqueLastModifiedTimeStamp", query = "select timeStampInfo.timestampLastModified.timestamp from ResearcherPage rp where rp.id = ?"),
        @NamedQuery(name = "ResearcherPage.findAnagraficaByRPID", query = "select dynamicField.anagrafica from ResearcherPage rp where rp.id = ?"),
        @NamedQuery(name = "ResearcherPage.findAllResearcherPageID", query = "select id from ResearcherPage order by id"),
        @NamedQuery(name = "ResearcherPage.uniqueUUID", query = "from ResearcherPage where uuid = ?"),
        @NamedQuery(name = "ResearcherPage.uniqueByCrisID", query = "from ResearcherPage where crisID = ?"),
        @NamedQuery(name = "ResearcherPage.idFindMax", query = "select max(id) from ResearcherPage"),
        @NamedQuery(name = "ResearcherPage.uniqueByEPersonId", query = "from ResearcherPage where epersonID = ?"),
        @NamedQuery(name = "ResearcherPage.uniqueByUUID", query = "from ResearcherPage where uuid = ?") })
public class ResearcherPage extends
        ACrisObject<RPProperty, RPPropertiesDefinition, RPNestedProperty, RPNestedPropertiesDefinition, RPNestedObject, RPTypeNestedObject>
{

    private static final String NAME = "fullName";

	@Column(unique = true, nullable = true)
    private Integer epersonID;

    /** log4j logger */
    @Transient
    private static Logger log = Logger.getLogger(ResearcherPage.class);

    /** timestamp info for creation and last modify */
    @Embedded
    private TimeStampInfo timeStampInfo;

    @Embedded
    @AttributeOverride(name = "timestamp", column = @Column(name = "namesTimestampLastModified"))
    private SingleTimeStampInfo namesModifiedTimeStamp;

    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_RPAGE_SEQ")
    @SequenceGenerator(name = "CRIS_RPAGE_SEQ", sequenceName = "CRIS_RPAGE_SEQ", allocationSize = 1)
    private Integer id;

    @Transient
    /**
     * The names that the ResearcherPage has when loaded from the db the first
     * time. It is useful for comparison with the current names to see if
     * changes has been made.
     * 
     */
    private String oldNames;

    /**
     * Map of additional custom data
     */
    @Embedded
    private RPAdditionalFieldStorage dynamicField;

    @Transient
    private boolean internalRP = true;

    @Transient
    private String fullName;

    @Transient
    private Integer oldEpersonID;

    @Transient
    private String oldOrcidPublicationsPreference;
    @Transient
    private String oldOrcidProjectsPreference;
    @Transient
    private List<String> oldOrcidProfilePreference;
    @Transient
    private Map<String,List<String>> oldMapOrcidProfilePreference;
    
    /**
     * Constructor method, create new ResearcherPage setting status to true.
     */
    public ResearcherPage()
    {
        this.dynamicField = new RPAdditionalFieldStorage();
        this.dynamicField.setResearcherPage(this);
    }

    /**
     * Getter method.
     * 
     * @return the db primary key
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * Setter method.
     * 
     * @param id
     *            the db primary key
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    /**
     * Wrapper method.
     * 
     * @return the fullName
     */
    public String getFullName()
    {
        for (RPProperty property : this.getDynamicField().getAnagrafica4view()
                .get(NAME))
        {
            return property.getValue().getObject().toString();
        }
        return null;
    }

    /**
     * Getter method.
     * 
     * @return the academic name
     */
    public RestrictedField getPreferredName()
    {
        RestrictedField result = new RestrictedField();
        for (RPProperty property : this.getDynamicField().getAnagrafica4view()
                .get("preferredName"))
        {
            result.setValue(property.getValue().getObject().toString());
            result.setVisibility(property.getVisibility());
            break;
        }
        return result;
    }

    /**
     * Wrapper method.
     * 
     * @return the chinese name
     */
    public RestrictedField getTranslatedName()
    {
        RestrictedField result = new RestrictedField();
        for (RPProperty property : this.getDynamicField().getAnagrafica4view()
                .get("translatedName"))
        {
            result.setValue(property.getValue().getObject().toString());
            result.setVisibility(property.getVisibility());
            break;
        }
        return result;
    }

    /**
     * Wrapper method.
     * 
     * @return the variants form of the name (include also Japanese, Korean,
     *         etc.)
     */
    public List<RestrictedField> getVariants()
    {
        List<RestrictedField> results = new ArrayList<RestrictedField>();

        for (RPProperty property : this.getDynamicField().getAnagrafica4view()
                .get("variants"))
        {
            RestrictedField result = new RestrictedField();
            result.setValue(property.getValue().getObject().toString());
            result.setVisibility(property.getVisibility());
            results.add(result);
        }
        return results;
    }

    /**
     * Wrapper method
     * 
     * @return
     */
    public RestrictedField getEmail()
    {
        RestrictedField result = new RestrictedField();
        for (RPProperty property : this.getDynamicField().getAnagrafica4view()
                .get("email"))
        {
            result.setValue(property.getValue().getObject().toString());
            result.setVisibility(property.getVisibility());
            break;
        }
        return result;

    }

    /**
     * Getter method.
     * 
     * @return the timestamp of creation and last modify of this ResearcherPage
     */
    public TimeStampInfo getTimeStampInfo()
    {
        if (timeStampInfo == null)
        {
            timeStampInfo = new TimeStampInfo();
        }
        return timeStampInfo;
    }

    /**
     * Getter method.
     * 
     * @return the timestamp of last modify to this Researcher Page's names
     */
    public SingleTimeStampInfo getNamesModifiedTimeStamp()
    {
        return namesModifiedTimeStamp;
    }

    /**
     * Getter method.
     * 
     * @see RPListener
     * @param oldNames
     *            a string containing all the names as initially set on the
     *            first time access. Useful for detect changes to a name:
     *            addition, deletion, visibility change
     */
    public String getOldNames()
    {
        return oldNames;
    }

    /**
     * Setter method.
     * 
     * @param namesModifiedTimeStamp
     *            the timestamp of last modified to researcher names
     */
    public void setNamesModifiedTimeStamp(
            SingleTimeStampInfo namesModifiedTimeStamp)
    {
        this.namesModifiedTimeStamp = namesModifiedTimeStamp;
    }

    /**
     * Setter method.
     * 
     * @see RPListener
     * @param oldNames
     *            a string containing all the names as initially set on the
     *            first time access. Useful for detect changes to a name:
     *            addition, deletion, visibility change
     */
    public void setOldNames(String oldNames)
    {
        this.oldNames = oldNames;
    }

    public void setDynamicField(RPAdditionalFieldStorage dynamicField)
    {
        this.dynamicField = dynamicField;
    }

    public RPAdditionalFieldStorage getDynamicField()
    {
        if (this.dynamicField == null)
        {
            this.dynamicField = new RPAdditionalFieldStorage();
        }
        return dynamicField;
    }

    @Transient
    public List<String> getAllPublicNames()
    {
        List<String> results = new ArrayList<String>();
        results.add(getFullName());
        if (getPreferredName().getValue() != null
                && !getPreferredName().getValue().isEmpty())
        {
            results.add(getPreferredName().getValue());
        }
        if (getTranslatedName().getValue() != null
                && !getTranslatedName().getValue().isEmpty())
        {
            results.add(getTranslatedName().getValue());
        }
        for (RestrictedField rf : getVariants())
        {
            if (rf.getVisibility() == VisibilityConstants.PUBLIC
                    && rf.getValue() != null)
            {
                results.add(rf.getValue());
            }
        }
        return results;
    }

    public List<String> getAllNames()
    {
        List<String> results = new ArrayList<String>();
        results.add(getFullName());
        if (getPreferredName().getValue() != null
                && !getPreferredName().getValue().isEmpty())
        {
            results.add(getPreferredName().getValue());
        }
        if (getTranslatedName().getValue() != null
                && !getTranslatedName().getValue().isEmpty())
        {
            results.add(getTranslatedName().getValue());
        }
        for (RestrictedField rf : getVariants())
        {
            if (rf.getValue() != null)
            {
                results.add(rf.getValue());
            }
        }
        return results;
    }

    public ResearcherPage clone() throws CloneNotSupportedException
    {
        ResearcherPage clone = (ResearcherPage) super.clone();
        RPAdditionalFieldStorage additionalTemp = new RPAdditionalFieldStorage();
        additionalTemp.setResearcherPage(clone);
        additionalTemp.duplicaAnagrafica(this
                    .getDynamicField());
        clone.setDynamicField(additionalTemp);
        return clone;
    }

    public void setInternalRP(boolean internalRP)
    {
        this.internalRP = internalRP;
    }

    public boolean isInternalRP()
    {
        return internalRP;
    }

    public String getNamePublicIDAttribute()
    {
        return ExportConstants.NAME_PUBLICID_ATTRIBUTE;
    }

    public String getIdentifyingValue()
    {
        return this.dynamicField.getIdentifyingValue();
    }

    public String getDisplayValue()
    {
        return this.dynamicField.getDisplayValue();
    }

    public List<RPProperty> getAnagrafica()
    {
        return this.dynamicField.getAnagrafica();
    }

    public Map<String, List<RPProperty>> getAnagrafica4view()
    {
        return this.dynamicField.getAnagrafica4view();
    }

    public void setAnagrafica(List<RPProperty> anagrafica)
    {
        this.dynamicField.setAnagrafica(anagrafica);
    }

    public RPProperty createProprieta(RPPropertiesDefinition tipologiaProprieta)
            throws IllegalArgumentException
    {
        return this.dynamicField.createProprieta(tipologiaProprieta);
    }

    public RPProperty createProprieta(
            RPPropertiesDefinition tipologiaProprieta, Integer posizione)
            throws IllegalArgumentException
    {
        return this.dynamicField.createProprieta(tipologiaProprieta, posizione);
    }

    public boolean removeProprieta(RPProperty proprieta)
    {
        return this.dynamicField.removeProprieta(proprieta);
    }

    public List<RPProperty> getProprietaDellaTipologia(
            RPPropertiesDefinition tipologiaProprieta)
    {
        return this.dynamicField.getProprietaDellaTipologia(tipologiaProprieta);
    }

    public Class<RPProperty> getClassProperty()
    {
        return this.dynamicField.getClassProperty();
    }

    public Class<RPPropertiesDefinition> getClassPropertiesDefinition()
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

    public EPerson getDspaceUser()
    {
        Context context = null;
        EPerson eperson = null;

        try
        {
            context = new Context();
            if (epersonID != null)
            {
                eperson = EPerson.find(context, epersonID);
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage());
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

        return eperson;
    }

    public Integer getEpersonID()
    {
        return epersonID;
    }

    public void setEpersonID(Integer idEPerson)
    {
        this.epersonID = idEPerson;
    }

    public String getPublicPath()
    {
        return "rp";
    }

    @Override
    public int getType()
    {
        return CrisConstants.RP_TYPE_ID;
    }

    @Override
    public String getName()
    {
        return getFullName();
    }

    @Override
    public String getAuthorityPrefix()
    {
        return "rp";
    }

    /**
     * Wrapper method
     * 
     * @return value list
     */
    public List<RestrictedFieldWithLock> getOrgUnit()
    {
        List<RestrictedFieldWithLock> results = new ArrayList<RestrictedFieldWithLock>();
        String pdef_dept = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "researcherpage.pdef.orgunit");
        for (RPProperty property : this.getDynamicField().getAnagrafica4view()
                .get(pdef_dept))
        {

            OUPointer pointer = (OUPointer) property.getValue();
            RestrictedFieldWithLock result = new RestrictedFieldWithLock();
            result.setValue(pointer.getObject().getName());
            result.setVisibility(property.getVisibility());
            result.setLock(property.getLockDef());
            result.setAuthority(pointer.getObject().getCrisID());
            results.add(result);
        }
        return results;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    @Override
    public Class<RPNestedObject> getClassNested()
    {
        return RPNestedObject.class;
    }

    @Override
    public Class<RPTypeNestedObject> getClassTypeNested()
    {
        return RPTypeNestedObject.class;
    }

    public void setOldEpersonID(Integer oldEpersonID)
    {
        this.oldEpersonID = oldEpersonID;
    }

    public Integer getOldEpersonID()
    {
        return oldEpersonID;
    }

    public String getTypeText()
    {
        return CrisConstants.getEntityTypeText(CrisConstants.RP_TYPE_ID);
    }

    public RestrictedFieldFile getPict()
    {
        RestrictedFieldLocalOrRemoteFile result = new RestrictedFieldLocalOrRemoteFile();
        String pdef_image = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "researcherpage.pdef.publicimage");
        for (RPProperty property : this.getDynamicField().getAnagrafica4view()
                .get(pdef_image))
        {            
            FileValue value = (FileValue) property.getValue();
            PropertyEditor propertyEditor = property.getTypo().getRendering()
                    .getPropertyEditor(null);
            propertyEditor.setValue(value.getObject());
            result.setValue(propertyEditor.getAsText());
            result.setVisibility(property.getVisibility());
            break;
        }
        if (result.getValue() == null || result.getValue().isEmpty())
        {
            for (RPProperty property : this.getDynamicField()
                    .getAnagrafica4view().get(pdef_image+"_ext"))
            {
                result.setRemoteUrl(property.getValue().getObject().toString());
                result.setValue(property.getValue().getObject().toString());
                result.setVisibility(property.getVisibility());
                break;
            }
        }
        return result;
    }

	@Override
	public boolean isDiscoverable() {
		return true;
	}
	
	@Override
	public String getMetadataFieldTitle() {
		return NAME;
	}

	public String getOldOrcidPublicationsPreference() {
		return oldOrcidPublicationsPreference;
	}

	public void setOldOrcidPublicationsPreference(String oldOrcidPublicationsPreference) {
		this.oldOrcidPublicationsPreference = oldOrcidPublicationsPreference;
	}

	public String getOldOrcidProjectsPreference() {
		return oldOrcidProjectsPreference;
	}

	public void setOldOrcidProjectsPreference(String oldOrcidProjectsPreference) {
		this.oldOrcidProjectsPreference = oldOrcidProjectsPreference;
	}

	public List<String> getOldOrcidProfilePreference() {
		if(this.oldOrcidProfilePreference == null) {
			this.oldOrcidProfilePreference = new ArrayList<String>();
		}
		return oldOrcidProfilePreference;
	}

	public void setOldOrcidProfilePreference(List<String> oldOrcidProfilePreference) {
		this.oldOrcidProfilePreference = oldOrcidProfilePreference;
	}

	public Map<String, List<String>> getOldMapOrcidProfilePreference() {
		if(oldMapOrcidProfilePreference == null) {
			oldMapOrcidProfilePreference = new HashMap<String, List<String>>();
		}
		return oldMapOrcidProfilePreference;
	}

	public void setOldMapOrcidProfilePreference(Map<String, List<String>> oldMapOrcidProfilePreference) {
		this.oldMapOrcidProfilePreference = oldMapOrcidProfilePreference;
	}

    @Override
    public Class<ResearcherPage> getCRISTargetClass()
    {
        return ResearcherPage.class;
    }

    @Override
    public boolean isOwner(EPerson eperson)
    {
        return eperson != null && this.getEpersonID()!=null && (this.getEpersonID() == eperson.getID());
    }
}
