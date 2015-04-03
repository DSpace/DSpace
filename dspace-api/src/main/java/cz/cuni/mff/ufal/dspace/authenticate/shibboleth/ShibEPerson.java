/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.authenticate.shibboleth;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * Shibboleth authentication for DSpace
 */
public class ShibEPerson
{
    // constants
    //
    final static String netid_property = ConfigurationManager.getProperty(
            "authentication-shibboleth","netid-header");
    final static String emailHeader = ConfigurationManager.getProperty(
            "authentication-shibboleth","email-header");
    final static boolean isUsingTomcatUser = ConfigurationManager.getBooleanProperty(
            "authentication-shibboleth","email-use-tomcat-remote-user");
    
    public final static String fnameHeader = ConfigurationManager.getProperty(
            "authentication-shibboleth","firstname-header");
    public final static String lnameHeader = ConfigurationManager.getProperty(
            "authentication-shibboleth","lastname-header");
    public final static String lnameHeader_fallback = ConfigurationManager.getProperty(
            "authentication-shibboleth","lastname-header-fallback");

    
    
    // variables
    //
    private static Logger logger_ = cz.cuni.mff.ufal.Logger.getLogger(ShibEPerson.class);
    
    private ShibHeaders shib_headers_ = null;
    private String org_ = null;
    
    
    // ctors
    //
    
    public ShibEPerson( ShibHeaders shib_headers, String org )
    {
        shib_headers_ = shib_headers;
        org_ = org;
    }
    
    //
    //
    
    public String get_email() {
        return shib_headers_.get_single(emailHeader);
    }
    public String get_first_name() {
        return shib_headers_.get_single(fnameHeader);
    }
    public String get_first_name(String default_value) {
        return get_first_name() != null ? get_first_name() : default_value;
    }
    public String get_last_name() {
        return shib_headers_.get_single(lnameHeader);
    }
    public String get_last_name(String default_value) {
        return get_last_name() != null ? get_last_name() : default_value;
    }

    public String get_last_name(String default_value, boolean fallback) 
    {
        if ( !fallback ) {
            return get_last_name(default_value);
        }else {
            if ( get_last_name(default_value) == null ) {
                String tmp = shib_headers_.get_single(lnameHeader_fallback);
                return null != tmp ? tmp : default_value;
            }else {
                return get_last_name(default_value);
            }
        }
        
    }

    
    
    //
    //
    
    public String get_first_netid()
    {
        String netid = null;
        for ( String netidHeader : get_netid_headers() ) 
        {
            netidHeader = netidHeader.trim();
            netid = shib_headers_.get_single(netidHeader);
            if (netid != null) 
            {
                //When creating use first match (eppn before targeted-id)
                return form_netid(netid);
            }
        }
        return null;
    }
    
    public EPerson get_by_netid(Context context) throws SQLException
    {
        EPerson eperson = null;
        
        for (String netidHeader : get_netid_headers())
        {
            netidHeader = netidHeader.trim();
            String netid = shib_headers_.get_single(netidHeader);
            if (netid != null) {
                netid = form_netid(netid); //are targeted-id unique among idps?
                eperson = EPerson.findByNetid(context, netid);

                if (eperson == null)
                {
                    logger_.info("Unable to identify EPerson based upon Shibboleth " +
                            "netid header: '"+netidHeader+"'='"+netid+"'.");
                }else {
                    logger_.debug("Identified EPerson based upon Shibboleth " +
                            "netid header: '"+netidHeader+"'='"+netid+"'.");
                    break; //Managed to find eperson no need to try other fields (eppn/targetedId)
                }
            }
        }

        return eperson;
    }
    
    
    //
    //
    
    public EPerson get_by_email(Context context) throws AuthorizeException, SQLException
    {
        EPerson eperson = null;
        
        if ( emailHeader != null ) {
            String email = shib_headers_.get_single(emailHeader);

            
            /* <UFAL>
             * 
             * Checking for a valid email address.
             * 
             */

            IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
            email = functionalityManager.getEmailAcceptedOrNull ( email );
            
            /* </UFAL> */
            
            
            if (email != null) {
                email = email.toLowerCase();
                eperson = EPerson.findByEmail(context, email);

                if (eperson == null)
                    logger_.info("Unable to identify EPerson based upon Shibboleth email header: " +
                            "'"+emailHeader+"'='"+email+"'.");
                else {
                    logger_.info("Identified EPerson based upon Shibboleth email header: " +
                            "'"+emailHeader+"'='"+email+"'.");
                    
                    /* <UFAL>
                     * 
                     * Checking if the user exists in the license database.
                     * If not register.
                     * 
                     */

                    if(!DSpaceApi.registerUser(org_, eperson)){
                        logger_.error("Unable to add the user in the Licenses database - " +
                                "already registered under different user?");
                        eperson = null;
                    }
                    
                    /* </UFAL> */
                    
                }

                if (eperson != null && eperson.getNetid() != null) {
                    // If the user has a netID it has been locked to that netid, don't let anyone else try and steal the account.
                    //log when caught in authenticate
                    //log.error("The identified EPerson based upon Shibboleth email header, '"+emailHeader+"'='"+email+"', is locked to another netid: '"+eperson.getNetid()+"'. This might be a possible hacking attempt to steal another users credentials. If the user's netid has changed you will need to manually change it to the correct value or unset it in the database.");
                    String oldNetid = eperson.getNetid();
                    //This works because we build netids this way above
                    int delimiter = oldNetid.lastIndexOf('[');
                    String oldIdp = oldNetid.substring( delimiter+1, oldNetid.length()-1);
                    oldNetid = oldNetid.substring(0, delimiter);
                    functionalityManager.setErrorMessage(
                            String.format("Your email (%s) is already associated with a different user. " +
                            		"It is also possible that you used a different identity provider to login before.", email));
                    //eperson=null;
                    throw new AuthorizeException("The identified EPerson based upon Shibboleth email header, " +
                            "'"+emailHeader+"'='"+email+"', is locked to another netid: '"+eperson.getNetid()+"'. " +
                                    "This might be a possible hacking attempt to steal another users credentials. " +
                                    "If the user's netid has changed you will need to manually change it to the " +
                                    "correct value or unset it in the database.");
                }
            
            }else if (email == null && eperson == null) {
                //Neither valid email nor netid, but will ask for the mail later
                logger_.warn("Empty email & netid from " + org_ + 
                        " received these headers:\n");
                shib_headers_.log_headers();
                //logAndSetMessage(context, request, organization);
            }
        }   
        
        return eperson;
    }

    
    //
    //
    
    public EPerson get_by_tomcat(Context context, HttpServletRequest request) throws AuthorizeException, SQLException
    {
        EPerson eperson = null;
        if (isUsingTomcatUser) 
        {
            String email = request.getRemoteUser();
            if (email != null) 
            {
                email.toLowerCase();
                eperson = EPerson.findByEmail(context, email);

                if (eperson == null) {
                    logger_.info("Unable to identify EPerson based upon Tomcat's remote user: '"+email+"'.");
                }else {
                    logger_.info("Identified EPerson based upon Tomcat's remote user: '"+email+"'.");
                }

                if (eperson != null && eperson.getNetid() != null) 
                {
                    // If the user has a netID it has been locked to that netid, don't let anyone else try and steal the account.
                    logger_.error("The identified EPerson based upon Tomcat's remote user, " +
                        "'"+email+"', is locked to another netid: '"+eperson.getNetid()+"'. " +
                        "This might be a possible hacking attempt to steal another users " +
                        "credentials. If the user's netid has changed you will need to " +
                        "manually change it to the correct value or unset it in the database.");
                    eperson = null;
                }
            }
        }
        
        return eperson;
    }
    
    //
    //
    private String form_netid(String netid) {
        return netid + "[" + org_ +"]";
    }

    private String[] get_netid_headers() 
    {
        String[] netidHeaders = null;
        if ( netid_property != null ) {
            netidHeaders = netid_property.split(",");
        }
        
        if ( netidHeaders == null ) {
            return new String[0];
        }
        
        return netidHeaders;
    }
    
    public static String info_to_log()
    {
        return 
        "  NetId Headers: '"+netid_property+"'='%s' (Optional) \n" +
        "  Email Header: '"+emailHeader+"'='%s' \n" +
        "  First Name Header: '"+fnameHeader+"'='%s' \n" +
        "  Last Name Header: '"+lnameHeader+"'='%s'";
    }
    
}