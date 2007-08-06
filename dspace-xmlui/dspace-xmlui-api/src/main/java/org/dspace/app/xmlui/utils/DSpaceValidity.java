/*
 * DSpaceValidity.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/04/27 01:19:52 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.xmlui.utils;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * This is a validity object specificaly implemented for the caching 
 * needs of DSpace, Manakin, and Cocoon.
 * 
 * The basic idea is that each time a DSpace object rendered by a cocoon 
 * component the object and everything about it that makes it unique should 
 * be reflected in the validity object for the component. By following this 
 * principle if the object has been updated externaly then the cache will be
 * invalidated.
 * 
 * This DSpaceValidity object makes this processes easier by abstracting out
 * the processes of determining what is unique about a DSpace object. A class
 * is expected to create a new DSpaceValidity object and add() to it all 
 * DSpaceObjects that are rendered by the component. This validity object will 
 * seralize all those objects to a string, take a hash of the string and compare
 * the hash of the string for any updates.
 * 
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
    
    /** A hash of the validityKey taken after completetion */
    protected long hash;
    
    /** The time when the validity is no longer assumed to be valid */
    protected long assumedValidityTime = 0;
    
    /** The length of time that a cache is assumed to be valid */
    protected long assumedValidityDelay = 0;

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
           this.validityKey.append(initialValidityKey);
    }

    public DSpaceValidity()
    {
        this(null);
    }
    
    /**
     * Complete this validity object. After the completion no more
     * objects may be added to the validity object and the object
     * will return as valid.
     */
    public DSpaceValidity complete() 
    {    
        this.completed = true;
        this.hash = HashUtil.hash(validityKey);
        this.validityKey = null;
        
        // Set the forced validity time.
        if (assumedValidityDelay > 0)
        	resetAssumedValidityTime();
        
        return this;
    }
    
    /**
     * Set the time delay for how long this cache will be assumed 
     * to be valid. When it is assumed valid no other checks will be
     * made to consider it's validity, and once the time has expired 
     * a full validation will occure on the next cache hit. If the 
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
    	this.validityKey.append("AssumedValidityDelay:"+milliseconds);
    }
    
    /**
     * Set the time delay for how long this cache will be assumed to be valid.
     * 
     * This method takes a string which is parsed for the delay time, the string 
     * must be of the following form: "<integer> <scale>" where scale is days,
     * hours, minutes, or seconds.
     * 
     * Examples: "1 day" or "12 hours" or "1 hour" or "30 minutes"
     * 
     * See the setAssumedValidityDelay(long) for more information.
     * 
     * @param delay The delay time in a variable scale.
     */
    public void setAssumedValidityDelay(String delay)
    {
    	if (delay == null || delay.length() == 0)
    		return;
    	
    	String[] parts = delay.split(" ");
    	
    	if (parts == null || parts.length != 2)
    		throw new IllegalArgumentException("Error unable to parse the assumed validity delay time: \""+delay+"\". All delays must be of the form \"<integer> <scale>\" where scale is either seconds, minutes, hours, or days.");

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
    	
    	// Now set the actualy delay.
    	setAssumedValidityDelay(milliseconds);
    }
    
    
    /**
     * Add a DSpace object to the validity. The order inwhich objects 
     * are added to the validity object is important, insure that 
     * objects are added in the *exact* same order each time a 
     * validity object is created.
     * 
     * Below are the following transative rules for adding 
     * objects, i.e. if an item is added then all the items 
     * bundles & bitstreams will also be added.
     * 
     * Communities -> logo bitstream
     * Collection -> logo bitstream
     * Item -> bundles -> bitstream
     * Bundles -> bitstreams
     * EPeople -> groups
     * 
     * @param dso
     *          The object to add to the validity.
     */
    public void add(DSpaceObject dso) throws SQLException
    {
        if (this.completed)
        {
            throw new IllegalStateException("Can not add DSpaceObject to a completed validity object");
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
            validityKey.append(community.getMetadata("introductory_text"));
            validityKey.append(community.getMetadata("short_description"));
            validityKey.append(community.getMetadata("side_bar_text"));
            validityKey.append(community.getMetadata("copyright_text"));
            validityKey.append(community.getMetadata("name"));
            
            // Add the communities logo
            this.add(community.getLogo());

        } 
        else if (dso instanceof Collection)
        {
            Collection collection = (Collection) dso;
            
            validityKey.append("Collection:");
            validityKey.append(collection.getHandle());
            validityKey.append(collection.getMetadata("introductory_text"));
            validityKey.append(collection.getMetadata("short_description"));
            validityKey.append(collection.getMetadata("side_bar_text"));
            validityKey.append(collection.getMetadata("provenance_description"));
            validityKey.append(collection.getMetadata("copyright_text"));
            validityKey.append(collection.getMetadata("license"));
            validityKey.append(collection.getMetadata("name")); 
            
            // Add the logo also;
            this.add(collection.getLogo());
            
        }
        else if (dso instanceof Item)
        {
            Item item = (Item) dso;
            
            validityKey.append("Item:");
            validityKey.append(item.getHandle());            
            // Include all metadata values in the validity key.
            DCValue[] dcvs = item.getDC(Item.ANY,Item.ANY,Item.ANY);
            for (DCValue dcv : dcvs)
            {
                validityKey.append(dcv.schema + ".");
                validityKey.append(dcv.element + ".");
                validityKey.append(dcv.qualifier + ".");
                validityKey.append(dcv.language + "=");
                validityKey.append(dcv.value);
            }
            
            for(Bundle bundle : item.getBundles())
            {
                // Add each of the items bundles & bitstreams.
                this.add(bundle);
            }
        }
        else if (dso instanceof BrowseItem)
        {
        	BrowseItem browseItem = (BrowseItem) dso;
        	
        	validityKey.append("BrowseItem:");
        	validityKey.append(browseItem.getHandle());
        	DCValue[] dcvs = browseItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            for (DCValue dcv : dcvs)
            {
                validityKey.append(dcv.schema + ".");
                validityKey.append(dcv.element + ".");
                validityKey.append(dcv.qualifier + ".");
                validityKey.append(dcv.language + "=");
                validityKey.append(dcv.value);
            }
        }
        else if (dso instanceof Bundle)
        {
            Bundle bundle = (Bundle) dso;
            
            validityKey.append("Bundle:");
            validityKey.append(bundle.getID());
            validityKey.append(bundle.getName());
            validityKey.append(bundle.getPrimaryBitstreamID());
            
            for(Bitstream bitstream : bundle.getBitstreams())
            {
                this.add(bitstream);
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
            validityKey.append(bitstream.getSize());
            validityKey.append(bitstream.getUserFormatDescription());
            validityKey.append(bitstream.getFormat().getDescription());
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
     * Add a non DSpaceObject to the validity, the object should be 
     * seralized into a string form. The order inwhich objects 
     * are added to the validity object is important, insure that 
     * objects are added in the *exact* same order each time a 
     * validity object is created.
     *
     * @param nonDSpaceObject
     *          The non DSpace object to add to the validity.
     */
    public void add(String nonDSpaceObject) throws SQLException
    {
        validityKey.append("String:");
        validityKey.append(nonDSpaceObject);
    }
    
    
    
    
    
    
    
    
    /**
     * This method is used during serializion. When tomcat is shutdown cocoon's in-memory 
     * cache is serialized and written to disk to later be read back into memory on start 
     * up. When this class is read back into memory the readObject(stream) method will be 
     * called.
     *
     * This method will re-read the serialization back into memory but it will then set 
     * the assume validity time to zero. This means the next cache hit after a server startup
     * will never be assumed valid. Only after it has been checked once will the regular assume
     * validity mechanism be used.
     * 
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
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
     */
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
        			// Assume the cache is valid with out rechecking another validity object.
        			return SourceValidity.VALID;
        		}
        	}
        	
        	return SourceValidity.UNKNOWN;
        }
        else
        {
        	// This is an error, state. We are being asked whether we are valid before
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
     */
    public int isValid(SourceValidity otherObject)
    {
        if (this.completed)
        {
            if (otherObject instanceof DSpaceValidity)
            {
                DSpaceValidity otherSSV = (DSpaceValidity) otherObject;
                if (hash == otherSSV.hash)
                {	
                	// Both caches have been checked are are considered valid, 
                	// now we reset their assumed validity timers for both cache
                	// objects.
                	if (this.assumedValidityDelay > 0)
                		this.resetAssumedValidityTime();
                	
                	if (otherSSV.assumedValidityDelay > 0)
                		otherSSV.resetAssumedValidityTime();
                	
                    return SourceValidity.VALID;
                }
            }
        }

        return SourceValidity.INVALID;
    }
}
