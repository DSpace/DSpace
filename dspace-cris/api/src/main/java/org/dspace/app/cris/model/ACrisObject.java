/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import it.cilea.osd.common.core.TimeStampInfo;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.beans.PropertyEditor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.export.ExportConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.authority.Choices;

@MappedSuperclass
public abstract class ACrisObject<P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>>
        extends DSpaceObject
        implements
        ICrisObject<P, TP>,
        BrowsableDSpaceObject,
        IExportableDynamicObject<TP, P, ACrisObject<P, TP, NP, NTP, ACNO, ATNO>>
{

    @Embedded    
    private SourceReference sourceReference;

    /** Cris public unique identifier, must be null */
    @Column(nullable = true, unique = true)
    private String crisID;

    private Boolean status;

    @Column(nullable = false, unique = true)
    private String uuid;

    public ACrisObject()
    {
        this.status = false;        
    }

    public Boolean getStatus()
    {
        return status;
    }

    public void setStatus(Boolean status)
    {
        this.status = status;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public String getUuid()
    {
        return uuid;
    }

    /**
     * Wrapper method
     * 
     * @param sourceID
     */
    public void setSourceID(String sourceID)
    {
        getSourceReference().setSourceID(sourceID);
    }

    /**
     * Wrappre method 
     * 
     * @return
     */
    public String getSourceID()
    {
        return getSourceReference().getSourceID();
    }

    public abstract String getPublicPath();

    public abstract String getAuthorityPrefix();

    @Override
    public String getHandle()
    {
        return uuid;
    }

    @Override
    public int getID()
    {
        return getId() != null ? getId().intValue() : -1;
    }

    @Override
    public boolean isArchived()
    {
        return getStatus() != null ? getStatus() : false;
    }

    @Override
    public boolean isWithdrawn()
    {
        return getStatus() != null ? !getStatus() : false;
    }

    /**
     * Convenience method to get data from ResearcherPage by a string. For any
     * existent field name the method must return the relative value (i.e
     * getMetadata("fullName") is equivalent to getFullName()) but the method
     * always return a list (with 0, 1 or more elements). For dynamic field it
     * returns the value of the dynamic field with the shorter name equals to
     * the argument. Only public values are returned!
     * 
     * 
     * @param dcField
     *            the field (not null) to retrieve the value
     * @return a list of 0, 1 or more values
     */
    public List<String> getMetadata(String field)
    {
        List<String> result = new ArrayList();

        List<P> dyna = getAnagrafica4view().get(field);
        for (P prop : dyna)
        {
            if (prop.getVisibility() == VisibilityConstants.PUBLIC)
                result.add(prop.toString());
        }

        return result;
    }

    @Override
    public DCValue[] getMetadata(String schema, String element,
            String qualifier, String lang)
    {
        List values = new ArrayList();
        String authority = null;
        if ("crisdo".equals(schema) && "name".equals(element))
        {
            values.add(getName());            
        }
        else if (!schema.equalsIgnoreCase("cris" + this.getPublicPath()))
        {
            return new DCValue[0];
        }
        else
        {
            element = getCompatibleJDynAShortName(this, element);

            List<P> proprieties = this.getAnagrafica4view().get(element);
            
            if (proprieties != null)
            {
                for (P prop : proprieties)
                {
                    Object val = prop.getObject();
                    if (StringUtils.isNotEmpty(qualifier)
                            && val instanceof ACrisObject)
                    {
                        authority = ResearcherPageUtils
                                .getPersistentIdentifier((ACrisObject) val);
                        qualifier = getCompatibleJDynAShortName(
                                (ACrisObject) val, qualifier);
                        List pointProps = (List) ((ACrisObject) val)
                                .getAnagrafica4view().get(qualifier);
                        if (pointProps != null && pointProps.size() > 0)
                        {
                            for (Object pprop : pointProps)
                            {
                                values.add(((Property) pprop).getObject());
                            }
                        }
                    }
                    else if (val instanceof ACrisObject)
                    {
                        authority = ResearcherPageUtils
                                .getPersistentIdentifier((ACrisObject) val);
                        values.add(((ACrisObject) val).getName());
                    }
                    else
                    {
                    	PropertyEditor propertyEditor = prop.getTypo().getRendering()
                                .getPropertyEditor(null);
                        propertyEditor.setValue(val);
                        values.add(propertyEditor.getAsText());
                    }
                }
            }
        }
        DCValue[] result = new DCValue[values.size()];
        for (int idx = 0; idx < values.size(); idx++)
        {
            result[idx] = new DCValue();
            result[idx].schema = schema;
            result[idx].element = element;
            result[idx].qualifier = qualifier;
            result[idx].authority = authority;
            result[idx].confidence = StringUtils.isNotEmpty(authority) ? Choices.CF_ACCEPTED
                    : Choices.CF_UNSET;
            result[idx].value = values.get(idx).toString();
        }
        return result;
    }

    private String getCompatibleJDynAShortName(ACrisObject aCrisObject,
            String element)
    {
        Set<String> keys = aCrisObject.getAnagrafica4view().keySet();
        if (!keys.contains(element))
        {
            // DSpace is case insensitive, metadata are all lowercase
            for (String key : keys)
            {
                if (key.replaceAll("[\\-_]", "").equalsIgnoreCase(element))
                {
                    return key;
                }
            }
        }
        return element;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public String getCrisID()
    {
        return crisID;
    }

    public void setCrisID(String crisID)
    {
        this.crisID = crisID;
    }

    abstract public TimeStampInfo getTimeStampInfo();

    public String getNamePublicIDAttribute()
    {
        return ExportConstants.NAME_PUBLICID_ATTRIBUTE;
    }

    public String getValuePublicIDAttribute()
    {
        return "" + this.getId();
    }

    public String getNameIDAttribute()
    {
        return ExportConstants.NAME_ID_ATTRIBUTE;
    }

    public String getValueIDAttribute()
    {
        if (this.getUuid() == null)
        {
            return "";
        }
        return "" + this.getUuid().toString();
    }

    public String getNameBusinessIDAttribute()
    {
        return ExportConstants.NAME_BUSINESSID_ATTRIBUTE;
    }

    public String getValueBusinessIDAttribute()
    {
        return this.getSourceID();
    }

    public String getNameTypeIDAttribute()
    {
        return ExportConstants.NAME_TYPE_ATTRIBUTE;
    }

    public String getValueTypeIDAttribute()
    {
        return "" + getType();
    }

    public String getNameSingleRowElement()
    {
        return ExportConstants.ELEMENT_SINGLEROW;
    }

    public ACrisObject<P, TP, NP, NTP, ACNO, ATNO> getAnagraficaSupport()
    {
        return this;
    }

    public abstract Class<ACNO> getClassNested();

    public abstract Class<ATNO> getClassTypeNested();

    @Override
    public void update() throws SQLException, AuthorizeException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateLastModified()
    {
        // TODO Auto-generated method stub
        
    }
    
    public String getSourceRef()
    {
        return getSourceReference().getSourceRef();
    }

    public void setSourceRef(String sourceRef)
    {
        getSourceReference().setSourceRef(sourceRef);
    }

    public SourceReference getSourceReference()
    {
        if(this.sourceReference==null) {
            this.sourceReference = new SourceReference();
        }
        return sourceReference;
    }

    public void setSourceReference(SourceReference sourceReference)
    {
        this.sourceReference = sourceReference;
    }

}
