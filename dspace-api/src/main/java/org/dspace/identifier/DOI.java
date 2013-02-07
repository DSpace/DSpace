/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

/**
 * DOI identifiers.
 *
 * @author Mark H. Wood
 */
public class DOI
        implements Identifier
{
    public static final String SCHEME = "doi:";
    
    
    /**
     * This method helps to convert a DOI into a URL. It takes DOIs in one of
     * the following formats  and returns it as URL (f.e. 
     * http://dx.doi.org/10.123/456). Allowed formats are:
     * <ul>
     *   <li>doi:10.123/456</li>
     *   <li>10.123/456</li>
     *   <li>http://dx.doi.org/10.123/456</li>
     * </ul>
     * 
     * @param identifier  A DOI that should be returned in external form.
     * @return A String containing a URL to the official DOI resolver.
     * @throws IllegalArgumentException If identifier is null or an empty String.
     * @throws IdentifierException If identifier could not be recognized as valid DOI.
     */
    public static String DOIToExternalForm(String identifier)
            throws IdentifierException
    {
        if (null == identifier) 
            throw new IllegalArgumentException("Identifier is null.", new NullPointerException());
        if (identifier.isEmpty())
            throw new IllegalArgumentException("Cannot format an empty identifier.");
        if (identifier.startsWith(SCHEME))
            return "http://dx.doi.org/" + identifier.substring(SCHEME.length());
        if (identifier.startsWith("10.") && identifier.contains("/"))
            return "http://dx.doi.org/" + identifier;
        if (identifier.startsWith("http://dx.doi.org/10."))
            return identifier;
        
        throw new IdentifierException(identifier + "does not seem to be a DOI.");
    }
}
