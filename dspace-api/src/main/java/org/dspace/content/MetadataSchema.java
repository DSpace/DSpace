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
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;

/**
 * Class representing a schema in DSpace.
 * <p>
 * The schema object exposes a name which can later be used to generate
 * namespace prefixes in RDF or XML, e.g. the core DSpace Dublin Core schema
 * would have a name of <code>'dc'</code>.
 * </p>
 *
 * @author Martin Hald
 * @version $Revision$
 * @see org.dspace.content.MetadataValue
 * @see org.dspace.content.MetadataField
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name="metadataschemaregistry")
public class MetadataSchema implements ReloadableEntity<Integer>
{
    /** Short Name of built-in Dublin Core schema. */
    public static final String DC_SCHEMA = "dc";

    @Id
    @Column(name="metadata_schema_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="metadataschemaregistry_seq")
    @SequenceGenerator(name="metadataschemaregistry_seq", sequenceName="metadataschemaregistry_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "namespace", unique = true, length = 256)
    private String namespace;

    @Column(name = "short_id", unique = true, length = 32)
    private String name;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.MetadataSchemaService#create(Context, String, String)}
     *
     */
    protected MetadataSchema()
    {

    }

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
        final MetadataSchema other = (MetadataSchema) obj;
        if (this.id != other.id)
        {
            return false;
        }
        if ((this.namespace == null) ? (other.namespace != null) : !this.namespace.equals(other.namespace))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 67 * hash + this.id;
        hash = 67 * hash + (this.namespace != null ? this.namespace.hashCode() : 0);
        return hash;
    }

    /**
     * Get the schema namespace.
     *
     * @return namespace String
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Set the schema namespace.
     *
     * @param namespace  XML namespace URI
     */
    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    /**
     * Get the schema name.
     *
     * @return name String
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the schema name.
     *
     * @param name  short name of schema
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the schema record key number.
     *
     * @return schema record key
     */
    public Integer getID()
    {
        return id;
    }
}
