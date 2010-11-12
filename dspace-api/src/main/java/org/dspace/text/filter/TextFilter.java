/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.text.filter;

/**
 * Define an interface for all browse ordering filters.
 * @author Graham Triggs
 */
public interface TextFilter
{
	public String filter(String str);
    
    public String filter(String str, String lang);
}
