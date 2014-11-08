/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

import org.apache.log4j.Logger;
import org.dspace.text.filter.TextFilter;
import org.dspace.sort.OrderFormatDelegate;

/**
 * Helper class for creating order delegates.
 *
 * To configure the filters create a subclass and, in an object initializer,
 * create an array of classes that implement TextFilter:
 * 
 * class MyLocaleDelegate extends AbstractTextFilterOFD {
 *   {
 *      filters = new TextFilter[] { new LocaleOrderingFilter(); }
 *   }
 * }
 * 
 * The order they are in the array, is the order that they are executed.
 * (this may be important for some filters - read their documentation!)
 * 
 * Example configurations that could be used:
 * { new DecomposeDiactritics(), new StripDiacritics(), new LowerCaseAndTrim() }
 *    - Decompose and then strip the diacritics, lowercase and trim the string.
 *    
 * { new MARC21InitialArticleWord(), new DecomposeDiactritics(), new LowerCaseTrim() }
 *    - Parse the initial article words based on the Library of Congress list of
 *      definite/indefinite article words, decompose diacritics, and lowercase/trim.
 *
 * { new LowerCaseTrim(), new LocaleOrderingFilter() }
 *    - Lowercase the string, then make a locale dependent sort text
 *      (note that the sort text is not human readable)
 * 
 * @author Graham Triggs
 */
public abstract class AbstractTextFilterOFD implements OrderFormatDelegate
{
	private static final Logger log = Logger.getLogger(AbstractTextFilterOFD.class);
	
	// Initialised in subclass in an object initializer
	protected TextFilter[] filters;

	/**
	 * Prepare the appropriate sort string for the given value in the
	 * given language.  Language should be supplied with the ISO-6390-1
	 * or ISO-639-2 standards.  For example "en" or "eng".
	 * 
	 * @param	value	the string value
	 * @param	language	the language to interpret in
	 */
	@Override
	public String makeSortString(String value, String language)
	{
		if (filters == null)
		{
			// Log an error if the class is not configured correctly
			log.error("No filters defined for " + this.getClass().getName());
		}
		else
		{
			// Normalize language into a two or three character code
	        if (language != null)
	        {
	            if (language.length() > 2 && language.charAt(2) == '_')
                {
                    language = language.substring(0, 2);
                }

	            if (language.length() > 3)
                {
                    language = language.substring(0, 3);
                }
	        }

	        // Iterate through filters, applying each in turn
	        for (int idx = 0; idx < filters.length; idx++)
	        {
	            if (language != null)
                {
                    value = filters[idx].filter(value, language);
                }
	            else
                {
                    value = filters[idx].filter(value);
                }
	        }
		}
		
        return value;
    }
}
