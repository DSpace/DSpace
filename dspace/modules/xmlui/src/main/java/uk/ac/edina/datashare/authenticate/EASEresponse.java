package uk.ac.edina.datashare.authenticate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import edu.umich.auth.AuthFilterRequestWrapper;
import edu.umich.auth.cosign.CosignPrincipal;
import edu.umich.auth.cosign.CosignServletCallbackHandler;
import uk.ac.edina.datashare.db.DbUpdate;
import uk.ac.edina.datashare.eperson.DSpaceAccount;
import uk.ac.edina.datashare.ldap.LDAPAccess;
import uk.ac.edina.datashare.ldap.User;
import uk.ac.edina.datashare.utils.DSpaceUtils;

/**
 * EASE login call back class.
 */
public class EASEresponse extends CosignServletCallbackHandler
{
    /** log4j category */
    private static final Logger LOG = Logger.getLogger(EASEresponse.class);
    
    /** The ease university user name attribute name */
    public static final String EASE_UUN = "ease.uun";
    
    /**
     * User has successfully logged into EASE, determine if the user has a
     * dspace account. If dspace account exists, log the user on.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void handleSuccessfulLogin() throws ServletException
    { 
        this.getRequest().setAttribute(EASE_UUN, null);
        
        // do standard successful login
        super.handleSuccessfulLogin();
        
        if(this.getRequest() instanceof AuthFilterRequestWrapper)
        { 
            HttpServletResponse response = this.getResponse();
            AuthFilterRequestWrapper request = (AuthFilterRequestWrapper)this.getRequest();
            String url = request.getContextPath();
            CosignPrincipal principal = (CosignPrincipal)request.getUserPrincipal();
            
            // get uun from principal name
            String uun = principal.getName();
            
            try
            {
                // try and get eperson from uun
                Context context = ContextUtil.obtainContext(request);
                EPerson eperson = EPerson.findByNetid(context, uun);
                
                if(eperson != null)
                {
                    // found user log them in
                    DSpaceAccount.login(context, request, eperson);
                }
                else
                {
                    // user not found try LDAP
                    User user = LDAPAccess.instance().getUserDetailsForUun(uun);
                    
                    if(user != null)
                    {
                        // details found in LDAP - first check if account exists
                        eperson = DSpaceUtils.findByEmail(
                                context,
                                user.getEmail());
                        
                        if(eperson != null)
                        {
                            // account exists, update netid
                            DSpaceAccount.updateNetId(context, eperson, uun);
                        }
                        else
                        {
                            // account doesn't exits create new one
                            eperson = DSpaceAccount.createAccount(
                                    context,
                                    user,
                                    user.getEmail(),
                                    uun);
                            
                            // generate sword key
                            DbUpdate.insertSwordKey(context, eperson);
                        } 
                        
                        context.commit();

                        // log user in
                        DSpaceAccount.login(context, request, eperson);
                    }
                    else
                    {
                        // clear any interrupted requests
                        request.getSession().setAttribute(AuthenticationUtil.REQUEST_INTERRUPTED, null);
                        
                        // unable to automatically create account go to registration
                        url = response.encodeRedirectURL(request.getContextPath() +
                                "/register?uun=" + uun);
                    }
                }
                
                // resume any interrupted request
                Map om = new HashMap();
                om.put(HttpEnvironment.HTTP_REQUEST_OBJECT, request);
                String interruptUrl = AuthenticationUtil.resumeInterruptedRequest(om);
                
                try
                {
                    if(interruptUrl == null){
                        response.sendRedirect(url);
                    }
                    else{
                        response.sendRedirect(interruptUrl);
                    }
                }
                catch(IOException ex)
                {
                    throw new RuntimeException(ex);
                }
            }
            catch(SQLException ex)
            {
                throw new RuntimeException("Failed to fetch context: " + ex.getMessage());
            } 
        }
    }
    
    /**
     * User failed to logon to EASE
     */
    public boolean handleFailedLogin(Exception ex) throws ServletException
    { 
        LOG.warn("EASE login failed: " + ex);
        return super.handleFailedLogin(ex);
    }
}
