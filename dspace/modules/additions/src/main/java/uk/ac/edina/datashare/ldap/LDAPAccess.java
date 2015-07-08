package uk.ac.edina.datashare.ldap;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;

/**
 * Access to LDAP directory at edinburgh university.
 */
public class LDAPAccess
{  
    /** log4j log */
    private static Logger log = Logger.getLogger(LDAPAccess.class);
    
    private static LDAPAccess ldapAccess = null;
    private SearchControls constraints = null;
    
    // staff LDAP context
    private DirContext staffContext = null;
    
    // student LDAP context
    private DirContext studentContext = null;
    
    private static final String CONTEXT_NAME  = "dc=ed,dc=ac,dc=uk";
    private static final String EMAIL_ADDRESS = "mail";
    private static final String COMMON_NAME   = "cn";
    private static final String TELEPHONE_NO  = "telephoneNumber";
   
    /**
     * Singleton LDAPAccess instance.
     * @return LDAPAccess instance.
     */
    public static synchronized LDAPAccess instance()
    {
        if(ldapAccess == null)
        {
            ldapAccess = new LDAPAccess();
        }
        
        return ldapAccess;
    }
    
    /**
     * Initialise LDAP directory access. Private access to prevent
     * instantiation.
     */
    private LDAPAccess()
    {      
        //  Set up the environment for creating the initial context
        Properties props = new Properties();
        
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.setProperty(Context.PROVIDER_URL, "ldap://eddir.ed.ac.uk:389");
        props.setProperty(Context.REFERRAL, "ignore");
        props.setProperty(Context.SECURITY_AUTHENTICATION, "simple");

        try
        {
            // create staff context
            this.staffContext = new InitialDirContext(props);
            
            // create student context
            props.setProperty(Context.PROVIDER_URL, "ldap://smsdir.ed.ac.uk:389");
            this.studentContext = new InitialDirContext(props);
            
            this.constraints = new SearchControls();
            this.constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            this.constraints.setReturningAttributes(null);
        }
        catch(NamingException ex)
        {
            log.error("Problem setting up LDAP connection");
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Get a user's email address for a university user name. If more than one
     * LDAP entry is found, the first email is returned.
     * @param uun University user name.
     * @return User's email address.
     */
    public String getEmailAddressForUun(String uun)
    {   
        // perform search
        User user = getResult("mailbox=" + uun + "@staffmail");

        return user.getEmail();
    }
    
    /**
     * Get details of staff or student using UUN.
     * @param uun University User Name.
     * @return User details.
     */
    public User getUserDetailsForUun(String uun)
    {
        // first try staff context
        User user = getResult("mailbox=" + uun + "@staffmail");
        
        if(user == null)
        {
            // now try student context with @sms
            user = getResult("mailbox=" + uun + "@sms", this.studentContext);
          
            if(user == null)
            {
                // still no entry, try @education
                user = getResult("mailbox=" + uun + "@education", this.studentContext);
            }
        }
        
        if(user != null)             
        { 
            if(user.getEmail() == null ||
               user.getFirstName() == null ||
               user.getSurname() == null)
            {
                // make sure all manditory values have
                // been populated, if not return null
                user = null;
            }
        }
        
        return user;
    }
    
    /**
     * Get user data for a given email address. If more than one LDAP entry is
     * found, the first email is returned.
     * @param email The email address to search for.
     * @return User data for a given email address.
     */
    public User getUserDetailsForEmail(String email)
    {
        return getResult("mail=" + email);
    }
    
    /**
     * Get first LDAP entry found for the given search string for a staff
     * member.
     * @param searchStr LDAP search string (e.g mail=some@email.add)
     * @return The LDAP user entry.
     */
    private User getResult(String searchStr)
    {
        return getResult(searchStr, this.staffContext);
    }
    
    /**
     * Get first LDAP entry found for the given search string.
     * @param searchStr LDAP search string (e.g mail=some@email.add)
     * @param context LDAP conext - for example staff or student.
     * @return The LDAP user entry.
     */
    @SuppressWarnings({ "rawtypes" })
    private User getResult(String searchStr, DirContext context)
    {
        User user = null;
        
        try
        {
            NamingEnumeration searchResults = null;
            
            try
            {
                // perform search
                searchResults = context.search(
                        CONTEXT_NAME,
                        searchStr,
                        this.constraints);
            }
            catch(Exception ex)
            {
                log.warn("Problem doing LDAP search for " + searchStr + ". " +
                        ex.getMessage());
            }
            
            if(searchResults != null)
            {
                for (; searchResults.hasMoreElements() ;)
                {
                    // get all results
                    NamingEnumeration results = ((SearchResult)searchResults.nextElement()).getAttributes().getAll();

                    if(results.hasMoreElements())
                    {
                        user = new User();

                        while(results.hasMoreElements())
                        {
                            Attribute attr = (Attribute)results.next();

                            // find the email entry
                            if(attr.getID().equals(EMAIL_ADDRESS))
                            {
                                user.setEmail(attr.get().toString());
                            }
                            else if(attr.getID().equals(COMMON_NAME))
                            {
                                String fullName = attr.get().toString();
                                int index = fullName.indexOf(" ");
                                if(index != -1){
                                	user.setFirstName(fullName.substring(0, index));
                                	user.setSurname(fullName.substring(index + 1));
                                }
                            }
                            else if(attr.getID().equals(TELEPHONE_NO))
                            {
                                user.setPhone(attr.get().toString());
                            }
                        }

                        // only fetch first, not interested in multiple entries 
                        break;
                    }
                }
            }
        }
        catch(NamingException ex)
        {
            log.error("LDAP Problem fetching results for " + searchStr);
            throw new RuntimeException(ex);
        }

        return user;
    }
    
    /**
     * Command line access to the LDAP look up.
     * @param args Only intersted in a single argument - the UUN.
     */
    public static void main(String[] args)
    {
        if(args.length == 1)
        {
            User user = LDAPAccess.instance().getUserDetailsForUun(args[0]);
            
            if(user != null)
            {
                System.out.println("Name:" + user.getFirstName() + " " + user.getSurname());
                System.out.println("Email:" + user.getEmail());
                System.out.println("Phone:" + user.getPhone());
            }
            else
            {
                System.out.println("No entry found for " + args[0]);
            }
        }
        else
        {
            System.err.println("Look up needs one argument.");
        }
    }
}
