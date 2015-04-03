/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;

public class MissingLicenseAgreementException extends AuthorizeException {

    /**
     * Create an empty authorize exception
     */
    public MissingLicenseAgreementException()
    {
        super();        
    }

    /**
     * create an exception with only a message
     * 
     * @param message
     */
    public MissingLicenseAgreementException(String message)
    {
        super(message);

    }
    

    /**
     * Create an authorize exception with a message, DSpaceObject and action 
     * 
     * @param message
     *            the message
     */
    public MissingLicenseAgreementException(String message, DSpaceObject o, int a)
    {
        super(message, o, a);
    }
    
}
