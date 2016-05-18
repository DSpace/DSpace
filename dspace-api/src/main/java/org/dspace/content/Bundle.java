/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.*;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BundleService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;

/**
 * Class representing bundles of bitstreams stored in the DSpace system
 * <P>
 * The corresponding Bitstream objects are loaded into memory. At present, there
 * is no metadata associated with bundles - they are simple containers. Thus,
 * the <code>update</code> method doesn't do much yet. Creating, adding or
 * removing bitstreams has instant effect in the database.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
@Entity
@Table(name="bundle")
public class Bundle extends DSpaceObject implements DSpaceObjectLegacySupport
{
    @Column(name="bundle_id", insertable = false, updatable = false)
    private Integer legacyId;

    @OneToOne
    @JoinColumn(name = "primary_bitstream_id")
    private Bitstream primaryBitstream;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="bundle2bitstream",
            joinColumns={@JoinColumn(name="bundle_id") },
            inverseJoinColumns={@JoinColumn(name="bitstream_id") }
    )
    @OrderColumn(name="bitstream_order")
    private final List<Bitstream> bitstreams = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "item2bundle",
            joinColumns = {@JoinColumn(name = "bundle_id", referencedColumnName = "uuid") },
            inverseJoinColumns = {@JoinColumn(name = "item_id", referencedColumnName = "uuid") }
    )
    private final List<Item> items = new ArrayList<>();

    @Transient
    protected transient BundleService bundleService;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.BundleService#create(Context, Item, String)}
     *
     */
    protected Bundle()
    {
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    /**
     * Get the name of the bundle
     * 
     * @return name of the bundle (ORIGINAL, TEXT, THUMBNAIL) or NULL if not set
     */
    @Override
    public String getName()
    {
        return getBundleService().getMetadataFirstValue(this, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
    }

    /**
     * Set the name of the bundle
     * 
     * @param context context
     * @param name
     *            string name of the bundle (ORIGINAL, TEXT, THUMBNAIL) are the
     *            values currently used
     * @throws SQLException if database error
     */
    public void setName(Context context, String name) throws SQLException
    {
        getBundleService().setMetadataSingleValue(context, this, MetadataSchema.DC_SCHEMA, "title", null, null, name);
    }

    /**
     * Get the primary bitstream ID of the bundle
     * 
     * @return primary bitstream ID or -1 if not set
     */
    public Bitstream getPrimaryBitstream()
    {
        return primaryBitstream;
    }

    /**
     * Set the primary bitstream ID of the bundle
     * 
     * @param bitstream
     *            primary bitstream (e.g. index html file)
     */
    public void setPrimaryBitstreamID(Bitstream bitstream)
    {
        primaryBitstream = bitstream;
        setModified();
    }

    /**
     * Unset the primary bitstream ID of the bundle
     */
    public void unsetPrimaryBitstreamID()
    {
    	primaryBitstream = null;
    }
    
    /**
     * Get the bitstreams in this bundle
     * 
     * @return the bitstreams
     */
    public List<Bitstream> getBitstreams() {
        return bitstreams;
    }

    void addBitstream(Bitstream bitstream){
        bitstreams.add(bitstream);
    }

    /**
     * Get the items this bundle appears in
     * 
     * @return array of <code>Item</code> s this bundle appears in
     */
    public List<Item> getItems() {
        return items;
    }

    void removeItem(Item item) {
        getItems().remove(item);
    }

    /**
     * Set the item this bundle appears in
     *
     * @return array of <code>Item</code> s this bundle appears in
     */
    void addItem(Item item) {
        getItems().add(item);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
        {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        if (this.getClass() != objClass)
        {
            return false;
        }
        final Bundle other = (Bundle) obj;
        if (this.getType() != other.getType())
        {
            return false;
        }
        if(!this.getID().equals(other.getID()))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash += 71 * hash + getType();
        hash += 71 * hash + getID().hashCode();
        return hash;
    }


    /**
     * return type found in Constants
     * @return bundle type
     */
    @Override
    public int getType()
    {
        return Constants.BUNDLE;
    }

    private BundleService getBundleService()
    {
        if(bundleService == null)
        {
            bundleService = ContentServiceFactory.getInstance().getBundleService();
        }
        return bundleService;
    }

}
