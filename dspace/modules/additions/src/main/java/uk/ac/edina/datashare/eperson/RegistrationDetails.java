package uk.ac.edina.datashare.eperson;

/**
 * Registration details required for EASE authtication and DSpace
 */
public class RegistrationDetails
{
    /** user's email address */
    private String email = null;
    
    /** user's university user name */
    private String uun = null;
    
    /**
     * @param email User's email address
     * @param uun User's university user name
     */
    public RegistrationDetails(String email, String uun)
    {
        this.email = email;
        this.uun = uun;
    }

    /**
     * Get the registered user's email address.
     * @return The user's email address.
     */
    public String getEmail()
    {
        return this.email;
    }

    /**
     * Get the registered user's university user name.
     * @return user's university user name
     */
    public String getUun()
    {
        return this.uun;
    }
}
