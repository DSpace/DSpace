/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * This is a validity object specifically implemented for the caching 
 * needs of DSpace, Manakin, and Cocoon.
 * 
 * <p>The basic idea is that, each time a DSpace object is rendered by a Cocoon
 * component, the object and everything about it that makes it unique should
 * be reflected in the validity object for the component. By following this 
 * principle, if the object has been updated externally then the cache will be
 * invalidated.
 * 
 * <p>This DSpaceValidity object makes this processes easier by abstracting out
 * the processes of determining what is unique about a DSpace object. A class
 * is expected to create a new DSpaceValidity object and add() to it all 
 * DSpaceObjects that are rendered by the component. This validity object will 
 * serialize all those objects to a string, take a hash of the string and compare
 * the hash of the string for any updates.
 * 
 * @author Scott Phillips
 */

public class DSpaceValidity implements SourceValidity
{
	
    private static final long serialVersionUID = 1L;
    
    /** The validityKey while it is being build, once it is completed. */
    protected StringBuffer validityKey;
    
    /** Simple flag to note if the object has been completed. */
    protected boolean completed = false;
    
    /** A hash of the validityKey taken after completion */
    protected long hash;
    
    /** The time when the validity is no longer assumed to be valid */
    protected long assumedValidityTime = 0;
    
    /** The length of time that a cache is assumed to be valid */
    protected long assumedValidityDelay = 0;


    transient protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
   	transient protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    transient protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();


    /**
     * Create a new DSpace validity object. 
     * 
     * @param initialValidityKey
     *      The initial string 
     */
    public DSpaceValidity(String initialValidityKey)
    {
        this.validityKey = new StringBuffer();

        if (initialValidityKey != null)
        {
            this.validityKey.append(initialValidityKey);
        }
    }

    public DSpaceValidity()
    {
        this(null);
    }
    
    /**
     * Complete this validity object. After the completion no more
     * objects may be added to the validity object and the object
     * will return as valid.
     * @return this.
     */
    public DSpaceValidity complete() 
    {    
        this.completed = true;
        this.hash = HashUtil.hash(validityKey);
        this.validityKey = null;
        
        // Set the forced validity time.
        if (assumedValidityDelay > 0)
        {
            resetAssumedValidityTime();
        }
        
        return this;
    }
    
    /**
     * Set the time delay for how long this cache will be assumed 
     * to be valid. When it is assumed valid no other checks will be
     * made to consider its validity, and once the time has expired 
     * a full validation will occur on the next cache hit. If the 
     * cache proves to be validated on this hit then the assumed 
     * validity timer is reset.
     * 
     * @param milliseconds The delay time in millieseconds.
     */
    public void setAssumedValidityDelay(long milliseconds )
    {
    	// record the delay time.
    	this.assumedValidityDelay = milliseconds;
    	
    	// Also add the delay time to the validity hash so if the
    	// admin changes the delay time then all the previous caches
    	// are invalidated.
    	this.validityKey.append("AssumedValidityDelay:").append(milliseconds);
    }
    
    /**
     * Set the time delay for how long this cache will be assumed to be valid.
     * 
     * <p>This method takes a string which is parsed for the delay time.  The string
     * must be of the following form: "{@code <integer> <scale>}" where scale is days,
     * hours, minutes, or seconds.
     * 
     * <p>Examples: "1 day" or "12 hours" or "1 hour" or "30 minutes"
     * 
     * @see #setAssumedValidityDelay(long)
     * 
     * @param delay The delay time in a variable scale.
     */
    public void setAssumedValidityDelay(String delay)
    {
    	if (delay == null || delay.length() == 0)
        {
            return;
        }
    	
    	String[] parts = delay.split(" ");
    	
    	if (parts == null || parts.length != 2)
        {
            throw new IllegalArgumentException("Error unable to parse the assumed validity delay time: \"" + delay + "\". All delays must be of the form \"<integer> <scale>\" where scale is either seconds, minutes, hours, or days.");
        }

    	long milliseconds = 0;
    	
    	long value = 0;
    	try { value = Long.valueOf(parts[0]); } 
    	catch (NumberFormatException nfe) {
    		throw new IllegalArgumentException("Error unable to parse the assumed validity delay time: \""+delay+"\". All delays must be of the form \"<integer> <scale>\" where scale is either seconds, minutes, hours, or days.",nfe);
    	}
    	
    	String scale = parts[1].toLowerCase();
    
    	if (scale.equals("weeks") || scale.equals("week"))
    	{
    		// milliseconds * seconds * minutes * hours * days * weeks
    		// 1000 * 60 * 60 * 24 * 7 * 1 = 604,800,000
    		milliseconds = value * 604800000;
    	}
    	else if (scale.equals("days") || scale.equals("day"))
    	{
    		// milliseconds * seconds * minutes * hours * days
    		// 1000 * 60 * 60 * 24 * 1 = 86,400,000
    		milliseconds = value * 86400000;
    	}
    	else if (scale.equals("hours") || scale.equals("hour"))
    	{
    		// milliseconds * seconds * minutes * hours
    		// 1000 * 60 * 60 * 1 = 3,600,000
    		milliseconds = value * 3600000;
    	}
    	else if (scale.equals("minutes") || scale.equals("minute"))
    	{
    		// milliseconds * second * minute
    		// 1000 * 60 * 1 = 60,000
    		milliseconds = value * 60000;
    	}
    	else if (scale.equals("seconds") || scale.equals("second"))
    	{
    		// milliseconds * second 
    		// 1000 * 1 = 1000
    		milliseconds = value * 1000;
    	}
    	else
    	{
    		throw new IllegalArgumentException("Error unable to parse the assumed validity delay time: \""+delay+"\". All delays must be of the form \"<integer> <scale>\" where scale is either seconds, minutes, hours, or days.");
        }
    	
    	// Now set the actual delay.
    	setAssumedValidityDelay(milliseconds);
    }
    
    
    /**
     * Add a DSpace object to the validity. The order in which objects 
     * are added to the validity object is important, ensure that 
     * objects are added in the *exact* same order each time a 
     * validity object is created.
     * 
     * Below are the following transitive rules for adding 
     * objects, i.e. if an item is added then all the item's
     * bundles and bitstreams will also be added.
     * 
     * <p>
     * {@literal Communities -> logo bitstream}
     * <br>
     * {@literal Collection -> logo bitstream}
     * <br>
     * {@literal Item -> bundles -> bitstream}
     * <br>
     * {@literal Bundles -> bitstreams}
     * <br>
     * {@literal EPeople -> groups}
     * 
     * @param context
     *          session context.
     * @param dso
     *          The object to add to the validity.
     * @throws java.sql.SQLException passed through.
     */
    public void add(Context context, DSpaceObject dso) throws SQLException
    {
        if (this.completed)
        {
            throw new IllegalStateException("Cannot add DSpaceObject to a completed validity object");
        }
        
        if (dso == null) 
        {
          this.validityKey.append("null");  
        }
        else if (dso instanceof Community)
        {
            Community community = (Community) dso;

            validityKey.append("Community:");
            validityKey.append(community.getHandle());
            validityKey.append(communityService.getMetadata(community, "introductory_text"));
            validityKey.append(communityService.getMetadata(community, "short_description"));
            validityKey.append(communityService.getMetadata(community, "side_bar_text"));
            validityKey.append(communityService.getMetadata(community, "copyright_text"));
            validityKey.append(communityService.getMetadata(community, "name"));
            
            // Add the communities logo
            this.add(context, community.getLogo());

        } 
        else if (dso instanceof Collection)
        {
            Collection collection = (Collection) dso;
            
            validityKey.append("Collection:");
            validityKey.append(collection.getHandle());
            validityKey.append(collectionService.getMetadata(collection, "introductory_text"));
            validityKey.append(collectionService.getMetadata(collection, "short_description"));
            validityKey.append(collectionService.getMetadata(collection, "side_bar_text"));
            validityKey.append(collectionService.getMetadata(collection, "provenance_description"));
            validityKey.append(collectionService.getMetadata(collection, "copyright_text"));
            validityKey.append(collectionService.getMetadata(collection, "license"));
            validityKey.append(collectionService.getMetadata(collection, "name")); 
            
            // Add the logo also;
            this.add(context, collection.getLogo());
            
        }
        else if (dso instanceof Item)
        {
            Item item = (Item) dso;
            
            validityKey.append("Item:");
            validityKey.append(item.getHandle());            
            validityKey.append(item.getOwningCollection());
            validityKey.append(item.getLastModified());
            // Include all metadata values in the validity key.
            List<MetadataValue> dcvs = itemService.getMetadata(item, Item.ANY, Item.ANY,Item.ANY,Item.ANY);
            for (MetadataValue dcv : dcvs)
            {
                validityKey.append(dcv.getMetadataField().toString('.'));
                validityKey.append(dcv.getLanguage()).append("=");
                validityKey.append(dcv.getValue());
            }
            
            for(Bundle bundle : item.getBundles())
            {
                // Add each of the items bundles & bitstreams.
                this.add(context, bundle);
            }
        }
        else if (dso instanceof Bundle)
        {
            Bundle bundle = (Bundle) dso;
            
            validityKey.append("Bundle:");
            validityKey.append(bundle.getID());
            validityKey.append(bundle.getName());
            validityKey.append(bundle.getPrimaryBitstream() != null ? bundle.getPrimaryBitstream().getID() : "");
            
            for(Bitstream bitstream : bundle.getBitstreams())
            {
                this.add(context, bitstream);
            }
        }
        else if (dso instanceof Bitstream)
        {
            Bitstream bitstream = (Bitstream) dso;
            
            validityKey.append("Bundle:");
            validityKey.append(bitstream.getID());
            validityKey.append(bitstream.getSequenceID());
            validityKey.append(bitstream.getName());
            validityKey.append(bitstream.getSource());
            validityKey.append(bitstream.getDescription());
            validityKey.append(bitstream.getChecksum());
            validityKey.append(bitstream.getChecksumAlgorithm());
            validityKey.append(bitstream.getSizeBytes());
            validityKey.append(bitstream.getUserFormatDescription());
            validityKey.append(bitstream.getFormat(context).getDescription());
        }
        else if (dso instanceof EPerson)
        {
            EPerson eperson = (EPerson) dso;
            
            validityKey.append("Bundle:");
            validityKey.append(eperson.getID());
            validityKey.append(eperson.getEmail());
            validityKey.append(eperson.getNetid());
            validityKey.append(eperson.getFirstName());
            validityKey.append(eperson.getLastName());
            validityKey.append(eperson.canLogIn());
            validityKey.append(eperson.getRequireCertificate());
        }
        else if (dso instanceof Group)
        {
            Group group = (Group) dso;
            
            validityKey.append("Group:");
            
            validityKey.append(group.getID());
            validityKey.append(group.getName());
        }
        else
        {
            throw new IllegalArgumentException("DSpaceObject of type '"+dso.getClass().getName()+"' is not supported by the DSpaceValidity object.");
        }    
    }
    
    /**
     * Add a non-DSpaceObject to the validity, the object should be 
     * serialized into a string form. The order in which objects 
     * are added to the validity object is important, ensure that 
     * objects are added in the *exact* same order each time a 
     * validity object is created.
     *
     * @param nonDSpaceObject
     *          The non-DSpace object to add to the validity.
     * @throws java.sql.SQLException passed through.
     */
    public void add(String nonDSpaceObject) throws SQLException
    {
        validityKey.append("String:");
        validityKey.append(nonDSpaceObject);
    }

    /**
     * This method is used during serialization. When Tomcat is shutdown, Cocoon's in-memory
     * cache is serialized and written to disk to later be read back into memory on start 
     * up. When this class is read back into memory the readObject(stream) method will be 
     * called.
     *
     * This method will re-read the serialization back into memory but it will then set 
     * the assume validity time to zero. This means the next cache hit after a server startup
     * will never be assumed valid. Only after it has been checked once will the regular assume
     * validity mechanism be used.
     * 
     * @param in stream for reading serialized data.
     * @throws IOException passed through.
     * @throws ClassNotFoundException passed through.
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
    	// Read in this object as normal.
    	in.defaultReadObject();
    	
    	// After reading from serialization do not assume validity anymore until it
    	// has been checked at least once.
    	this.assumedValidityTime = 0;
    }
    
    /**
     * Reset the assume validity time. This should be called only when the validity of this cache
     * has been confirmed to be accurate. This will reset the assume valid timer based upon the
     * configured delay.
     */
    private void resetAssumedValidityTime()
    {
    	this.assumedValidityTime = System.currentTimeMillis() + this.assumedValidityDelay;
    }
    
    /**
     * Determine if the cache is still valid
     * @return {@link org.apache.excalibur.source.SourceValidity#VALID},
     *         {@link org.apache.excalibur.source.SourceValidity#UNKNOWN},
     *         or {@link org.apache.excalibur.source.SourceValidity#INVALID}.
     */
    @Override
    public int isValid()
    {
        // Return true if we have a hash.
        if (this.completed)
        {
        	// Check if we are configured to assume validity for a period of time.
        	if (this.assumedValidityDelay > 0)
        	{
        		// Check if we should assume validitity.
        		if (System.currentTimeMillis() < this.assumedValidityTime)
        		{
        			// Assume the cache is valid without rechecking another validity object.
        			return SourceValidity.VALID;
        		}
        	}
        	
        	return SourceValidity.UNKNOWN;
        }
        else
        {
        	// This is an error state. We are being asked whether we are valid before
        	// we have been initialized.
            return SourceValidity.INVALID;
        }
    }

    /**
     * Determine if the cache is still valid based 
     * upon the other validity object.
     * 
     * @param otherObject 
     *          The other validity object.
     * @return {@link org.apache.excalibur.source.SourceValidity#VALID}
     *         or {@link org.apache.excalibur.source.SourceValidity#INVALID}.
     */
    @Override
    public int isValid(SourceValidity otherObject)
    {
        if (this.completed && otherObject instanceof DSpaceValidity)
        {
            DSpaceValidity otherSSV = (DSpaceValidity) otherObject;
            if (hash == otherSSV.hash)
            {
                // Both caches have been checked are are considered valid,
                // now we reset their assumed validity timers for both cache
                // objects.
                if (this.assumedValidityDelay > 0)
                {
                    this.resetAssumedValidityTime();
                }

                if (otherSSV.assumedValidityDelay > 0)
                {
                    otherSSV.resetAssumedValidityTime();
                }

                return SourceValidity.VALID;
            }
        }

        return SourceValidity.INVALID;
    }
}
