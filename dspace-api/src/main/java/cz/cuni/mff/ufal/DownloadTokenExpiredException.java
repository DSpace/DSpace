/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;

public class DownloadTokenExpiredException extends AuthorizeException {

    /**
     * Create an empty authorize exception
     */
    public DownloadTokenExpiredException()
    {
        super();        
    }

    /**
     * create an exception with only a message
     * 
     * @param message
     */
    public DownloadTokenExpiredException(String message)
    {
        super(message);

    }
    

    /**
     * Create an authorize exception with a message, DSpaceObject and action 
     * 
     * @param message
     *            the message
     */
    public DownloadTokenExpiredException(String message, DSpaceObject o, int a)
    {
        super(message, o, a);
    }
    
}

