/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.text.filter;

/**
 * Strips decomposed diacritic characters from the supplied string
 * 
 * @author Graham Triggs
 *
 */
public class StripDiacritics implements TextFilter
{
    @Override
    public String filter(String str)
    {
        return str.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    @Override
    public String filter(String str, String lang)
    {
        return str.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

}
