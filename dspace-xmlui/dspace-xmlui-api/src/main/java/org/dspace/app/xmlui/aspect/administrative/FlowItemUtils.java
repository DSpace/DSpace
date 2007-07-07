/*
 * FlowItemUtils.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
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
 */package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.servlet.multipart.Part;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * Utility methods to processes actions on Groups. These methods are used
 * exclusivly from the administrative flow scripts.
 * 
 * @author Jay Paz
 * @author Scott Phillips
 */
public class FlowItemUtils 
{

	/** Language Strings */
	private static final Message T_metadata_updated = new Message("default","The Item's metadata was successfully updated.");
	private static final Message T_metadata_added = new Message("default","New metadata was added.");
	private static final Message T_item_withdrawn = new Message("default","The item has been withdrawn.");
	private static final Message T_item_reinstated = new Message("default","The item has been reinstated.");
	private static final Message T_bitstream_added = new Message("default","The new bitstream was successfully uploaded.");
	private static final Message T_bitstream_failed = new Message("default","Error while uploading file.");
	private static final Message T_bitstream_updated = new Message("default","The bitstream has been updated.");
	private static final Message T_bitstream_delete = new Message("default","The selected bitstreams have been deleted.");
	
	
	/**
	 * Resolve the given identifier to an item. The identifier may be either an
	 * internal ID or a handle. If an item is found then the result the internal
	 * ID of the item will be placed in the result "itemID" parameter.
	 * 
	 * If the identifier was unable to be resolved to an item then the "identifier"
	 * field is placed in error.
	 * 
	 * @param context The current DSpace context.
	 * @param identifier An Internal ID or a handle
	 * @return A flow result
	 */
	public static FlowResult resolveItemIdentifier(Context context, String identifier) throws SQLException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		//		Check whether it's a handle or internal id (by check ing if it has a slash inthe string)
		if (identifier.contains("/")) 
		{
			DSpaceObject dso = HandleManager.resolveToObject(context, identifier);
	
			if (dso != null && dso.getType() == Constants.ITEM) 
			{ 
				result.setParameter("itemID", dso.getID());
				result.setParameter("type", Constants.ITEM);
				result.setContinue(true);
				return result;
			}
		}
		else
		{
		
			Item item = null;
			try {
				item = Item.find(context, Integer.valueOf(identifier));
			} catch (NumberFormatException e) {
				// ignoring the exception
			}

			if (item != null) 
			{
				result.setParameter("itemID", item.getID());
				result.setParameter("type", Constants.ITEM);
				result.setContinue(true);
				return result;
			}
		}

		result.addError("identifier");
		return result;
	}
	
	/**
	 * Process the request parameters to update the item's metadata and remove any selected bitstreams.
	 * 
	 * Each metadata entry will have three fields "name_X", "value_X", and "language_X" where X is an
	 * integer that relates all three of the fields together. The name parameter stores the metadata name 
	 * that is used by the entry (i.e schema_element_qualifier). The value and language paramaters are user
	 * inputed fields. If the optional parameter "remove_X" is given then the metadata value is removed.
	 * 
	 * To support AJAX operations on this page an aditional parameter is considered, the "scope". The scope
	 * is the set of metadata entries that are being updated during this request. It the metadata name, 
	 * schema_element_qualifier, only fields that have this name are considered! If all fields are to be
	 * considered then scope should be set to "*". 
	 * 
	 * When creating an AJAX query include all the name_X, value_X, language_X, and remove_X for the fields
	 * in the set, and then set the scope parameter to be the metadata field.
	 * 
	 * @param context The current DSpace context
	 * @param itemID  internal item id
	 * @param request the Cocoon request
	 * @return A flow result
	 */
	public static FlowResult processEditItem(Context context, int itemID, Request request) throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Item item = Item.find(context, itemID);
		
		
		// STEP 1:
		// Clear all metadata within the scope
		// Only metadata values within this scope will be considered. This
		// is so ajax request can operate on only a subset of the values.
		String scope = request.getParameter("scope");
		if ("*".equals(scope))
		{
			item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
		}
		else
		{
			String[] parts = parseName(scope);
			item.clearMetadata(parts[0],parts[1],parts[2],Item.ANY);
		}
		
		// STEP 2:
		// First determine all the metadata fields that are within
		// the scope parameter
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		Enumeration parameters = request.getParameterNames();
		while(parameters.hasMoreElements())
		{

			// Only consider the name_ fields
			String parameterName = (String) parameters.nextElement();
			if (parameterName.startsWith("name_"))
			{
				// Check if the name is within the scope
				String parameterValue = request.getParameter(parameterName);
				if ("*".equals(scope) || scope.equals(parameterValue))
				{
					// Extract the index from the name.
					String indexString = parameterName.substring("name_".length());
					Integer index = Integer.valueOf(indexString);
					indexes.add(index);
				}
			}
		}
			
		
		// STEP 3:
		// Iterate over all the indexes within the scope and add them back in.
		for (Integer index : indexes)
		{
			String name = request.getParameter("name_"+index);
			String value = request.getParameter("value_"+index);
			String lang = request.getParameter("language_"+index);
			String remove = request.getParameter("remove_"+index);
			
			// the user selected the remove checkbox.
			if (remove != null)
				continue;
			
			// get the field's name broken up
			String[] parts = parseName(name);
			
			// Add the metadata back in.
			item.addMetadata(parts[0], parts[1], parts[2], lang, value);	
		}
		
		item.update();
		context.commit();
		
		result.setContinue(true);
		
		result.setOutcome(true);
		result.setMessage(T_metadata_updated);
		
		return result;
	}
	
	/**
	 * Process the request paramaters to add a new metadata entry for the item.
	 * 
	 * @param context The current DSpace context
	 * @param itemID  internal item id
	 * @param request the Cocoon request
	 * @return A flow result
	 */
	public static FlowResult processAddMetadata(Context context, int itemID, Request request) throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Item item = Item.find(context, itemID);
		
		
		String fieldID = request.getParameter("field");
		String value = request.getParameter("value");
		String language = request.getParameter("language");
		
		MetadataField field = MetadataField.find(context,Integer.valueOf(fieldID));
		MetadataSchema schema = MetadataSchema.find(context,field.getSchemaID());
		
		item.addMetadata(schema.getName(), field.getElement(), field.getQualifier(), language, value);
		
		item.update();
		context.commit();
		
		result.setContinue(true);
		
		result.setOutcome(true);
		result.setMessage(T_metadata_added);
		
		return result;
	}


	/**
	 * Withdraw the specified item, this method assumes that the action has been confirmed.
	 * 
	 * @param context The DSpace context
	 * @param itemID The id of the to-be-withdrawn item.
	 * @return A result object
	 */
	public static FlowResult processWithdrawItem(Context context, int itemID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Item item = Item.find(context, itemID);
		item.withdraw();
		context.commit();

		result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(T_item_withdrawn);
        
		return result;
	}
	
	
	/**
	 * Reinstate the specified item, this method assumes that the action has been confirmed.
	 * 
	 * @param context The DSpace context
	 * @param itemID The id of the to-be-reinstated item.
	 * @return A result object
	 */
	public static FlowResult processReinstateItem(Context context, int itemID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Item item = Item.find(context, itemID);
		item.reinstate();
		context.commit();

		result.setContinue(true);
        result.setOutcome(true);
        result.setMessage(T_item_reinstated);
        
		return result;
	}
	
	
	/**
	 * Permanently delete the specified item, this method assumes that
	 * the action has been confirmed.
	 * 
	 * @param context The DSpace context
	 * @param itemID The id of the to-be-deleted item.
	 * @return A result object
	 */
	public static FlowResult processDeleteItem(Context context, int itemID) throws SQLException, AuthorizeException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Item item = Item.find(context, itemID);
			
        Collection[] collections = item.getCollections();

        // Remove item from all the collections it's in
        for (Collection collection : collections)
        {
            collection.removeItem(item);
        }	
        
        // Note: when removing an item from the last collection it will
        // be removed from the system. So there is no need to also call
        // an item.delete() method.        
        
        context.commit();
		
        result.setContinue(true);
        
		return result;
	}
	
	
	/**
	 * Add a new bitstream to the item. The bundle, bitstream (aka file), and description 
	 * will be used to create a new bitstream. If the format needs to be adjusted then they 
	 * will need to access the edit bitstream form after it has been uploaded.
	 * 
	 * @param context The DSpace content
	 * @param itemID The item to add a new bitstream too
	 * @param request The request.
	 * @return A flow result
	 */
	public static FlowResult processAddBitstream(Context context, int itemID, Request request) throws SQLException, AuthorizeException, IOException 
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		// Upload a new file
		Item item = Item.find(context, itemID);
		
		
		Object object = request.get("file");
		Part filePart = null;
		if (object instanceof Part)
			filePart = (Part) object;

		if (filePart != null && filePart.getSize() > 0)
		{
			InputStream is = filePart.getInputStream();

			String bundleName = request.getParameter("bundle");
			
			Bitstream bitstream;
			Bundle[] bundles = item.getBundles(bundleName);
			if (bundles.length < 1)
			{
				// set bundle's name to ORIGINAL
				bitstream = item.createSingleBitstream(is, bundleName);
			}
			else
			{
				// we have a bundle already, just add bitstream
				bitstream = bundles[0].createBitstream(is);
			}

			// Strip all but the last filename. It would be nice
			// to know which OS the file came from.
			String name = filePart.getUploadName();

			while (name.indexOf('/') > -1)
			{
				name = name.substring(name.indexOf('/') + 1);
			}

			while (name.indexOf('\\') > -1)
			{
				name = name.substring(name.indexOf('\\') + 1);
			}

			bitstream.setName(name);
			bitstream.setSource(filePart.getUploadName());
			bitstream.setDescription(request.getParameter("description"));

			// Identify the format
			BitstreamFormat format = FormatIdentifier.guessFormat(context, bitstream);
			bitstream.setFormat(format);

			// Update to DB
			bitstream.update();
			item.update();
			
			result.setContinue(true);
	        result.setOutcome(true);
	        result.setMessage(T_bitstream_added); 
		}
		else
		{
			result.setContinue(false);
	        result.setOutcome(false);
	        result.setMessage(T_bitstream_failed); 
		}
		return result;
	}
	
	
	/**
	 * Update a bitstream's metadata.
	 * 
	 * @param context The DSpace content
	 * @param itemID The item to which the bitstream belongs
	 * @param bitstreamID The bitstream being updated.
	 * @param description The new description of the bitstream
	 * @param formatID The new format ID of the bitstream
	 * @param userFormat Any user supplied formats.
	 * @return A flow result object.
	 */
	public static FlowResult processEditBitstream(Context context, int itemID, int bitstreamID, String primary, String description, int formatID, String userFormat) throws SQLException, AuthorizeException 
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Bitstream bitstream = Bitstream.find(context, bitstreamID);
		BitstreamFormat currentFormat = bitstream.getFormat();

		//Step 1:
		// Update the bitstream's description
		if (description != null && description.length() > 0)
		{
			bitstream.setDescription(description);
		}
		
		//Step 2:
		// Check if the primary bitstream status has changed
		Bundle[] bundles = bitstream.getBundles();
		if (bundles != null && bundles.length > 0)
		{
			if (bitstreamID == bundles[0].getPrimaryBitstreamID())
			{
				// currently the bitstream is primary
				if ("no".equals(primary))
				{
					// However the user has removed this bitstream as a primary bitstream.
					bundles[0].unsetPrimaryBitstreamID();
					bundles[0].update();
				}
			}
			else
			{
				// currently the bitstream is non-primary
				if ("yes".equals(primary))
				{
					// However the user has set this bitstream as primary.
					bundles[0].setPrimaryBitstreamID(bitstreamID);
					bundles[0].update();
				}
			}
		}
		
		
		//Step 2:
		// Update the bitstream's format
		if (formatID > 0)
		{
			if (currentFormat == null || currentFormat.getID() != formatID)
			{
				BitstreamFormat newFormat = BitstreamFormat.find(context, formatID);
				if (newFormat != null)
				{
					bitstream.setFormat(newFormat);
				}
			}
		}
		else
		{
			if (userFormat != null && userFormat.length() > 0)
			{
				bitstream.setUserFormatDescription(userFormat);
			}
		}
		
		//Step 3:
		// Save our changes
		bitstream.update();
		context.commit();
		
		 result.setContinue(true);
	     result.setOutcome(true);
	     result.setMessage(T_bitstream_updated);
	        
		
		return result;
	}
	
	/**
	 * Delete the given bitstreams from the bundle and item. If there are no more bitstreams 
	 * left in a bundle then also remove it.
	 * 
	 * @param context Current dspace content
	 * @param itemID The item id from which to remove bitstreams
	 * @param bitstreamIDs A bundle slash bitstream id pair of bitstreams to be removed.
	 * @return A flow result
	 */
	public static FlowResult processDeleteBitstreams(Context context, int itemID, String[] bitstreamIDs) throws SQLException, AuthorizeException, IOException, UIException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		Item item = Item.find(context, itemID);
		
		for (String id : bitstreamIDs)
		{
			String[] parts = id.split("/");
			
			if (parts.length != 2)
				throw new UIException("Unable to parse id into bundle and bitstream id: "+id);
			
			int bundleID = Integer.valueOf(parts[0]);
			int bitstreamID = Integer.valueOf(parts[1]);
			
			Bundle bundle = Bundle.find(context, bundleID);
			Bitstream bitstream = Bitstream.find(context,bitstreamID);
			
			bundle.removeBitstream(bitstream);
			
			if (bundle.getBitstreams().length == 0)
			{
				item.removeBundle(bundle);
			}
		}
		
		item.update();
		
		result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(T_bitstream_delete);
		
		return result;
	}
	
	
	/**
	 * Parse the given name into three parts, divided by an _. Each part should represent the 
	 * schema, element, and qualifier. You are guaranteed that if no qualifier was supplied the 
	 * third entry is null.
	 * 
	 * @param name The name to be parsed.
	 * @return An array of name parts.
	 */
	private static String[] parseName(String name) throws UIException
	{
		String[] parts = new String[3];
		
		String[] split = name.split("_");
		if (split.length == 2) {
			parts[0] = split[0];
			parts[1] = split[1];
			parts[2] = null;
		} else if (split.length == 3) {
			parts[0] = split[0];
			parts[1] = split[1];
			parts[2] = split[2];
		} else {
			throw new UIException("Unable to parse metedata field name: "+name);
		}
		return parts;
	}
}
