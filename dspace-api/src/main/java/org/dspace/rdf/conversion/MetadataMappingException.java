/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * 
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.conversion;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class MetadataMappingException extends Exception {
    public MetadataMappingException(String msg)
    {
        super(msg);
    }
    
    public MetadataMappingException(Exception cause)
    {
        super(cause);
    }
    
    public MetadataMappingException(String msg, Exception cause)
    {
        super(msg, cause);
    }
    
}
