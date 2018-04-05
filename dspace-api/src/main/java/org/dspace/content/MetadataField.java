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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;


/**
 * DSpace object that represents a metadata field, which is
 * defined by a combination of schema, element, and qualifier.  Every
 * metadata element belongs in a field.
 *
 * @author Martin Hald
 * @version $Revision$
 * @see org.dspace.content.MetadataValue
 * @see org.dspace.content.MetadataSchema
 */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name="metadatafieldregistry")
public class MetadataField implements ReloadableEntity<Integer> {

    @Id
    @Column(name="metadata_field_id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="metadatafieldregistry_seq")
    @SequenceGenerator(name="metadatafieldregistry_seq", sequenceName="metadatafieldregistry_seq", allocationSize = 1, initialValue = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "metadata_schema_id",nullable = false)
    private MetadataSchema metadataSchema;

    @Column(name = "element", length = 64)
    private String element;

    @Column(name = "qualifier", length = 64)
    private String qualifier = null;

//    @Column(name = "scope_note")
//    @Lob
    @Column(name="scope_note", columnDefinition = "text")
    private String scopeNote;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.MetadataFieldService#create(Context, MetadataSchema, String, String, String)}
     *
     */
    protected MetadataField()
    {

    }

    /**
     * Get the metadata field id.
     *
     * @return metadata field id
     */
    public Integer getID()
    {
        return id;
    }

    /**
     * Get the element name.
     *
     * @return element name
     */
    public String getElement()
    {
        return element;
    }

    /**
     * Set the element name.
     *
     * @param element new value for element
     */
    public void setElement(String element)
    {
        this.element = element;
    }

    /**
     * Get the qualifier.
     *
     * @return qualifier
     */
    public String getQualifier()
    {
        return qualifier;
    }

    /**
     * Set the qualifier.
     *
     * @param qualifier new value for qualifier
     */
    public void setQualifier(String qualifier)
    {
        this.qualifier = qualifier;
    }

    /**
     * Get the scope note.
     *
     * @return scope note
     */
    public String getScopeNote()
    {
        return scopeNote;
    }

    /**
     * Set the scope note.
     *
     * @param scopeNote new value for scope note
     */
    public void setScopeNote(String scopeNote)
    {
        this.scopeNote = scopeNote;
    }

    /**
     * Get the schema .
     *
     * @return schema record
     */
    public MetadataSchema getMetadataSchema() {
        return metadataSchema;
    }

    /**
     * Set the schema record key.
     *
     * @param metadataSchema new value for key
     */
    public void setMetadataSchema(MetadataSchema metadataSchema) {
        this.metadataSchema = metadataSchema;
    }



    /**
     * Return <code>true</code> if <code>other</code> is the same MetadataField
     * as this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         MetadataField as this object
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
        final MetadataField other = (MetadataField) obj;
        if (this.getID() != other.getID())
        {
            return false;
        }
        if (!getMetadataSchema().equals(other.getMetadataSchema()))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 47 * hash + getID();
        hash = 47 * hash + getMetadataSchema().getID();
        return hash;
    }

    public String toString(char separator) {
        if (qualifier == null)
        {
            return getMetadataSchema().getName() + separator + element;
        }
        else
        {
            return getMetadataSchema().getName() + separator + element + separator + qualifier;
        }
    }

    @Override
    public String toString() {
        return toString('_');
    }
}
