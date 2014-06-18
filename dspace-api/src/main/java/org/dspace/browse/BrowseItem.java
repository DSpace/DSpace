/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Entity class to represent an item that is being used to generate Browse
 * results.  This behaves in many was similar to the Item object, but its
 * metadata handling has been further optimised for performance in both
 * reading and writing, and it does not deal with other objects related to
 * items
 * 
 * FIXME: this class violates some of the encapsulation of the Item, but there is
 * unfortunately no way around this until DAOs and an interface are provided 
 * for the Item class.
 * 
 * @author Richard Jones
 *
 */
public class BrowseItem extends DSpaceObject
{
	/** Logger */
    private static Logger log = Logger.getLogger(BrowseItem.class);
    
    /** DSpace context */
	private Context context;
	
	/** a List of all the metadata */
	private List<DCValue> metadata = new ArrayList<DCValue>();
	
	/** database id of the item */
	private int id = -1;

    /** is the item in the archive */
    private boolean in_archive = true;

    /** is the item withdrawn */
    private boolean withdrawn  = false;

    /** is the item discoverable */
    private boolean discoverable = true;
    
    /** item handle */
	private String handle = null;

    /**
	 * Construct a new browse item with the given context and the database id
	 * 
	 * @param context	the DSpace context
     * @param id		the database id of the item
     * @param in_archive
     * @param withdrawn
     */
	public BrowseItem(Context context, int id, boolean in_archive, boolean withdrawn, boolean discoverable)
	{
		this.context = context;
		this.id = id;
        this.in_archive = in_archive;
        this.withdrawn = withdrawn;
        this.discoverable = discoverable;
    }

	/**
	 * Get String array of metadata values matching the given parameters
	 * 
	 * @param schema	metadata schema
	 * @param element	metadata element
	 * @param qualifier	metadata qualifier
	 * @param lang		metadata language
	 * @return			array of matching values
	 * @throws SQLException
	 */
	public DCValue[] getMetadata(String schema, String element, String qualifier, String lang)
		throws SQLException
	{
        try
        {
            BrowseItemDAO dao = BrowseDAOFactory.getItemInstance(context);

            // if the qualifier is a wildcard, we have to get it out of the
            // database
            if (Item.ANY.equals(qualifier))
            {
                return dao.queryMetadata(id, schema, element, qualifier, lang);
            }

            if (!metadata.isEmpty())
            {
                List<DCValue> values = new ArrayList<DCValue>();
                Iterator<DCValue> i = metadata.iterator();

                while (i.hasNext())
                {
                    DCValue dcv = i.next();

                    if (match(schema, element, qualifier, lang, dcv))
                    {
                        values.add(dcv);
                    }
                }

                if (values.isEmpty())
                {
                    DCValue[] dcvs = dao.queryMetadata(id, schema, element, qualifier, lang);
                    if (dcvs != null)
                    {
                    	Collections.addAll(metadata, dcvs);
                    }
                    return dcvs;
                }

                // else, Create an array of matching values
                DCValue[] valueArray = new DCValue[values.size()];
                valueArray = (DCValue[]) values.toArray(valueArray);

                return valueArray;
            }
            else
            {
                DCValue[] dcvs = dao.queryMetadata(id, schema, element, qualifier, lang);
                if (dcvs != null)
                {
                	Collections.addAll(metadata, dcvs);
                }
                return dcvs;
            }
        }
        catch (BrowseException be)
        {
            log.error("caught exception: ", be);
            return null;
        }
    }
	
	/**
	 * Get the type of object.  This object masquerades as an Item, so this
	 * returns the value of Constants.ITEM
	 * 
	 *@return Constants.ITEM
	 */
	public int getType()
	{
		return Constants.ITEM;
	}

	/**
	 * @deprecated
	 * @param real
	 */
	public int getType(boolean real)
	{
		if (!real)
		{
			return Constants.ITEM;
		}
		else
		{
			return getType();
		}
	}
	
	/**
	 * get the database id of the item
	 * 
	 *	@return database id of item
	 */
	public int getID()
	{
		return id;
	}

	/**
	 * Set the database id of the item
	 * 
	 * @param id	the database id of the item
	 */
	public void setID(int id)
	{
		this.id = id;
	}
	
	/**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>DCValue</code> object passed
     * in.  Any or all of the element, qualifier and language passed
     * in can be the <code>Item.ANY</code> wildcard.
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
    private boolean match(String schema, String element, String qualifier,
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

	/* (non-Javadoc)
	 * @see org.dspace.content.DSpaceObject#getHandle()
	 */
	public String getHandle()
	{
		// Get our Handle if any
		if (this.handle == null)
		{
			try
			{
				this.handle = HandleManager.findHandle(context, this);
			}
			catch (SQLException e)
			{
				log.error("caught exception: ", e);
			}
		}
		return this.handle;
	}
    
	/**
	 * Get a thumbnail object out of the item.
	 * 
	 * Warning: using this method actually instantiates an Item, which has a
	 * corresponding performance hit on the database during browse listing
	 * rendering.  That's your own fault for wanting to put images on your
	 * browse page!
	 * 
	 * @throws SQLException
	 */
    public Thumbnail getThumbnail()
    	throws SQLException
    {
    	// instantiate an item for this one.  Not nice.
        Item item = Item.find(context, id);
    	
    	if (item == null)
    	{
    		return null;
    	}
    	
    	// now go sort out the thumbnail
    	
    	// if there's no original, there is no thumbnail
    	Bundle[] original = item.getBundles("ORIGINAL");
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
        Bundle[] thumbs = item.getBundles("THUMBNAIL");
        
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
                return new Thumbnail(thumbnailBitstream, originalBitstream);
        	}
        }

        return null;
    }

	public String getName()
    {
        // FIXME: there is an exception handling problem here
		try
		{
			DCValue t[] = getMetadata("dc", "title", null, Item.ANY);
			return (t.length >= 1) ? t[0].value : null;
		}
		catch (SQLException sqle)
		{
        	log.error("caught exception: ", sqle);
			return null;
		}
    }

    @Override
    public void update() throws SQLException, AuthorizeException
    {

    }

    @Override
    public void updateLastModified()
    {

    }

    public boolean isArchived()
    {
        return in_archive;
    }

    public boolean isWithdrawn()
    {
        return withdrawn;
    }
    
    public boolean isDiscoverable() {
    	return discoverable;
	}
}
