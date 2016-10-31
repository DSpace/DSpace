/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

/**
 * Details for a destination.  This is used to represent a destination. If 
 * expressed as a string, the destination looks like: 
 * <pre>
 * &lt;user&gt;[&lt;onBehalfOf&gt;]:&lt;password&gt;@&lt;url&gt;
 * </pre>
 * 
 * @author Neil Taylor
 */
public class PostDestination
{
    /**
     * URL for the post destination. 
     */
    private String url; 
    
    /** 
     * The username. 
     */
    private String username;
    
    /**
     * The password. 
     */
    private String password;
    
    /**
     * The onBehalfOf ID. 
     */
    private String onBehalfOf;
    
    /**
     * Create a new instance. 
     */
    public PostDestination()
    {
       // No-Op
    }
    
    /**
     * Create a new instance. 
     * 
     * @param url          The url. 
     * @param username     The username. 
     * @param password     The password. 
     * @param onBehalfOf   The onBehalfOf id. 
     */
    public PostDestination(String url, String username, String password, String onBehalfOf)
    {
       this.url = url; 
       this.username = username; 
       this.password = password;
       this.onBehalfOf = onBehalfOf;
    }
 
    /**
     * @return the url
     */
    public String getUrl()
    {
       return url;
    }
 
    /**
     * @param url the url to set
     */
    public void setUrl(String url)
    {
       this.url = url;
    }
 
    /**
     * @return the username
     */
    public String getUsername()
    {
       return username;
    }
 
    /**
     * @param username the username to set
     */
    public void setUsername(String username)
    {
       this.username = username;
    }
 
    /**
     * @return the password
     */
    public String getPassword()
    {
       return password;
    }
 
    /**
     * @param password the password to set
     */
    public void setPassword(String password)
    {
       this.password = password;
    }
 
    /**
     * @return the onBehalfOf
     */
    public String getOnBehalfOf()
    {
       return onBehalfOf;
    }
 
    /**
     * @param onBehalfOf the onBehalfOf to set
     */
    public void setOnBehalfOf(String onBehalfOf)
    {
       this.onBehalfOf = onBehalfOf;
    }
    
    /** 
     * Create a string representation of this object. 
     * 
     * @return The string. 
     */
    public String toString()
    {
       StringBuffer buffer = new StringBuffer();
       buffer.append(username);
       if ( onBehalfOf != null )
       {
          buffer.append("[");
          buffer.append(onBehalfOf);
          buffer.append("]");
       }
       
       if ( password != null )
       {
          buffer.append(":******");
       }
       buffer.append("@");
       buffer.append(url);
       
       return buffer.toString(); 
    }
}
