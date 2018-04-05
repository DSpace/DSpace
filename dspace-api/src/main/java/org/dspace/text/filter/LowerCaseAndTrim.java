/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.text.filter;

/**
 * Lowercase and trim leading / trailing whitespace
 * 
 * @author Graham Triggs
 */
public class LowerCaseAndTrim implements TextFilter
{

    @Override
    public String filter(String str)
    {
        return str.toLowerCase().trim();
    }

    @Override
    public String filter(String str, String lang)
    {
        return str.toLowerCase().trim();
    }

}
