package org.datadryad.app.xmlui.aspect.ame;

import java.io.IOException;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;

/**
 * Flow utilities for AME update, add, remove operations.
 * 
 * @author craig.willis@unc.edu
 *
 */
@SuppressWarnings("deprecation")
public class AMEUtils
{

	private static final Message T_metadata_updated = new Message("default", "Metadata value(s) updated");
	private static final Message T_metadata_added = new Message("default", "Metadata value added");
	private static final Message T_metadata_removed= new Message("default", "Metadata value removed");
	
	
	/**
	 * Process the suggested terms form.
	 */
	public static FlowResult processUpdate(Context context, int itemID, Request request) 
		throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Item item = Item.find(context, itemID);
		
		// Clear metadata within the suggest terms scope
		item.clearMetadata("dwc","ScientificName",null,Item.ANY);
		item.clearMetadata("dc","subject",null,Item.ANY);

		// Get the fields/values
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		Enumeration parameters = request.getParameterNames();
		while(parameters.hasMoreElements())
		{
			// Only consider the name_ fields
			String parameterName = (String) parameters.nextElement();
			if (parameterName.startsWith("name_"))
			{
				// Extract the index from the name.
				String indexString = parameterName.substring("name_".length());
				Integer index = Integer.valueOf(indexString);
				indexes.add(index);
			}
		}

		// Add fields in
		for (Integer index=1; index <= indexes.size(); ++index)
		{
			String name = request.getParameter("name_"+index);
			if (name != null)
			{
				String value = request.getParameter("value_"+index);
	            String authority = request.getParameter("value_"+index+"_authority");
	            String confidence = request.getParameter("value_"+index+"_confidence");
	            
				// get the field's name broken up
				String[] parts = parseName(name);
	
	            int iconf = Choices.CF_UNSET;
	            if (confidence != null && confidence.length() > 0)
	                iconf = Choices.getConfidenceValue(confidence);
	
	            if (authority != null && authority.length() > 0 && iconf == Choices.CF_UNSET)
	                iconf = Choices.CF_NOVALUE;
	            item.addMetadata(parts[0], parts[1], parts[2], null,
	                                 value, authority, iconf);
			}
		}

		item.update();
		context.commit();

		result.setContinue(true);

		result.setOutcome(true);
		result.setMessage(T_metadata_updated);

		return result;
	}
	
	public static FlowResult processRemove(Context context, int itemID, Request request) 
		throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Item item = Item.find(context, itemID);

		String name = request.getParameter("remove_field_name");
		String value = request.getParameter("remove_field_value");
		
		String[] parts = parseName(name);
		
		DCValue[] dcValues = item.getMetadata(parts[0], parts[1], null, Item.ANY);
		item.clearMetadata(parts[0], parts[1], null, Item.ANY);
		for (DCValue dc: dcValues)
		{
			if (!dc.value.equals(value))
				item.addMetadata(dc.schema, dc.element, dc.qualifier, dc.language, dc.value, dc.authority, dc.confidence);
			
		}

		item.update();
		context.commit();

		result.setContinue(true);

		result.setOutcome(true);
		result.setMessage(T_metadata_removed);

		return result;
	}
	
	public static FlowResult processAdd(Context context, int itemID, Request request) throws SQLException, AuthorizeException, UIException, IOException
	{
		FlowResult result = new FlowResult();
		result.setContinue(false);

		Item item = Item.find(context, itemID);
		
		DCValue[] subjects = item.getMetadata("dc.subject");
		List<String> existingSubjects = new ArrayList<String>();
		for (DCValue dc: subjects) {
			existingSubjects.add(dc.value);
		}
		
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		Enumeration parameters = request.getParameterNames();
		while(parameters.hasMoreElements())
		{
			// Only consider the name_ fields
			String parameterName = (String) parameters.nextElement();
			if (parameterName.startsWith("add_dc_subject"))
			{
				// Extract the index from the name.
				String indexString = parameterName.substring("add_dc_subject_".length());
				Integer index = Integer.valueOf(indexString);
				indexes.add(index);
			}
		}

		// Add fields in
		for (Integer index=0; index <= indexes.size(); ++index)
		{
			String value = request.getParameter("add_dc_subject_"+index);
			if (value != null)
			{
	            String authority = request.getParameter("add_"+index+"_authority");
	            String confidence = request.getParameter("add_"+index+"_confidence");
	            
	            int iconf = Choices.CF_UNSET;
	            if (confidence != null && confidence.length() > 0)
	                iconf = Choices.getConfidenceValue(confidence);
	
	            if (!existingSubjects.contains(value))
	            	item.addMetadata("dc", "subject", null, "en", value, authority, iconf);
			}
		}

		String value = request.getParameter("dc_subject");
		if (value != null && value.trim().length() > 0) {
			if (!existingSubjects.contains(value))
				item.addMetadata("dc", "subject", null, "en", value);
		}
		
		
		
		// Process Scientific Names
		DCValue[] sciNames = item.getMetadata("dwc.ScientificName");
		List<String> existingSciNames = new ArrayList<String>();
		for (DCValue dc: sciNames) {
			existingSciNames.add(dc.value);
		}
		
		indexes = new ArrayList<Integer>();
		parameters = request.getParameterNames();
		while(parameters.hasMoreElements())
		{
			// Only consider the name_ fields
			String parameterName = (String) parameters.nextElement();
			if (parameterName.startsWith("add_dwc_ScientificName"))
			{
				// Extract the index from the name.
				String indexString = parameterName.substring("add_dwc_ScientificName_".length());
				Integer index = Integer.valueOf(indexString);
				indexes.add(index);
			}
		}

		// Add fields in
		for (Integer index=0; index <= indexes.size(); ++index)
		{
			value = request.getParameter("add_dwc_ScientificName_"+index);
			if (value != null)
			{
	            String authority = request.getParameter("add_"+index+"_authority");
	            String confidence = request.getParameter("add_"+index+"_confidence");
	            
	            int iconf = Choices.CF_UNSET;
	            if (confidence != null && confidence.length() > 0)
	                iconf = Choices.getConfidenceValue(confidence);
	
	            if (!existingSciNames.contains(value))
	            	item.addMetadata("dwc", "ScientificName", null, "en", value, authority, iconf);
			}
		}

		value = request.getParameter("dwc_ScientificName");
		if (value != null && value.trim().length() > 0) {
			if (!existingSciNames.contains(value))
				item.addMetadata("dwc", "ScientificName", null, "en", value);
		}
		
		item.update();
		context.commit();

		result.setContinue(true);

		result.setOutcome(true);
		result.setMessage(T_metadata_added);

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