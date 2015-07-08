package uk.ac.edina.datashare.ldap;

import uk.ac.edina.datashare.eperson.IUser;

/**
 * Represents a LDAP registered user.
 */
public class User implements IUser
{
    private String firstName = null;
    private String surname = null;
    private String email = null;
    private String phone = null;
    
    /**
     * Get user's email address.
     * @return user's email address.
     */
    public String getEmail()
    {
        return email;
    }
    
    /**
     * Set user's email address.
     * @param email User's email address.
     */
    public void setEmail(String email)
    {
        this.email = email;
    }
    
    /**
     * Get user's first name.
     * @return User's first name.
     */
    public String getFirstName()
    {
        return firstName;
    }
    
    /**
     * Set user's forename. Capitalised first chacter if not already.
     * @param firstName User's forename.
     */
    public void setFirstName(String firstName)
    {
        Character first = firstName.charAt(0);
        
        if(!Character.isTitleCase(first))
        {
            this.firstName = Character.toTitleCase(first) + firstName.substring(1);
        }
        else
        {
            this.firstName = surname;
        }
    }
    
    /**
     * Get user's phone number.
     * @return User's phone number.
     */
    public String getPhone()
    {
        return phone;
    }
    
    /**
     * Set user's phone number.
     * @param phone User's phone number.
     */
    public void setPhone(String phone)
    {
        this.phone = phone;
    }
    
    /**
     * Get user's second name.
     * @return User's second name.
     */
    public String getSurname()
    {
        return surname;
    }
    
    /**
     * Set user's second name.
     * @param surname User's second name.
     */
    public void setSurname(String surname)
    {
        Character first = surname.charAt(0);
           
        if(!Character.isTitleCase(first))
        {
            this.surname = Character.toTitleCase(first) + surname.substring(1);
        }
        else
        {
            this.surname = surname;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.edina.dspace.eperson.IUser#getDSpaceId()
     */
    public int getDSpaceId()
    {
        return -1;
    }

    /*
     * (non-Javadoc)
     * @see org.edina.dspace.eperson.IUser#getExternalId()
     */
    public String getExternalId()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.edina.dspace.eperson.IUser#getInstitution()
     */
    public String getInstitution()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.edina.dspace.eperson.IUser#getIpAddress()
     */
    public String getIpAddress()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.edina.dspace.eperson.IUser#hasAccount()
     */
    public boolean hasAccount()
    {
        return false;
    }
}
