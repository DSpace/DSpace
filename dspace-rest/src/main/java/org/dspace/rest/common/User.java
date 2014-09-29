package org.dspace.rest.common;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class for handle login information for POST request.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
@XmlRootElement(name = "user")
public class User
{

    private String email;

    private String password;

    public User()
    {
    }

    public User(String email, String password)
    {
        this.email = email;
        this.password = password;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

}
