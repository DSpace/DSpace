package org.dspace.authenticate;

import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.Get;
import org.dspace.authority.orcid.Orcid;
import org.dspace.authority.orcid.model.Bio;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;

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

        String orcid = (String) request.getAttribute("orcid");
        String token = (String) request.getAttribute("access_token");
        String refreshToken = (String) request.getAttribute("refresh_token");
        boolean test = "true".equals(request.getParameter("test"));
        if (request == null||orcid==null)
        {
            return BAD_ARGS;
        }
        //Get ORCID profile if they are authenticated (test mode = ?test=true&orcid=[ID]
        //use [orcid]@test.com as the email for test mode
        if(test)
        {
            email = orcid+"@test.com";
        }

        // No email address, perhaps the eperson has been setup, better check it
        if (email == null)
        {
            EPerson p = context.getCurrentUser();
            if (p != null)
            {
                //if eperson exists then get ORCID Profile and binding data to Eperson Account
                email = p.getEmail();
                p.setMetadata("orcid",orcid);
                p.setMetadata("access_token",token);
                orcid = p.getMetadata("orcid");
            }
        }
        //get the orcid profile
        Bio bio = null;
        Orcid orcidObject = Orcid.getOrcid();
        if(orcid!=null)
        {
            if(token==null||test){
                bio = orcidObject.getBio(orcid);
            }
            else
            {
                bio = orcidObject.getBio(orcid,token);
            }
        }
        //get the email from orcid
        if(bio!=null)
        {
            email = bio.getEmail();

        }

        //If Eperson does not exist follow steps similar to Shib....
        if (email == null)
        {
            log.error("No email is given, you're denied access by OAuth, please release email address");
            return AuthenticationMethod.BAD_ARGS;
        }

        email = email.toLowerCase();
        String fname = "";
        if (bio != null)
        {
            // try to grab name from the orcid profile
            fname = bio.getName().getGivenNames();

        }
        String lname = "";
        if (bio != null)
        {
            // try to grab name from the orcid profile
            lname = bio.getName().getFamilyName();
        }

        EPerson eperson = null;
        try
        {
            eperson = EPerson.findByEmail(context, email);
            context.setCurrentUser(eperson);
        }
        catch (AuthorizeException e)
        {
            log.warn("Fail to locate user with email:" + email, e);
            eperson = null;
        }

        // auto create user if needed
        if (eperson == null
                && ConfigurationManager
                .getBooleanProperty("authentication.shib.autoregister"))
        {
            log.info(LogManager.getHeader(context, "autoregister", "email="
                    + email));

            // TEMPORARILY turn off authorisation
            context.setIgnoreAuthorization(true);
            try
            {
                eperson = EPerson.create(context);
                eperson.setEmail(email);
                if (fname != null)
                {
                    eperson.setFirstName(fname);
                }
                if (lname != null)
                {
                    eperson.setLastName(lname);
                }
                eperson.setCanLogIn(true);
                AuthenticationManager.initEPerson(context, request, eperson);
                eperson.setMetadata("orcid",orcid);
                eperson.setMetadata("access_token",token);
                eperson.update();
                context.commit();
                context.setCurrentUser(eperson);
            }
            catch (AuthorizeException e)
            {
                log.warn("Fail to authorize user with email:" + email, e);
                eperson = null;
            }
            finally
            {
                context.setIgnoreAuthorization(false);
            }
        }
        else
        {
            //found the eperson , update the eperson record with orcid id
            try{
                eperson.setMetadata("orcid",orcid);
                eperson.setMetadata("access_token",token);
                eperson.update();
                context.commit();
            }catch (Exception e)
            {
                log.debug("error when update orcid id:"+orcid+" for eperson:"+eperson);
            }
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
