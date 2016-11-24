/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

/**
 * Represents a generic SWORD exception. If this thrown by a repository,
 * it would result in a SWORD Error Document being thrown.
 * 
 * @author Stuart Lewis
 */
public class SWORDErrorException extends Exception 
{
    /** The error URI (defined in ErrorCodes class) */
    private String errorURI;
    
    /** The HTTP error code (defined in HTTPServletResponse class) */
    private int status; 
    
    /** The error message given by the repository */
    private String description;
    
    
    /**
     * Create a new instance and store the specified data. 
     * 
     * @param errorURI The errorURI of the exception being thrown 
     * @param description  A description of the error thrown.
     */
    public SWORDErrorException(String errorURI, String description)
    {  
       super(description); 
       this.errorURI = errorURI;
       this.description = description;
       
       if (errorURI.equals(ErrorCodes.ERROR_BAD_REQUEST)) { status = 400; }
       else if (errorURI.equals(ErrorCodes.ERROR_CHECKSUM_MISMATCH)) { status = 412; }
       else if (errorURI.equals(ErrorCodes.ERROR_CONTENT)) { status = 415; }
       else if (errorURI.equals(ErrorCodes.MAX_UPLOAD_SIZE_EXCEEDED)) { status = 413; }
       else if (errorURI.equals(ErrorCodes.MEDIATION_NOT_ALLOWED)) { status = 412; }
       else if (errorURI.equals(ErrorCodes.TARGET_OWNER_UKNOWN)) { status = 401; }
       else { status = 400; }
    }


    /**
     * @return the errorURI
     */
    public String getErrorURI() {
        return errorURI;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }
    
    /**
     * Set the status
     * 
     * @param status The HTTP status code
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }    
}
