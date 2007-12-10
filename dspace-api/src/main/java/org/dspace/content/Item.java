/*
 * Item.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.Thumbnail;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.content.dao.BitstreamFormatDAOFactory;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.event.Event;

/**
 * Class representing an item in DSpace. Note that everything is held in memory
 * until update() is called on the ItemDAO.
 * 
 * @author James Rutherford
 * @version $Revision$
 */
public class Item extends DSpaceObject
{
    private static Logger log = Logger.getLogger(Item.class);

    public static final String ANY = "*";

    protected ItemDAO dao;
    protected BitstreamDAO bitstreamDAO;
    protected BitstreamFormatDAO bitstreamFormatDAO;
    protected BundleDAO bundleDAO;
    protected CollectionDAO collectionDAO;
    protected CommunityDAO communityDAO;
    protected EPersonDAO epersonDAO;

    protected String identifier;
    protected boolean inArchive;
    protected boolean withdrawn;
    protected Date lastModified;

    protected int owningCollectionId;
    protected Collection owningCollection;
    protected int submitterId;
    protected EPerson submitter;

    protected List<Bundle> bundles;
    protected List<DCValue> metadata;

    protected boolean metadataChanged;

    /**
     * True if anything else was changed since last update()
     * (to drive event mechanism)
     */
    private boolean modified;
    
    public Item(Context context, int id)
    {
        this.id = id;
        this.context = context;

        modified = false;
        clearDetails();
        
        dao = ItemDAOFactory.getInstance(context);
        bitstreamDAO = BitstreamDAOFactory.getInstance(context);
        bitstreamFormatDAO = BitstreamFormatDAOFactory.getInstance(context);
        bundleDAO = BundleDAOFactory.getInstance(context);
        collectionDAO = CollectionDAOFactory.getInstance(context);
        communityDAO = CommunityDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);

        identifiers = new ArrayList<ExternalIdentifier>();
        bundles = new ArrayList<Bundle>();
        metadata = new ArrayList<DCValue>();
        metadataChanged = false;
    }

    /**
     * Find out if the item is part of the main archive
     * 
     * @return true if the item is in the main archive
     */
    public boolean isArchived()
    {
        return inArchive;
    }

    /**
     * Only <code>WorkflowItem.archive()</code> should really set this.
     * 
     * @param inArchive new value for the flag
     */
    public void setArchived(boolean inArchive)
    {
        this.inArchive = inArchive;
    }

    /**
     * Find out if the item has been withdrawn
     * 
     * @return true if the item has been withdrawn
     */
    public boolean isWithdrawn()
    {
        return withdrawn;
    }

    public void setWithdrawn(boolean withdrawn)
    {
        this.withdrawn = withdrawn;
    }

    /**
     * Get the date the item was last modified, or the current date if
     * last_modified is null
     * 
     * @return the date the item was last modified, or the current date if the
     *         column is null.
     */
    public Date getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(Date date)
    {
        // FIXME: Sanity checks!
        lastModified = date;
    }

    /**
     * Get the owning Collection for the item
     * 
     * @return Collection that is the owner of the item
     */
    public Collection getOwningCollection()
    {
        modified = true;
        return owningCollection;
    }

    /**
     * List the owning Collection for the item
     * 
     * @param owningCollection Collection
     */
    public void setOwningCollection(Collection owningCollection)
    {
        this.owningCollection = owningCollection;
        modified = true;
    }

    public void setOwningCollectionId(int owningCollectionId)
    {
        this.owningCollectionId = owningCollectionId;
    }

    public List<DCValue> getMetadata()
    {
        return metadata;
    }

    public void setMetadata(List<DCValue> metadata)
    {
        this.metadata = metadata;
    }

    /**
     * Get metadata for the item in a chosen schema.
     * See <code>MetadataSchema</code> for more information about schemas.
     * Passing in a <code>null</code> value for <code>qualifier</code>
     * or <code>lang</code> only matches metadata fields where that
     * qualifier or languages is actually <code>null</code>.
     * Passing in <code>Item.ANY</code>
     * retrieves all metadata fields with any value for the qualifier or
     * language, including <code>null</code>
     * <P>
     * Examples:
     * <P>
     * Return values of the unqualified "title" field, in any language.
     * Qualified title fields (e.g. "title.uniform") are NOT returned:
     * <P>
     * <code>item.getMetadata("dc", "title", null, Item.ANY );</code>
     * <P>
     * Return all US English values of the "title" element, with any qualifier
     * (including unqualified):
     * <P>
     * <code>item.getMetadata("dc, "title", Item.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant. When retrieving with wildcards, values of a
     * particular element/qualifier/language combinations will be adjacent, but
     * the overall ordering of the combinations is indeterminate.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the element name. <code>Item.ANY</code> matches any
     *            element. <code>null</code> doesn't really make sense as all
     *            metadata must have an element.
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are returned, and
     *            <code>Item.ANY</code> means values with any country code or
     *            no country code are returned.
     * @return metadata fields that match the parameters
     */
    public DCValue[] getMetadata(String schema, String element, String qualifier,
            String lang)
    {
        // Build up list of matching values
        List<DCValue> values = new ArrayList<DCValue>();

        for (DCValue dcv : metadata)
        {
            if (match(schema, element, qualifier, lang, dcv))
            {
                // We will return a copy of the object in case it is altered
                DCValue copy = new DCValue();
                copy.element = dcv.element;
                copy.qualifier = dcv.qualifier;
                copy.value = dcv.value;
                copy.language = dcv.language;
                copy.schema = dcv.schema;

                values.add(copy);
            }
        }

        return values.toArray(new DCValue[0]);
    }
    
    /**
     * Retrieve metadata field values from a given metadata string
     * of the form <schema prefix>.<element>[.<qualifier>|.*]
     *
     * @param mdString
     *            The metadata string of the form
     *            <schema prefix>.<element>[.<qualifier>|.*]
     */
    public DCValue[] getMetadata(String mdString)
    {
        StringTokenizer st = new StringTokenizer(mdString, ".");
        
        String[] tokens = { "", "", "" };
        int i = 0;
        while(st.hasMoreTokens())
        {
            tokens[i] = st.nextToken().toLowerCase().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];
        
        DCValue[] values;
        if ("*".equals(qualifier))
        {
            values = getMetadata(schema, element, Item.ANY, Item.ANY);
        }
        else if ("".equals(qualifier))
        {
            values = getMetadata(schema, element, null, Item.ANY);
        }
        else
        {
            values = getMetadata(schema, element, qualifier, Item.ANY);
        }
        
        return values;
    }
    
    /**
     * Add metadata fields. These are appended to existing values.
     * Use <code>clearDC</code> to remove values. The ordering of values
     * passed in is maintained.
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifer name, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param values
     *            the values to add.
     */
    public void addMetadata(String schema, String element, String qualifier,
            String lang, String... values)
    {
        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (String value : values)
        {
            DCValue dcv = new DCValue();
            dcv.schema = schema;
            dcv.element = element;
            dcv.qualifier = qualifier;
            dcv.language = lang;

            if (value != null && !value.trim().equals(""))
            {
                // remove control unicode char
                String temp = value.trim();
                char[] dcvalue = temp.toCharArray();
                for (int charPos = 0; charPos < dcvalue.length; charPos++)
                {
                    if (Character.isISOControl(dcvalue[charPos]) &&
                        !String.valueOf(dcvalue[charPos]).equals("\u0009") &&
                        !String.valueOf(dcvalue[charPos]).equals("\n") &&
                        !String.valueOf(dcvalue[charPos]).equals("\r"))
                    {
                        dcvalue[charPos] = ' ';
                    }
                }
                dcv.value = String.valueOf(dcvalue);
            }
            else
            {
                continue;
//                dcv.value = null;
            }
            
            if (!metadata.contains(dcv))
            {
                metadata.add(dcv);
                metadataChanged = true;
            }

            addDetails(schema+"."+element+((qualifier==null)? "": "."+qualifier));
        }
    }

    /**
     * Clear metadata values. As with <code>getDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.<code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>item.clearDC(Item.ANY, Item.ANY, Item.ANY)</code> will
     * remove all Dublin Core metadata associated with an item.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the Dublin Core element to remove, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are removed, and <code>Item.ANY</code>
     *            means values with any country code or no country code are
     *            removed.
     */
    public void clearMetadata(String schema, String element, String qualifier,
            String lang)
    {
        if (metadata.size() == 0)
        {
            return;
        }

        Iterator<DCValue> i = metadata.iterator();
        while (i.hasNext())
        {
            if (match(schema, element, qualifier, lang, i.next()))
            {
                i.remove();
                metadataChanged = true;
            }
        }
    }

    /**
     * Get the e-person that originally submitted this item
     * 
     * @return the submitter
     */
    public EPerson getSubmitter()
    {
        return submitter;
    }

    /**
     * List the e-person that originally submitted this item. This is a public
     * method since it is handled by the WorkspaceItem class in the ingest
     * package. <code>update</code> must be called to write the change to the
     * database.
     * 
     * @param submitter
     *            the submitter
     */
    public void setSubmitter(EPerson submitter)
    {
        this.submitter = submitter;
        submitterId = submitter.getID();
        modified = true;
    }

    public void setSubmitter(int submitterId)
    {
        setSubmitter(epersonDAO.retrieve(submitterId));
    }

    /**
     * Get the bundles in this item.
     * 
     * @return the bundles in an unordered array
     */
    public Bundle[] getBundles()
    {
        return bundles.toArray(new Bundle[0]);
    }

    public void setBundles(List<Bundle> bundles)
    {
        this.bundles = bundles;
    }

    /**
     * Get the bundles matching a bundle name (name corresponds roughly to type)
     * 
     * @param name
     *            name of bundle (ORIGINAL/TEXT/THUMBNAIL)
     * 
     * @return the bundles in an unordered array
     */
    public Bundle[] getBundles(String name)
    {
        List<Bundle> tmp = new ArrayList<Bundle>();
        for (Bundle bundle : getBundles())
        {
            if (name.equals(bundle.getName()))
            {
                tmp.add(bundle);
            }
        }
        return tmp.toArray(new Bundle[0]);
    }

    /**
     * Create a bundle in this item, with immediate effect
     * 
     * @param name
     *            bundle name (ORIGINAL/TEXT/THUMBNAIL)
     * @return the newly created bundle
     * @throws AuthorizeException
     */
    public Bundle createBundle(String name) throws AuthorizeException
    {
        if ((name == null) || "".equals(name))
        {
            throw new RuntimeException("Bundle must be created with non-empty name");
        }

        AuthorizeManager.authorizeAction(context, this, Constants.ADD);

        // FIXME: Ideally, we wouldn't reach into the DAO layer here, we'd just
        // let everything fall into place when item.update() is called, but I
        // haven't quite worked out the logistics of that yet. Basically, we
        // need the behaviour to propogate downwards (ie: through bundles into
        // bitstreams) because bitstreams don't yet use DAOs, they do the
        // creation immediately. Once Bitstreams use DAOs, we *should* be able
        // to replace this code with the following:
        // Addendum: That didn't seem to do it. Need to figure out why.
        /*
        Bundle b = new Bundle();
        b.setName(name);
        addBundle(b);
        return b;
        */
        Bundle b = bundleDAO.create();
        b.setName(name);
        bundleDAO.update(b);

        addBundle(b);

        return b;
    }

    /**
     * Add an existing bundle to this item. This has immediate effect.
     * 
     * @param b
     *            the bundle to add
     */
    public void addBundle(Bundle b) throws AuthorizeException
    {
        // Check it's not already there
        for (Bundle bundle : getBundles())
        {
            if ((b.getName().equals(bundle.getName())) ||
                (b.getID() == bundle.getID()))
            {
                // Bundle is a duplicate, do nothing
                return;
            }
        }

        AuthorizeManager.inheritPolicies(context, this, b);

        bundles.add(b);

        context.addEvent(new Event(Event.ADD, Constants.ITEM, getID(), Constants.BUNDLE, b.getID(), b.getName()));
    }

    /**
     * Remove a bundle. This may result in the bundle being deleted, if the
     * bundle is orphaned.
     *
     * FIXME: Will this ever not be the case? Can multiple Items own the same
     * Bundle? (I think the answer is no).
     * 
     * @param b
     *            the bundle to remove
     */
    public void removeBundle(Bundle b) throws AuthorizeException
    {
        AuthorizeManager.authorizeAction(context, this, Constants.REMOVE);

        log.info(LogManager.getHeader(context, "remove_bundle", "item_id="
                + getID() + ",bundle_id=" + b.getID()));

        Iterator<Bundle> i = bundles.iterator();
        while (i.hasNext())
        {
            if (i.next().getID() == b.getID())
            {
                i.remove();
            }
        }

        context.addEvent(new Event(Event.REMOVE, Constants.ITEM, getID(), Constants.BUNDLE, b.getID(), b.getName()));
    }

    /**
     * Create a single bitstream in a new bundle. Provided as a convenience
     * method for the most common use.
     * 
     * @param is
     *            the stream to create the new bitstream from
     * @param name
     *            is the name of the bundle (ORIGINAL, TEXT, THUMBNAIL)
     * @return Bitstream that is created
     * @throws AuthorizeException
     * @throws IOException
     */
    public Bitstream createSingleBitstream(InputStream is, String name)
            throws AuthorizeException, IOException
    {
        // Authorisation is checked by methods below
        // Create a bundle
        log.info("creating bundle " + name);
        Bundle bnd = createBundle(name);
        log.info("creating bitstream");
        Bitstream bitstream = bnd.createBitstream(is);
        log.info("adding bundle to item");
        addBundle(bnd);

        // FIXME: Create permissions for new bundle + bitstream
        return bitstream;
    }

    /**
     * Convenience method, calls createSingleBitstream() with name "ORIGINAL"
     * 
     * @param is
     *            InputStream
     * @return created bitstream
     * @throws AuthorizeException
     * @throws IOException
     */
    public Bitstream createSingleBitstream(InputStream is)
            throws AuthorizeException, IOException
    {
        return createSingleBitstream(is, "ORIGINAL");
    }

    /**
     * Get all non-internal bitstreams in the item. This is mainly used for
     * auditing for provenance messages and adding format.* DC values. The order
     * is indeterminate.
     * 
     * @return non-internal bitstreams.
     */
    public Bitstream[] getNonInternalBitstreams()
    {
        List<Bitstream> bitstreamList = new ArrayList<Bitstream>();

        // Go through the bundles and bitstreams picking out ones which aren't
        // of internal formats
        for (Bundle b : getBundles())
        {
            Bitstream[] bitstreams = b.getBitstreams();

            for (int j = 0; j < bitstreams.length; j++)
            {
                if (!bitstreams[j].getFormat().isInternal())
                {
                    // Bitstream is not of an internal format
                    bitstreamList.add(bitstreams[j]);
                }
            }
        }

        Bitstream[] bsArray = new Bitstream[bitstreamList.size()];
        bsArray = (Bitstream[]) bitstreamList.toArray(bsArray);

        return bsArray;
    }

    /**
     * Store a copy of the license a user granted in this item.
     * 
     * @param license
     *            the license the user granted
     * @param eperson
     *            the eperson who granted the license
     * @throws IOException
     * @throws AuthorizeException
     */
    public void licenseGranted(String license, EPerson eperson)
            throws IOException, AuthorizeException
    {
        // Put together text to store
        String licenseText = "License granted by " + eperson.getFullName()
                + " (" + eperson.getEmail() + ") on "
                + DCDate.getCurrent().toString() + " (GMT):\n\n" + license;

        // Store text as a bitstream
        byte[] licenseBytes = licenseText.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(licenseBytes);
        Bitstream b = createSingleBitstream(bais, "LICENSE");

        // Now set the format and name of the bitstream
        b.setName("license.txt");
        b.setSource("Written by org.dspace.content.Item");

        // Find the License format
        BitstreamFormat bf =
                bitstreamFormatDAO.retrieveByShortDescription("License");
        b.setFormat(bf);

        bitstreamDAO.update(b);
    }

    /**
     * Remove just the DSpace license from an item This is useful to update the
     * current DSpace license, in case the user must accept the DSpace license
     * again (either the item was rejected, or resumed after saving)
     * <p>
     * This method is used by the org.dspace.submit.step.LicenseStep class
     * 
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeDSpaceLicense() throws AuthorizeException,
            IOException
    {
        // get all bundles with name "LICENSE" (these are the DSpace license
        // bundles)
        Bundle[] bunds = getBundles("LICENSE");

        for (int i = 0; i < bunds.length; i++)
        {
            // FIXME: probably serious troubles with Authorizations
            // fix by telling system not to check authorization?
            removeBundle(bunds[i]);
        }
    }

    /**
     * Remove all licenses from an item - it was rejected
     * 
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeLicenses() throws AuthorizeException,
            IOException
    {
        // Find the License format
        BitstreamFormat bf =
                bitstreamFormatDAO.retrieveByShortDescription("License");
        int licensetype = bf.getID();

        // search through bundles, looking for bitstream type license
        for (Bundle bundle : getBundles())
        {
            boolean removethisbundle = false;

            for (Bitstream bitstream : bundle.getBitstreams())
            {
                BitstreamFormat format = bitstream.getFormat();

                if (format.getID() == licensetype)
                {
                    removethisbundle = true;
                }
            }

            // probably serious troubles with Authorizations
            // fix by telling system not to check authorization?
            if (removethisbundle)
            {
                removeBundle(bundle);
            }
        }
    }

    /**
     * Withdraw the item from the archive. It is kept in place, and the content
     * and metadata are not deleted, but it is not publicly accessible.
     * 
     * @throws AuthorizeException
     * @throws IOException
     */
    @Deprecated
    public void withdraw() throws AuthorizeException, IOException
    {
        ArchiveManager.withdrawItem(context, this);

		context.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), "WITHDRAW"));
    }

    /**
     * Reinstate a withdrawn item
     * 
     * @throws AuthorizeException
     * @throws IOException
     */
    @Deprecated
    public void reinstate() throws AuthorizeException, IOException
    {
        ArchiveManager.reinstateItem(context, this);
        context.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), "REINSTATE"));
    }

    /**
     * Return true if the given Collection 'owns' this item.
     * 
     * @param c Collection
     * @return true if this Collection owns this item
     */
    public boolean isOwningCollection(Collection c)
    {
        if (owningCollectionId > 0)
        {
            if (c.getID() == owningCollectionId)
            {
                return true;
            }
        }
        else if (owningCollection != null)
        {
            if (c.getID() == owningCollection.getID())
            {
                return true;
            }
        }

        // not the owner
        return false;
    }

    /**
     * Return type found in Constants
     * 
     * @return int Constants.ITEM
     */
    public int getType()
    {
        return Constants.ITEM;
    }

    /**
     * remove all of the policies for item and replace them with a new list of
     * policies
     * 
     * @param newpolicies -
     *            this will be all of the new policies for the item and its
     *            contents
     * @throws AuthorizeException
     *
     * FIXME: Move this somewhere more appropriate. It isn't the responsibility
     * of the Item to do this kind of thing.
     */
    public void replaceAllItemPolicies(List newpolicies)
        throws AuthorizeException
    {
        // remove all our policies, add new ones
        AuthorizeManager.removeAllPolicies(context, this);
        AuthorizeManager.addPolicies(context, newpolicies, this);
    }

    /**
     * remove all of the policies for item's bitstreams and bundles and replace
     * them with a new list of policies
     * 
     * @param newpolicies -
     *            this will be all of the new policies for the bundle and
     *            bitstream contents
     * @throws AuthorizeException
     *
     * FIXME: Move this somewhere more appropriate. It isn't the responsibility
     * of the Item to do this kind of thing.
     */
    public void replaceAllBitstreamPolicies(List newpolicies)
            throws AuthorizeException
    {
        // remove all policies from bundles, add new ones
        // Remove bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bundle mybundle = bunds[i];

            Bitstream[] bs = mybundle.getBitstreams();

            for (int j = 0; j < bs.length; j++)
            {
                Bitstream mybitstream = bs[j];

                // change bitstream policies
                AuthorizeManager.removeAllPolicies(context, bs[j]);
                AuthorizeManager.addPolicies(context, newpolicies, bs[j]);
            }

            // change bundle policies
            AuthorizeManager.removeAllPolicies(context, mybundle);
            AuthorizeManager.addPolicies(context, newpolicies, mybundle);
        }
    }

    /**
     * remove all of the policies for item's bitstreams and bundles that belong
     * to a given Group
     * 
     * @param g
     *            Group referenced by policies that needs to be removed
     *
     * FIXME: Move this somewhere more appropriate. It isn't the responsibility
     * of the Item to do this kind of thing.
     */
    public void removeGroupPolicies(Group g)
    {
        // remove Group's policies from Item
        AuthorizeManager.removeGroupPolicies(context, this, g);

        // remove all policies from bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bundle mybundle = bunds[i];

            Bitstream[] bs = mybundle.getBitstreams();

            for (int j = 0; j < bs.length; j++)
            {
                Bitstream mybitstream = bs[j];

                // remove bitstream policies
                AuthorizeManager.removeGroupPolicies(context, bs[j], g);
            }

            // change bundle policies
            AuthorizeManager.removeGroupPolicies(context, mybundle, g);
        }
    }

    /**
     * remove all policies on an item and its contents, and replace them with
     * the DEFAULT_ITEM_READ and DEFAULT_BITSTREAM_READ policies belonging to
     * the collection.
     * 
     * @param c
     *            Collection
     * @throws AuthorizeException
     *
     * FIXME: Move this somewhere more appropriate. It isn't the responsibility
     * of the Item to do this kind of thing.
     */
    public void inheritCollectionDefaultPolicies(Collection c)
            throws AuthorizeException
    {
        // remove the submit authorization policies
        // and replace them with the collection's default READ policies
        List policies = AuthorizeManager.getPoliciesActionFilter(context, c,
                Constants.DEFAULT_ITEM_READ);

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        Iterator i = policies.iterator();

        // MUST have default policies
        if (!i.hasNext())
        {
            throw new RuntimeException("Collection " + c.getID()
                    + " has no default item READ policies");
        }

        while (i.hasNext())
        {
            ResourcePolicy rp = (ResourcePolicy) i.next();
            rp.setAction(Constants.READ);
        }

        replaceAllItemPolicies(policies);

        policies = AuthorizeManager.getPoliciesActionFilter(context, c,
                Constants.DEFAULT_BITSTREAM_READ);

        // change the action to just READ
        // just don't call update on the resourcepolicies!!!
        i = policies.iterator();

        if (!i.hasNext())
        {
            throw new RuntimeException("Collection " + c.getID()
                    + " has no default bitstream READ policies");
        }

        while (i.hasNext())
        {
            ResourcePolicy rp = (ResourcePolicy) i.next();
            rp.setAction(Constants.READ);
        }

        replaceAllBitstreamPolicies(policies);
    }

    /**
     * return TRUE if context's user can edit item, false otherwise
     * 
     * @return boolean true = current user can edit item
     */
    public boolean canEdit()
    {
        // can this person write to the item?
        if (AuthorizeManager.authorizeActionBoolean(context, this,
                Constants.WRITE))
        {
            return true;
        }

        // is this collection not yet created, and an item template is created
        if (getOwningCollection() == null)
        {
            return true;
        }

        // is this person an COLLECTION_EDITOR for the owning collection?
        if (getOwningCollection().canEditBoolean())
        {
            return true;
        }

        // is this person an COLLECTION_EDITOR for the owning collection?
        return AuthorizeManager.authorizeActionBoolean(context,
                getOwningCollection(), Constants.COLLECTION_ADMIN);

    }

    public String getName()
    {
        DCValue t[] = getMetadata("dc", "title", null, Item.ANY);
        return (t.length >= 1) ? t[0].value : null;
    }

    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>DCValue</code> object passed
     * in.  Any or all of the elemenent, qualifier and language passed
     * in can be the <code>Item.ANY</code> wildcard.
     *
     * FIXME: It's a bit filthy and horrid to make this protected, but I need
     * to access from the ItemProxy subclass. Really, it should exist somewhere
     * else, possibly in DCValue.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the element to match, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier to match, or <code>Item.ANY</code>
     * @param language
     *            the language to match, or <code>Item.ANY</code>
     * @param dcv
     *            the Dublin Core value
     * @return <code>true</code> if there is a match
     */
    protected boolean match(String schema, String element, String qualifier,
            String language, DCValue dcv)
    {
        // We will attempt to disprove a match - if we can't we have a match
        if (!element.equals(Item.ANY) && !element.equals(dcv.element))
        {
            // Elements do not match, no wildcard
            return false;
        }

        if (qualifier == null)
        {
            // Value must be unqualified
            if (dcv.qualifier != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!qualifier.equals(Item.ANY))
        {
            // Not a wildcard, so qualifier must match exactly
            if (!qualifier.equals(dcv.qualifier))
            {
                return false;
            }
        }

        if (language == null)
        {
            // Value must be null language to match
            if (dcv.language != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!language.equals(Item.ANY))
        {
            // Not a wildcard, so language must match exactly
            if (!language.equals(dcv.language))
            {
                return false;
            }
        }
        else if (!schema.equals(Item.ANY))
        {
            if (dcv.schema != null && !dcv.schema.equals(schema))
            {
                // The namespace doesn't match
                return false;
            }
        }

        // If we get this far, we have a match
        return true;
    }

    ////////////////////////////////////////////////////////////////////
    // Stuff from BrowseItem
    ////////////////////////////////////////////////////////////////////

    /**
     * Get a thumbnail object out of the item.
     * 
     * Warning: using this method actually instantiates an Item, which has a
     * corresponding performance hit on the database during browse listing
     * rendering.  That's your own fault for wanting to put images on your
     * browse page!
     * 
     * @return
     */
    public Thumbnail getThumbnail()
    {
        // if there's no original, there is no thumbnail
        Bundle[] original = getBundles("ORIGINAL");
        if (original.length == 0)
        {
            return null;
        }
        
        // if multiple bitstreams, check if the primary one is HTML
        boolean html = false;
        if (original[0].getBitstreams().length > 1)
        {
            Bitstream[] bitstreams = original[0].getBitstreams();

            for (int i = 0; (i < bitstreams.length) && !html; i++)
            {
                if (bitstreams[i].getID() == original[0].getPrimaryBitstreamID())
                {
                    html = bitstreams[i].getFormat().getMIMEType().equals("text/html");
                }
            }
        }

        // now actually pull out the thumbnail (ouch!)
        Bundle[] thumbs = getBundles("THUMBNAIL");
        
        // if there are thumbs and we're not dealing with an HTML item
        // then show the thumbnail
        if ((thumbs.length > 0) && !html)
        {
            Bitstream thumbnailBitstream;
            Bitstream originalBitstream;
            
            if ((original[0].getBitstreams().length > 1) && (original[0].getPrimaryBitstreamID() > -1))
            {
                originalBitstream = Bitstream.find(context, original[0].getPrimaryBitstreamID());
                thumbnailBitstream = thumbs[0].getBitstreamByName(originalBitstream.getName() + ".jpg");
            }
            else
            {
                originalBitstream = original[0].getBitstreams()[0];
                thumbnailBitstream = thumbs[0].getBitstreams()[0];
            }
            
            if ((thumbnailBitstream != null)
                    && (AuthorizeManager.authorizeActionBoolean(context, thumbnailBitstream, Constants.READ)))
            {
                Thumbnail thumbnail = new Thumbnail(thumbnailBitstream, originalBitstream);
                return thumbnail;
            }
        }

        return null;
    }

    ////////////////////////////////////////////////////////////////////
    // Deprecated methods
    ////////////////////////////////////////////////////////////////////

    @Deprecated
    public static Item find(Context context, int id)
    {
        return ItemDAOFactory.getInstance(context).retrieve(id);
    }

    @Deprecated
    static Item create(Context context) throws AuthorizeException
    {
        Item item = ItemDAOFactory.getInstance(context).create();

        context.addEvent(new Event(Event.CREATE, Constants.ITEM, item.getID(), null));

        return item;
    }

    @Deprecated
    public static ItemIterator findAll(Context context)
    {
        ItemDAO dao = ItemDAOFactory.getInstance(context);
        List<Item> items = dao.getItems();

        return new ItemIterator(context, items);
    }

    @Deprecated
    public static ItemIterator findBySubmitter(Context context,
            EPerson eperson)
    {
        ItemDAO dao = ItemDAOFactory.getInstance(context);
        List<Item> items = dao.getItemsBySubmitter(eperson);

        ArrayList<Integer> list = new ArrayList<Integer>();
        
        for (Item i : items)
        {
            list.add(i.getID());
        }

        return new ItemIterator(context, list);
    }

    @Deprecated
    public Collection[] getCollections()
    {
        List<Collection> parents = collectionDAO.getParentCollections(this);
        return (Collection[]) parents.toArray(new Collection[0]);
    }

    @Deprecated
    public Community[] getCommunities()
    {
        List<Community> parents = communityDAO.getParentCommunities(this);
        return (Community[]) parents.toArray(new Community[0]);
    }

    @Deprecated
    public void decache()
    {
        dao.decache(this);
    }

    @Deprecated
    public void update() throws AuthorizeException
    {
        dao.update(this);
        context.addEvent(new Event(Event.MODIFY_METADATA, Constants.ITEM, getID(), getDetails()));
        metadataChanged = false;
        clearDetails();
    }

    @Deprecated
    void delete() throws AuthorizeException, IOException
    {
        dao.delete(this.getID());
    }

    /** Deprecated because we shouldn't be referencing DC explicitly any more */
    @Deprecated
    public DCValue[] getDC(String element, String qualifier, String lang)
    {
        return getMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang);
    }

    @Deprecated
    public void addDC(String element, String qualifier, String lang,
            String... values)
    {
        addMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang, values);
    }

    @Deprecated
    public void clearDC(String element, String qualifier, String lang)
    {
        clearMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang);
    }

    /**
     * Moves the item from one collection to another one
     *
     * @throws AuthorizeException
     * @throws IOException
     */
    @Deprecated
    public void move (Collection from, Collection to) throws AuthorizeException, IOException
    {
        ArchiveManager.move(context, this, from, to);
        
        /*
        if (isOwningCollection(from))
        {
            setOwningCollection(to);
            update();
        }

        to.addItem(this);
        from.removeItem(this);
        */
    }

    /**
     * Get the collections this item is not in.
     *
     * @return the collections this item is not in, if any.
     */
    @Deprecated
    public Collection[] getCollectionsNotLinked()
    {
        return collectionDAO.getCollectionsNotLinked(this).toArray(new Collection[0]);
    }
}
