/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

/**
 * Interface for browse order delegates
 * 
 * @author Graham Triggs
 */
public interface OrderFormatDelegate
{
	/**
	 * Prepare the appropriate sort string for the given value in the
	 * given language.  Language should be supplied with the ISO-6390-1
	 * or ISO-639-2 standards.  For example "en" or "eng".
	 * 
	 * @param	value	the string value
	 * @param	language	the language to interpret in
	 */
    public String makeSortString(String value, String language);
}
