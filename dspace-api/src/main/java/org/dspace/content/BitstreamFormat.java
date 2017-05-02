/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

/**
 * Class representing a particular bitstream format.
 * <P>
 * Changes to the bitstream format metadata are only written to the database
 * when <code>update</code> is called.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
@Entity
@Table(name="bitstreamformatregistry")
public class BitstreamFormat implements Serializable, ReloadableEntity<Integer>
{

    @Id
    @Column(name="bitstream_format_id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="bitstreamformatregistry_seq")
    @SequenceGenerator(name="bitstreamformatregistry_seq", sequenceName="bitstreamformatregistry_seq", allocationSize = 1, initialValue = 1)
    private Integer id;

    @Column(name="short_description", length = 128, unique = true)
    private String shortDescription;

//    @Column(name="description")
//    @Lob //Generates a TEXT or LONGTEXT data type
    @Column(name="description", columnDefinition = "text")
    private String description;


    @Column(name="mimetype", length = 256)
    private String mimetype;

    @Column(name="support_level")
    private int supportLevel = -1;

    @Column(name="internal")
    private boolean internal = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name="fileextension", joinColumns=@JoinColumn(name="bitstream_format_id"))
        @CollectionId(
                columns = @Column(name="file_extension_id"),
                type=@Type(type="integer"),
                generator = "fileextension_seq"
        )
    @SequenceGenerator(name="fileextension_seq", sequenceName="fileextension_seq", allocationSize = 1)
    @Column(name="extension")
    @Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    private List<String> fileExtensions;

    @Transient
    private transient BitstreamFormatService bitstreamFormatService;

    /**
     * The "unknown" support level - for bitstream formats that are unknown to
     * the system
     */
    @Transient
    public static final int UNKNOWN = 0;

    /**
     * The "known" support level - for bitstream formats that are known to the
     * system, but not fully supported
     */
    @Transient
    public static final int KNOWN = 1;

    /**
     * The "supported" support level - for bitstream formats known to the system
     * and fully supported.
     */
    @Transient
    public static final int SUPPORTED = 2;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.BitstreamFormatService#create(Context)}
     *
     */
    protected BitstreamFormat()
    {
        fileExtensions = new LinkedList<>();
    }

    /**
     * Get the internal identifier of this bitstream format
     * 
     * @return the internal identifier
     */
    public final Integer getID()
    {
        return id;
    }

    /**
     * Get a short (one or two word) description of this bitstream format
     * 
     * @return the short description
     */
    public String getShortDescription()
    {
        return shortDescription;
    }

    void setShortDescriptionInternal(String shortDescription) {
        this.shortDescription = shortDescription;
    }




    /**
     * Get a description of this bitstream format, including full application or
     * format name
     * 
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }


    /**
     * Set the description of the bitstream format
     * 
     * @param s
     *            the new description
     */
    public void setDescription(String s)
    {
        this.description = s;
    }

    /**
     * Get the MIME type of this bitstream format, for example
     * <code>text/plain</code>
     * 
     * @return the MIME type
     */
    public String getMIMEType()
    {
        return mimetype;
    }

    /**
     * Set the MIME type of the bitstream format
     * 
     * @param s
     *            the new MIME type
     */
    public void setMIMEType(String s)
    {
        this.mimetype = s;
    }

    /**
     * Get the support level for this bitstream format - one of
     * <code>UNKNOWN</code>,<code>KNOWN</code> or <code>SUPPORTED</code>.
     * 
     * @return the support level
     */
    public int getSupportLevel()
    {
        return supportLevel;
    }

    /**
     * Set the support level for this bitstream format - one of
     * <code>UNKNOWN</code>,<code>KNOWN</code> or <code>SUPPORTED</code>.
     *
     * @param supportLevel the support level
     */
    void setSupportLevelInternal(int supportLevel) {
        this.supportLevel = supportLevel;
    }

    /**
     * Find out if the bitstream format is an internal format - that is, one
     * that is used to store system information, rather than the content of
     * items in the system
     * 
     * @return <code>true</code> if the bitstream format is an internal type
     */
    public boolean isInternal()
    {
        return internal;
    }

    /**
     * Set whether the bitstream format is an internal format
     * 
     * @param b
     *            pass in <code>true</code> if the bitstream format is an
     *            internal type
     */
    public void setInternal(boolean b)
    {
        internal = b;
    }


    /**
     * Get the filename extensions associated with this format
     * 
     * @return the extensions
     */
    public List<String> getExtensions()
    {
        return fileExtensions;
    }

    /**
     * Set the filename extensions associated with this format
     * 
     * @param exts
     *            String [] array of extensions
     */
    public void setExtensions(List<String> exts)
    {
        this.fileExtensions = exts;
    }

        /*
        Getters & setters which should be removed on the long run, they are just here to provide all getters & setters to the item object
    */

    public void setShortDescription(Context context, String s) throws SQLException
    {
        getBitstreamFormatService().setShortDescription(context, this, s);
    }

    public void setSupportLevel(int sl)
    {
        getBitstreamFormatService().setSupportLevel(this, sl);
    }

    private BitstreamFormatService getBitstreamFormatService() {
        if(bitstreamFormatService == null) {
            bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
        }
        return bitstreamFormatService;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Collection
     * as this object, <code>false</code> otherwise
     *
     * @param other
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         collection as this object
     */
     @Override
     public boolean equals(Object other)
     {
         if (other == null)
         {
             return false;
         }
         Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(other);
         if (this.getClass() != objClass)
         {
             return false;
         }
         final BitstreamFormat otherBitstreamFormat = (BitstreamFormat) other;
         if (this.getID() != otherBitstreamFormat.getID() )
         {
             return false;
         }

         return true;
     }

     @Override
     public int hashCode()
     {
         int hash = 5;
         hash += 70 * hash + getID();
         return hash;
     }

}
