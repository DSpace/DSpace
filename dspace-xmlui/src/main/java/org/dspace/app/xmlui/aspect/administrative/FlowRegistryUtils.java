/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.utils.RequestUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
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

	protected static final MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
	protected static final BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
	protected static final MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

	
	
	/**
	 * Add a new metadata schema. The ID of the new schema will be added
	 * as the "schemaID" parameter on the results object.
	 * 
	 * @param context The DSpace context
	 * @param namespace The new schema's namespace
	 * @param name The new schema's name.
	 * @return A flow result
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws org.dspace.content.NonUniqueMetadataException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException on unsupported encoding.
	 */
	public static FlowResult processAddMetadataSchema(Context context, String namespace, String name)
            throws SQLException, AuthorizeException, NonUniqueMetadataException, UIException
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
		
		if (namespace == null || namespace.length() <= 0)
        {
            result.addError("namespace");
        }
		if (name == null || 
			name.length() <= 0 ||
			name.indexOf('.') != -1 ||
			name.indexOf('_') != -1 ||
			name.indexOf(' ') != -1)
        {
            // The name must not be empty nor contain dot, underscore or spaces.
            result.addError("name");
        }
		
		
		if (result.getErrors() == null)
		{
			MetadataSchema schema = metadataSchemaService.create(context, name, namespace);

		    result.setContinue(true);
		    result.setOutcome(true);
		    result.setMessage(T_add_metadata_schema_success_notice);   
		    result.setParameter("schemaID", schema.getID());
		}
		
		return result;
	}
	
	/**
	 * Delete the given schemas.
	 * 
	 * @param context The DSpace context
	 * @param schemaIDs A list of schema IDs to be deleted.
	 * @return A flow result
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws org.dspace.content.NonUniqueMetadataException passed through.
	 */
	public static FlowResult processDeleteMetadataSchemas(Context context, String[] schemaIDs)
            throws SQLException, AuthorizeException, NonUniqueMetadataException
	{
		FlowResult result = new FlowResult();
		
		int count = 0;
		for (String id : schemaIDs) 
    	{
			MetadataSchema schema = metadataSchemaService.find(context, Integer.valueOf(id));
			
			// First remove and fields in the schema
			List<MetadataField> fields = metadataFieldService.findAllInSchema(context, schema);
			for (MetadataField field : fields)
            {
				metadataFieldService.delete(context, field);
            }
			
			// Once all the fields are gone, then delete the schema.
	        metadataSchemaService.delete(context, schema);
	        count++;
    	}
		
		if (count > 0)
		{
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
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException on unsupported encoding.
	 */
	public static FlowResult processAddMetadataField(Context context, int schemaID, String element, String qualifier, String note)
            throws IOException, AuthorizeException, SQLException, UIException
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
        {
            qualifier = null;
        }
		
		if (result.getErrors() == null)
		{
			try
			{

				MetadataSchema schema = metadataSchemaService.find(context, schemaID);
				MetadataField field = metadataFieldService.create(context, schema, element, qualifier, note);

				result.setContinue(true);
				result.setOutcome(true);
				result.setMessage(T_add_metadata_field_success_notice);
				result.setParameter("fieldID", field.getID());
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
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException on unsupported encoding.
	 */
	public static FlowResult processEditMetadataField(Context context, int schemaID, int fieldID, String element, String qualifier, String note)
            throws IOException, AuthorizeException, SQLException, UIException
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
        {
            qualifier = null;
        }
		
		// Check to make sure the field is unique, sometimes the NonUniqueMetadataException is not thrown.
		MetadataField possibleDuplicate = metadataFieldService.findByElement(context, metadataSchemaService.find(context, schemaID), element, qualifier);
		if (possibleDuplicate != null && possibleDuplicate.getID() != fieldID)
        {
            result.addError("duplicate_field");
        }
		
		if (result.getErrors() == null)
		{	
			try
			{
				// Update the metadata for a DC type
				MetadataField field = metadataFieldService.find(context, fieldID);
				field.setElement(element);
				field.setQualifier(qualifier);
				field.setScopeNote(note);
				metadataFieldService.update(context, field);
				
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
        {
            errors.add("element_badchar");
        }
		
		// Is the element too long?
		if (element.length() > 64)
        {
            errors.add("element_tolong");
        }
		

		// The qualifier can be empty.
		if (qualifier != null && qualifier.length() > 0)
		{
			if (qualifier.length() > 64)
            {
                errors.add("qualifier_tolong");
            }
			
			if (qualifier.indexOf('.') != -1 ||
				qualifier.indexOf('_') != -1 ||
				qualifier.indexOf(' ') != -1)
            {
                errors.add("qualifier_badchar");
            }
		}
		
		// If there were no errors then just return null.
		if (errors.size() == 0)
        {
            return null;
        }
		
		return errors;
	}
	
	/**
	 * Move the specified metadata fields to the target schema.
	 * 
	 * @param context The DSpace context
	 * @param schemaID The target schema ID
	 * @param fieldIDs The fields to be moved.
	 * @return A results object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws org.dspace.content.NonUniqueMetadataException passed through.
     * @throws java.io.IOException passed through.
	 */	
	public static FlowResult processMoveMetadataField(Context context, int schemaID, String[] fieldIDs)
            throws NumberFormatException, SQLException, AuthorizeException, NonUniqueMetadataException, IOException
	{
		FlowResult result = new FlowResult();

		int count = 0;
		for (String id : fieldIDs) 
		{
			MetadataField field = metadataFieldService.find(context, Integer.valueOf(id));
			field.setMetadataSchema(metadataSchemaService.find(context, schemaID));
			metadataFieldService.update(context, field);
			count++;
		}

		if (count > 0)
		{
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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static FlowResult processDeleteMetadataField(Context context, String[] fieldIDs)
            throws NumberFormatException, SQLException, AuthorizeException
	{
        FlowResult result = new FlowResult();
		
		int count = 0;
		for (String id : fieldIDs) 
    	{
			MetadataField field = metadataFieldService.find(context, Integer.valueOf(id));
			metadataFieldService.delete(context, field);
	        count++;
    	}
		
		if (count > 0)
		{
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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
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

        // The format must at least have a name.
        if (formatID != 1 && (shortDescription == null || shortDescription.length() == 0))
        {
        	result.addError("short_description");
        	return result;
        }
        
        // Remove leading periods from file extensions.
        for (int i = 0; i < extensionsList.size(); i++)
        {
        	if (extensionsList.get(i).startsWith("."))
            {
				extensionsList.set(i, extensionsList.get(i).substring(1));
            }
        }
        
        
        // Get or create the format
        BitstreamFormat format;
		if (formatID >= 0)
        {
            format = bitstreamFormatService.find(context, formatID);
        }
		else
        {
            format = bitstreamFormatService.create(context);
        }
        
		// Update values
		format.setMIMEType(mimeType);
		if (formatID != 1) // don't change the unknow format.
        {
            format.setShortDescription(context, shortDescription);
        }
		format.setDescription(description);
		format.setSupportLevel(Integer.valueOf(supportLevel));
		if (internal == null)
        {
            format.setInternal(false);
        }
		else
        {
            format.setInternal(true);
        }
		format.setExtensions(extensionsList);

		
		// Commit the change
		bitstreamFormatService.update(context, format);

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
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
	public static FlowResult processDeleteBitstreamFormats(Context context, String[] formatIDs)
            throws NumberFormatException, SQLException, AuthorizeException
	{
        FlowResult result = new FlowResult();
		
		int count = 0;
		for (String id : formatIDs) 
    	{
			BitstreamFormat format = bitstreamFormatService.find(context,Integer.valueOf(id));
			bitstreamFormatService.delete(context, format);
	        count++;
    	}
		
		if (count > 0)
		{
			result.setContinue(true);
			result.setOutcome(true);
			result.setMessage(T_delete_bitstream_format_success_notice);
		}
        
		return result;
	}
	
}
