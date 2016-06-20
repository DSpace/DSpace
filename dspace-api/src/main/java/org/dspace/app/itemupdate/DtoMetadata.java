/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.text.ParseException;
import org.dspace.content.Item;

/**
 *   A data transfer object class enhancement of org.dspace.content.DCValue, which is deprecated
 *   Name intended to not conflict with DSpace API classes for similar concepts but not usable in this context
 *   
 *   Adds some utility methods
 *   
 *   Really not at all general enough but supports Dublin Core and the compound form notation {@code <schema>.<element>[.<qualifier>]}
 *   
 *   Does not support wildcard for qualifier
 * 
 *
 */
class DtoMetadata 
{
	final String schema;   
	final String element;
	final String qualifier;
	final String language;
	final String value;
	
	protected DtoMetadata(String schema, String element, String qualifier, String language, String value)
	{
		this.schema = schema;
		this.element = element;
		this.qualifier = qualifier;
		this.language = language;
		this.value = value;
	}
	
	/**
	 *  Factory method
	 *  
	 *  
	 * @param schema     not null, not empty -  'dc' is the standard case
	 * @param element    not null, not empty
	 * @param qualifier  null; don't allow empty string or * indicating 'any'
	 * @param language   null or empty
	 * @param value      value
	 * @return DtoMetadata object
         * @throws IllegalArgumentException if arg error
	 */
	public static DtoMetadata create(String schema,
			    String element,
			    String qualifier,
			    String language,
			    String value)
	throws IllegalArgumentException
    {
		if ((qualifier != null) &&  (qualifier.equals(Item.ANY) || qualifier.equals("")))
		{
			throw new IllegalArgumentException("Invalid qualifier: " + qualifier);
		}
		return new DtoMetadata(schema, element, qualifier, language, value);
    }

	/**
	 *   Factory method to create metadata object
	 *   
	 * 
	 * @param compoundForm   of the form <schema>.<element>[.<qualifier>]
	 * @param language   null or empty
	 * @param value      value
         * @throws ParseException if parse error
         * @throws IllegalArgumentException if arg error
	 */
	public static DtoMetadata create(String compoundForm, String language, String value)
	throws ParseException, IllegalArgumentException  
    {
		String[] ar = MetadataUtilities.parseCompoundForm(compoundForm);
		
		String qual = null;
		if (ar.length > 2)
		{
    		qual = ar[2];
		}
		
		return create(ar[0], ar[1], qual, language, value);
    }

	/**
	 *   Determine if this metadata field matches the specified type:
	 *    schema.element or schema.element.qualifier
	 *    
	 * 
	 * @param compoundForm   of the form <schema>.<element>[.<qualifier>|.*]
	 * @param wildcard       allow wildcards in compoundForm param
	 * @return whether matches
	 */
	public boolean matches(String compoundForm, boolean wildcard)
	{
		String[] ar = compoundForm.split("\\s*\\.\\s*"); //MetadataUtilities.parseCompoundForm(compoundForm);
		
		if ((ar.length < 2) || (ar.length > 3))
		{
			return false;
		}
		
		if (!this.schema.equals(ar[0]) || !this.element.equals(ar[1]))
		{
			return false;
		}
		
		if (ar.length == 2)
		{
			if (this.qualifier != null)
			{
				return false;
			}
		}
		
		if (ar.length == 3)	
		{
			if (this.qualifier == null)
			{
				return false;
			}
			if (wildcard && ar[2].equals(Item.ANY))
			{
				return true;
			}
			if (!this.qualifier.equals(ar[2]))
			{
				return false;
			}
		}
		return true;		
	}
	
	public String toString()
	{
		String s =  "\tSchema: " + schema + " Element: " + element;
		if (qualifier != null)
		{
			s+= " Qualifier: " + qualifier;
		}
		s+= " Language: " + ((language == null) ? "[null]" : language);
        s += " Value: " + value;
        
        return s;
	}
	
	public String getValue()
	{
		return value;
	}
		
}
