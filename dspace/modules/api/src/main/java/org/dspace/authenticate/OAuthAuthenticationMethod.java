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


        //Get ORCID profile if they are authenticated (test mode = ?test=true&orcid=[ID]
        //use [orcid]@test.com as the email for test mode
        if(test)
        {
            email = orcid+"@test.com";
        }

        EPerson currentUser = context.getCurrentUser();


        if(orcid!=null)
        {
            //Step 1, check if the orcid id exists or not
            EPerson e = EPerson.findByOrcidId(context,orcid);

            if(e!=null)
            {
               //orcid id already exists

               if(currentUser!=null&&e.getID()==currentUser.getID())
               {
                   //the orcid id already linked to the current user
                   request.getSession().setAttribute("oauth.authenticated",
                           Boolean.TRUE);
                   return AuthenticationMethod.SUCCESS;
               }
               else if(currentUser==null)
               {
                   //the orcid id exists and already linked to the user ,login successful
                   currentUser = e;
                   request.getSession().setAttribute("oauth.authenticated",
                           Boolean.TRUE);
                   context.setCurrentUser(e);
                   return AuthenticationMethod.SUCCESS;
               }
               else
               {
                   //todo:report the exist orcid user
                   request.getSession().setAttribute("exist_orcid",e.getEmail());
                   return BAD_CREDENTIALS;
               }
            }
            else{



            //Step 2, check the current login user
            if (currentUser != null)
            {
                //link the orcid id to the current login user
                currentUser.setMetadata("orcid",orcid);
                currentUser.setMetadata("access_token",token);
                orcid = currentUser.getMetadata("orcid");
                try{
                currentUser.update();
                }catch (Exception exception)
                {
                    log.error("error when link the orcid id:"+orcid+" to current login in user:"+currentUser.getEmail());
                }
                context.commit();
                request.getSession().setAttribute("oauth.authenticated",
                        Boolean.TRUE);
                return AuthenticationMethod.SUCCESS;
            }
            else{

            //step 3, orcid id doen't exist and user does not login, remind user to create user account and then link the orcid
            request.getSession().setAttribute("set_orcid",orcid);
            request.getSession().setAttribute("oauth.authenticated",Boolean.TRUE);
            return BAD_CREDENTIALS;

            }
            }
        }
        else
        {
            return BAD_CREDENTIALS;
        }


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
