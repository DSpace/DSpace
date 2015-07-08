package uk.ac.edina.datashare.eperson;

/**
 * DSpace user.
 */
public interface IUser
{
    /**
     * @return User's first name.
     */
    String getFirstName();
    
    /**
     * @return User's surname.
     */
    String getSurname();
    
    /**
     * @return User's institution name, if appropriate.
     */
    String getInstitution();
    
    /**
     * @return User's IP address.
     */
    String getIpAddress();
    
    /**
     * @return User's external system id.
     */
    String getExternalId();
    
    /**
     * @return DSpace's EPerson id.
     */
    int getDSpaceId();
    
    /**
     * @return Does this user have an account?
     */
    boolean hasAccount();
}
