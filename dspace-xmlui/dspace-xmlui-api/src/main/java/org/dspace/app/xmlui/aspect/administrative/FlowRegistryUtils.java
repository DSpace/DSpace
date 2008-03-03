/*
 * FlowRegistryUtils.java
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.utils.RequestUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Utility methods to process actions on either the metadata registry 
 * or format registry.
 * @author scott phillips
 */
public class FlowRegistryUtils 
{

	/** Language Strings */
	private static final Message T_add_metadata_schema_success_notice =
		new Message("default","xmlui.administrative.FlowRegistryUtils.add_metadata_schema_success_notice");
	private static final Message T_delete_metadata_schema_success_notice =
		new Message("default","xmlui.administrative.FlowRegistryUtils.delete_metadata_schema_success_notice");
	private static final Message T_add_metadata_field_success_notice =
		new Message("default","xmlui.administrative.FlowRegistryUtils.add_metadata_field_success_notice");
	private static final Message T_edit_metadata_field_success_notice =
		new Message("default","xmlui.administrative.FlowRegistryUtils.edit_metadata_field_success_notice");
	private static final Message T_move_metadata_field_sucess_notice =
		new Message("default","xmlui.administrative.FlowRegistryUtils.move_metadata_field_success_notice");
	private static final Message T_delete_metadata_field_success_notice =
		new Message("default","xmlui.administrative.FlowRegistryUtils.delete_metadata_field_success_notice");
	private static final Message T_edit_bitstream_format_success_notice =
		new Message("default","xmlui.administrative.FlowRegistryUtils.edit_bitstream_format_success_notice");
	private static final Message T_delete_bitstream_format_success_notice =
		new Message("default","xmlui.administrative.FlowRegistryUtils.delete_bitstream_format_success_notice");

	
	
	/**
	 * Add a new metadata schema. The ID of the new schema will be added
	 * as the "schemaID" parameter on the results object.
	 * 
	 * @param context The DSpace context
	 * @param namespace The new schema's namespace
	 * @param name The new schema's name.
	 * @return A flow result
	 */
	public static FlowResult processAddMetadataSchema(Context context, String namespace, String name) throws SQLException, AuthorizeException, NonUniqueMetadataException, UIException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		// Decode the namespace and name
		try
        {
            namespace = URLDecoder.decode(namespace, Constants.DEFAULT_ENCODING);
            name = URLDecoder.decode(name,Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }
		
		if (namespace == null || 
			namespace.length() <= 0)
			result.addError("namespace");
		if (name == null || 
			name.length() <= 0 ||
			name.indexOf('.') != -1 ||
			name.indexOf('_') != -1 ||
			name.indexOf(' ') != -1)
			// The name must not be empty nor contain dot, underscore or spaces.
			result.addError("name");
		
		
		if (result.getErrors() == null)
		{
			MetadataSchema schema = new MetadataSchema();
		    schema.setNamespace(namespace);
		    schema.setName(name);
		    schema.create(context);

		    context.commit();
		    
		    result.setContinue(true);
		    result.setOutcome(true);
		    result.setMessage(T_add_metadata_schema_success_notice);   
		    result.setParameter("schemaID", schema.getSchemaID());
		}
		
		return result;
	}
	
	/**
	 * Delete the given schemas.
	 * 
	 * @param context The DSpace context
	 * @param schemaIDs A list of schema IDs to be deleted.
	 * @return A flow result
	 */
	public static FlowResult processDeleteMetadataSchemas(Context context, String[] schemaIDs) throws SQLException, AuthorizeException, NonUniqueMetadataException
	{
		FlowResult result = new FlowResult();
		
		int count = 0;
		for (String id : schemaIDs) 
    	{
			MetadataSchema schema = MetadataSchema.find(context, Integer.valueOf(id));
			
			// First remove and fields in the schema
			MetadataField[] fields = MetadataField.findAllInSchema(context, schema.getSchemaID());
			for (MetadataField field : fields)
				field.delete(context);
			
			// Once all the fields are gone, then delete the schema.
	        schema.delete(context);
	        count++;
    	}
		
		if (count > 0)
		{
			context.commit();
			
			result.setContinue(true);
			result.setOutcome(true);
			result.setMessage(T_delete_metadata_schema_success_notice);
		}
        
		return result;
	}
	
	/**
	 * Add a new metadata field. The newly created field's ID will be added as
	 * the "fieldID" parameter on the results object.
	 * 
	 * @param context The DSpace context
	 * @param schemaID The id of the schema where this new field should be added.
	 * @param element The field's element.
	 * @param qualifier The field's qualifier.
	 * @param note A scope not about the field.
	 * @return A results object
	 */
	public static FlowResult processAddMetadataField(Context context, int schemaID, String element, String qualifier, String note) throws IOException, AuthorizeException, SQLException, UIException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		// Decode the element, qualifier, and note.
		try
        {
            element = URLDecoder.decode(element, Constants.DEFAULT_ENCODING);
            qualifier = URLDecoder.decode(qualifier,Constants.DEFAULT_ENCODING);
            note = URLDecoder.decode(note,Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }
		
		// Check if the field name is good.
		result.setErrors(checkMetadataFieldName(element, qualifier));
		
		// Make sure qualifier is null if blank.
		if ("".equals(qualifier))
			qualifier = null;
		
		if (result.getErrors() == null)
		{
			try
			{
				
				MetadataField field = new MetadataField();
				field.setSchemaID(schemaID);
				field.setElement(element);
				field.setQualifier(qualifier);
				field.setScopeNote(note);
				field.create(context);
				
				context.commit();
				
				result.setContinue(true);
				result.setOutcome(true);
				result.setMessage(T_add_metadata_field_success_notice);
				result.setParameter("fieldID", field.getFieldID());
			} 
			catch (NonUniqueMetadataException nume)
			{
				result.addError("duplicate_field");
			}
			
		}
		
		return result;
	}
	
	/**
	 * Edit a metadata field.
	 * 
	 * @param context The DSpace context.
	 * @param schemaID The ID of the schema for this field.
	 * @param fieldID The id of this field.
	 * @param element A new element value
	 * @param qualifier A new qualifier value
	 * @param note A new note value.
	 * @return A results object.
	 */
	public static FlowResult processEditMetadataField(Context context, int schemaID, int fieldID, String element, String qualifier, String note) throws IOException, AuthorizeException, SQLException, UIException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		// Decode the element, qualifier, and note.
		try
        {
            element = URLDecoder.decode(element, Constants.DEFAULT_ENCODING);
            qualifier = URLDecoder.decode(qualifier,Constants.DEFAULT_ENCODING);
            note = URLDecoder.decode(note,Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }
		
		// Check if the field name is good.
		result.setErrors(checkMetadataFieldName(element, qualifier));
		
		// Make sure qualifier is null if blank.
		if ("".equals(qualifier))
			qualifier = null;
		
		// Check to make sure the field is unique, sometimes the NonUniqueMetadataException is not thrown.
		MetadataField possibleDuplicate = MetadataField.findByElement(context, schemaID, element, qualifier);
		if (possibleDuplicate != null && possibleDuplicate.getFieldID() != fieldID)
			result.addError("duplicate_field");
		
		if (result.getErrors() == null)
		{	
			try
			{
				// Update the metadata for a DC type
				MetadataField field = MetadataField.find(context, fieldID);
				field.setElement(element);
				field.setQualifier(qualifier);
				field.setScopeNote(note);
				field.update(context);
				
				context.commit();
				
				result.setContinue(true);
				result.setOutcome(true);
				result.setMessage(T_edit_metadata_field_success_notice);
			} 
			catch (NonUniqueMetadataException nume)
			{
				// This shouldn't ever occure.
				result.addError("duplicate_field");
			}	
		}
		
		return result;
	}
	
	/**
	 * Simple method to check the a metadata field's name: element and qualifier.
	 * 
	 * @param element The field's element.
	 * @param qualifier The field's qualifier
	 * @return A list of errors found, null if none are found.
	 */
	private static List<String> checkMetadataFieldName(String element, String qualifier)
	{
		List<String> errors = new ArrayList<String>();
		
		
		// Is the element empty?
		if (element == null || element.length() <= 0)
		{
			element = ""; // so that the rest of the checks don't fail.
			errors.add("element_empty");
		}
		
		// Is there a bad character in the element?
		if (element.indexOf('.') != -1 ||
			element.indexOf('_') != -1 ||
			element.indexOf(' ') != -1)
			errors.add("element_badchar");
		
		// Is the element too long?
		if (element.length() > 64)
			errors.add("element_tolong");
		

		// The qualifier can be empty.
		if (qualifier != null && qualifier.length() > 0)
		{
			if (qualifier.length() > 64)
				errors.add("qualifier_tolong");
			
			if (qualifier.indexOf('.') != -1 ||
				qualifier.indexOf('_') != -1 ||
				qualifier.indexOf(' ') != -1)
				errors.add("qualifier_badchar");
		}
		
		// If there were no errors then just return null.
		if (errors.size() == 0)
			return null;
		
		return errors;
	}
	
	/**
	 * Move the specified metadata fields to the target schema.
	 * 
	 * @param context The DSpace context
	 * @param schemaID The target schema ID
	 * @param fieldIDs The fields to be moved.
	 * @return A results object.
	 */	
	public static FlowResult processMoveMetadataField(Context context, int schemaID, String[] fieldIDs) throws NumberFormatException, SQLException, AuthorizeException, NonUniqueMetadataException, IOException
	{
		FlowResult result = new FlowResult();

		int count = 0;
		for (String id : fieldIDs) 
		{
			MetadataField field = MetadataField.find(context, Integer.valueOf(id));
			field.setSchemaID(schemaID);
			field.update(context);
			count++;
		}

		if (count > 0)
		{
			context.commit();

			result.setContinue(true);
			result.setOutcome(true);
			result.setMessage(T_move_metadata_field_sucess_notice);
		}

		return result;
	}

	
	/**
	 * Delete the specified metadata fields.
	 * 
	 * @param context The DSpace context
	 * @param fieldIDs The fields to be deleted.
	 * @return A results object
	 */
	public static FlowResult processDeleteMetadataField(Context context, String[] fieldIDs) throws NumberFormatException, SQLException, AuthorizeException
	{
        FlowResult result = new FlowResult();
		
		int count = 0;
		for (String id : fieldIDs) 
    	{
			MetadataField field = MetadataField.find(context, Integer.valueOf(id));
	        field.delete(context);
	        count++;
    	}
		
		if (count > 0)
		{
			context.commit();
			
			result.setContinue(true);
			result.setOutcome(true);
			result.setMessage(T_delete_metadata_field_success_notice);
		}
        
		return result;
	}
	
	
	/**
	 * Edit a bitstream format. If the formatID is -1 then a new format is created.
	 * The formatID of the new format is added as a parameter to the results object.
	 * 
	 * FIXME: the reason we accept a request object is so that we can use the 
	 * RequestUtils.getFieldvalues() to get the multivalue field values.
	 * 
	 * @param context The dspace context
	 * @param formatID The id of the format being updated.
	 * @param request The request object, for all the field entries.
	 * @return A results object
	 */
	public static FlowResult processEditBitstreamFormat(Context context, int formatID, Request request) throws SQLException, AuthorizeException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);
		
		// Get the values
        String mimeType = request.getParameter("mimetype");
        String shortDescription = request.getParameter("short_description");
        String description = request.getParameter("description");
        String supportLevel = request.getParameter("support_level");
        String internal = request.getParameter("internal");
        List<String> extensionsList = RequestUtils.getFieldValues(request, "extensions");
        String[] extensions = extensionsList.toArray(new String[extensionsList.size()]);
		
        // The format must at least have a name.
        if (formatID != 1 && (shortDescription == null || shortDescription.length() == 0))
        {
        	result.addError("short_description");
        	return result;
        }
        
        // Remove leading periods from file extensions.
        for (int i = 0; i < extensions.length; i++)
        	if (extensions[i].startsWith("."))
        		extensions[i] = extensions[i].substring(1);
        
        
        // Get or create the format
        BitstreamFormat format;
		if (formatID >= 0)
			format = BitstreamFormat.find(context, formatID);
		else
			format = BitstreamFormat.create(context);
        
		// Update values
		format.setMIMEType(mimeType);
		if (formatID != 1) // don't change the unknow format.
			format.setShortDescription(shortDescription);
		format.setDescription(description);
		format.setSupportLevel(Integer.valueOf(supportLevel));
		if (internal == null)
			format.setInternal(false);
		else
			format.setInternal(true);
		format.setExtensions(extensions);

		
		// Commit the change
        format.update();
        context.commit();
		
		// Return status
        result.setContinue(true);
		result.setOutcome(true);
		result.setMessage(T_edit_bitstream_format_success_notice);
		result.setParameter("formatID",format.getID());
        
		return result;
	}
	
	/**
	 * Delete the specified bitstream formats.
	 * 
	 * @param context The DSpace context
	 * @param formatIDs The formats-to-be-deleted.
	 * @return A results object.
	 */
	public static FlowResult processDeleteBitstreamFormats(Context context, String[] formatIDs) throws NumberFormatException, SQLException, AuthorizeException
	{
        FlowResult result = new FlowResult();
		
		int count = 0;
		for (String id : formatIDs) 
    	{
			BitstreamFormat format = BitstreamFormat.find(context,Integer.valueOf(id));
			format.delete();
	        count++;
    	}
		
		if (count > 0)
		{
			context.commit();
			
			result.setContinue(true);
			result.setOutcome(true);
			result.setMessage(T_delete_bitstream_format_success_notice);
		}
        
		return result;
	}
	
}
