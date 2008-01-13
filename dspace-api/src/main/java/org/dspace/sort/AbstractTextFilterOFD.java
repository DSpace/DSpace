/*
 * AbstractTextFilterOFD.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2007/03/02 11:22:13 $
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
	private final static Logger log = Logger.getLogger(AbstractTextFilterOFD.class);
	
	// Initialised in subclass in an object initializer
	protected TextFilter[] filters;

	/**
	 * Prepare the appropriate sort string for the given value in the
	 * given language.  Languate should be supplied with the ISO-6390-1
	 * or ISO-639-2 standards.  For example "en" or "eng".
	 * 
	 * @param	value	the string value
	 * @param	language	the language to interpret in
	 */
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
	                language = language.substring(0,2);

	            if (language.length() > 3)
	                language = language.substring(0,3);
	        }

	        // Iterate through filters, applying each in turn
	        for (int idx = 0; idx < filters.length; idx++)
	        {
	            if (language != null)
	                value = filters[idx].filter(value, language);
	            else
	                value = filters[idx].filter(value);
	        }
		}
		
        return value;
    }
}
