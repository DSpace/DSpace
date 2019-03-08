/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.orcid.jaxb.model.record_v2.EmailType;
import org.orcid.jaxb.model.record_v2.Emails;
import org.orcid.jaxb.model.record_v2.PersonalDetails;

/**
 *
 * @author mdiggory at atmire.com
 */
public class OAuthAuthenticationMethod implements AuthenticationMethod{

    /** log4j category */
    private static Logger log = Logger.getLogger(OAuthAuthenticationMethod.class);

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isImplicit() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int[] getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
        return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request) throws SQLException {


        String email = null;
        EPerson eperson = null;
        

//        String refreshToken = (String) request.getAttribute("refresh_token");
        if (request == null)
        {
            return BAD_ARGS;
        }
        
        String orcid = (String) request.getAttribute("orcid");
        if(orcid== null ){
        	return BAD_ARGS;
        }
        String token = (String) request.getAttribute("access_token");
        String scope = (String) request.getAttribute("scope");        
        EPerson[] epersons = EPerson.search(context, orcid);
        
        if(epersons != null && epersons.length > 1) {
            log.error("Fail to authorize user with orcid: "+orcid + " email:" + email + " - Multiple Users found");
            return AuthenticationMethod.NO_SUCH_USER;
        }
        // No email address, perhaps the eperson has been setup, better check it
        if (epersons == null || epersons.length == 0)
        {
        	eperson = context.getCurrentUser();
            if (eperson != null)
            {
                //if eperson exists then get ORCID Profile and binding data to Eperson Account
                email = eperson.getEmail();
            }
        }
        else {
            eperson = epersons[0];
        }
        
        //get email from orcid
        OrcidService orcidObject = OrcidService.getOrcid();
        if (orcid != null && email == null)
        {
            Emails emails = orcidObject.getEmails(orcid, token);
            if (emails != null)
            {
                if(emails.getEmail() != null && !emails.getEmail().isEmpty()) {
                    for(EmailType emailCType : emails.getEmail()) {
                        if(emailCType.isVerified()) {
                            email = emailCType.getEmail();
                        }
                    }
                }
            }
        }

//        //If Eperson does not exist follow steps similar to Shib....
//        if (eperson == null && email == null)
//        {
//            log.error("No email is given, you're denied access by OAuth, please release email address");
//            return AuthenticationMethod.BAD_ARGS;
//        }

        if (email != null) {
            email = email.toLowerCase();
        }
        
        // never logged in! Use verified email from ORCID Registry if exist
        if (eperson == null && email != null) {
	        try
	        {
	            eperson = EPerson.findByEmail(context, email);
	        }
	        catch (AuthorizeException e)
	        {
	            log.warn("Fail to locate user with email:" + email, e);
	            eperson = null;
	        }
        }
        
        try
        {
        	// TEMPORARILY turn off authorisation
            context.turnOffAuthorisationSystem();
	        // auto create user if needed
	        if (eperson == null
	                && ConfigurationManager
	                .getBooleanProperty("authentication-oauth", "autoregister"))
	        {
	            log.info(LogManager.getHeader(context, "autoregister", "orcid="
	                    + orcid));

	            String fname = "";
	            String lname = "";
	            
	            PersonalDetails personalDetails = orcidObject.getPersonalDetails(orcid, token);
	            if (personalDetails != null)
	            {
	                if (personalDetails.getName() != null) {
	                    // try to grab name from the orcid profile
	                    fname = personalDetails.getName().getGivenNames().getValue();

	                    // try to grab name from the orcid profile
	                    lname = personalDetails.getName().getFamilyName().getValue();
	                }
	            }
	            
                eperson = EPerson.create(context);
                eperson.setEmail(email!=null?email:orcid);
                eperson.setFirstName(fname);
                eperson.setLastName(lname);
                eperson.setCanLogIn(true);
                AuthenticationManager.initEPerson(context, request, eperson);
                eperson.addMetadata("eperson", "orcid", null, null, orcid);
                eperson.addMetadata("eperson", "orcid", "accesstoken", null, token);
                eperson.update();
                context.commit();
                context.setCurrentUser(eperson);
	        }
	        else if(eperson!=null)
	        {
	            //found the eperson , update the eperson record with orcid id
                //eperson.setNetid(orcid);
                if (eperson.getEmail() == null) {
                	eperson.setEmail(email!=null?email:orcid);
                } 
                //eperson.setMetadata("access_token",token);
                Metadatum[] md = eperson.getMetadata("eperson", "orcid", null, null);
                boolean found = false;
                if (md != null && md.length > 0) {
                	for (Metadatum m : md) {
                		if (StringUtils.equals(m.value, orcid)) {
                			found = true;
                			break;
                		}
                	}
                }
                if (!found) {
                	eperson.addMetadata("eperson", "orcid", null, null, orcid);
                }
                eperson.addMetadata("eperson", "orcid", "accesstoken", null, token);
                eperson.update();
                context.commit();
	        }
        }
        catch (AuthorizeException e)
        {
            log.warn("Fail to authorize user with orcid: "+orcid + " email:" + email, e);
            eperson = null;
        }
        finally
        {
            context.restoreAuthSystemState();
        }

        if (eperson == null)
        {
            return AuthenticationMethod.NO_SUCH_USER;
        }
        else
        {
            // the person exists, just return ok
            context.setCurrentUser(eperson);
            request.getSession().setAttribute("oauth.authenticated",
                    Boolean.TRUE);
        }

        return AuthenticationMethod.SUCCESS;
    }
    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {
        if(ConfigurationManager.getBooleanProperty("authentication-oauth","choice-page")){
            return response.encodeRedirectURL(request.getContextPath()
                + "/oauth-login");
        }
        else
        {
            return null;
        }
    }

    @Override
    public String loginPageTitle(Context context) {
        return "org.dspace.authenticate.OAuthAuthentication.title";
    }
}
