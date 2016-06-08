/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;

/**
 * Database access class representing a Dublin Core metadata value.
 * It represents a value of a given <code>MetadataField</code> on an Item.
 * (The Item can have many values of the same field.)  It contains element, qualifier, value and language.
 * the field (which names the schema, element, and qualifier), language,
 * and a value.
 *
 * @author Martin Hald
 * @see org.dspace.content.MetadataSchema
 * @see org.dspace.content.MetadataField
 */
@Entity
@Table(name="metadatavalue")
public class MetadataValue implements ReloadableEntity<Integer>
{
    /** The reference to the metadata field */
    @Id
    @Column(name="metadata_value_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="metadatavalue_seq")
    @SequenceGenerator(name="metadatavalue_seq", sequenceName="metadatavalue_seq", allocationSize = 1)
    private Integer id;

    /** The primary key for the metadata value */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "metadata_field_id")
    private MetadataField metadataField = null;

    /** The value of the field */
    @Lob
    @Type(type="org.hibernate.type.MaterializedClobType")
    @Column(name="text_value")
    private String value;

    /** The language of the field, may be <code>null</code> */
    @Column(name = "text_lang", length = 24)
    private String language;

    /** The position of the record. */
    @Column(name = "place")
    private int place = 1;

    /** Authority key, if any */
    @Column(name = "authority", length = 100)
    private String authority = null;

    /** Authority confidence value -- see Choices class for values */
    @Column(name = "confidence")
    private int confidence = -1;

    @ManyToOne(fetch = FetchType.LAZY, cascade={CascadeType.PERSIST})
    @JoinColumn(name="dspace_object_id")
    protected DSpaceObject dSpaceObject;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.MetadataValueService#create(Context, DSpaceObject, MetadataField)}
     *
     */
    protected MetadataValue()
    {
        id = 0;
    }

    /**
     * Get the field ID the metadata value represents.
     *
     * @return metadata value ID
     */
    public Integer getID()
    {
        return id;
    }

    /**
     * Get the dspaceObject
     *
     * @return dspaceObject
     */
    public DSpaceObject getDSpaceObject()
    {
        return dSpaceObject;
    }

    /**
     * Set the dspaceObject ID.
     *
     * @param dso new dspaceObject ID
     */
    public void setDSpaceObject(DSpaceObject dso)
    {
        this.dSpaceObject = dso;
    }

    /**
     * Get the language (e.g. "en").
     *
     * @return language
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * Set the language (e.g. "en").
     *
     * @param language new language
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * Get the place ordering.
     *
     * @return place ordering
     */
    public int getPlace()
    {
        return place;
    }

    /**
     * Set the place ordering.
     *
     * @param place new place (relative order in series of values)
     */
    public void setPlace(int place)
    {
        this.place = place;
    }

    public MetadataField getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    /**
     * Get the metadata value.
     *
     * @return metadata value
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Set the metadata value
     *
     * @param value new metadata value
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Get the metadata authority
     *
     * @return metadata authority
     */
    public String getAuthority ()
    {
        return authority ;
    }

    /**
     * Set the metadata authority
     *
     * @param value new metadata authority
     */
    public void setAuthority (String value)
    {
        this.authority  = value;
    }

    /**
     * Get the metadata confidence
     *
     * @return metadata confidence
     */
    public int getConfidence()
    {
        return confidence;
    }

    /**
     * Set the metadata confidence
     *
     * @param value new metadata confidence
     */
    public void setConfidence(int value)
    {
        this.confidence = value;
    }


    /**
     * Return <code>true</code> if <code>other</code> is the same MetadataValue
     * as this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         MetadataValue as this object
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        if (getClass() != objClass)
        {
            return false;
        }
        final MetadataValue other = (MetadataValue) obj;
        if (this.id != other.id)
        {
            return false;
        }
        if (this.getID() != other.getID())
        {
            return false;
        }
        if (this.getDSpaceObject().getID() != other.getDSpaceObject().getID())
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 47 * hash + this.id;
        hash = 47 * hash + this.getID();
        hash = 47 * hash + this.getDSpaceObject().getID().hashCode();
        return hash;
    }


}