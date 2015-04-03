/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.eperson;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.dspace.authenticate.shibboleth.ShibEPerson;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * Show first screen.
 * 
 * modified for LINDAT/CLARIN
 */
public class WelcomeLogin extends AbstractDSpaceTransformer
{
    /**language strings */
    public static final Message T_title =
    message("xmlui.EPerson.WelcomeLogin.title");

    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    public static final Message T_trail =
        message("xmlui.EPerson.WelcomeLogin.trail");
    
    public static final Message T_head1 =
        message("xmlui.EPerson.WelcomeLogin.head1");

    public static final Message T_head2 =
        message("xmlui.EPerson.WelcomeLogin.head2");

    public static final Message T_para1 =
        message("xmlui.EPerson.WelcomeLogin.para1");

    public static final Message T_warn1 =
        message("xmlui.EPerson.WelcomeLogin.warn1");

    public static final Message T_btn_ok =
        message("xmlui.EPerson.WelcomeLogin.buttonok");

    private static java.util.List<String> ignores_ = get_ignores();
    
    /**
     * Set the page title and trail.
     */
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }
    
    /**
     * Get header keys to ignore.
     */
    static private java.util.List<String> get_ignores() 
    {
        String ignore_s = ConfigurationManager.getProperty("lr", "lr.login.welcome.message.ignore");
        java.util.List<String> ignores = new ArrayList<String>();
        for ( String s : ignore_s.split(",") ) {
            ignores.add(s.trim().toLowerCase());
        }
        return ignores;
    }

    /**
     * Display the login form.
     * @throws AuthorizeException 
     */
    public void addBody(Body body) throws SQLException, SAXException,
            WingException, AuthorizeException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        Division wel = body.addDivision("welcome-div", "well well-light");
        wel.setHead(T_head1);
        
        Division privacy = wel.addDivision("privacy-div", "alert alert-info wordbreak");
        privacy.addPara(null, "bold").addContent(T_head2);
        privacy.addPara(null, "").addContent(T_para1);
                
        IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();

        String welcome = (String) request.getSession().getAttribute("shib.welcome");
        if ( null != welcome ) 
        {
        	List l = privacy.addList("shib-keys", null, "table");
            for ( String line : welcome.split("\n") ) 
            {
            	String key = " ";
            	String value = " ";
            	try {
            		String key_value[] = line.split("=");
            		key = key_value[0].trim();
            		/* Convert name with right encoding */
            		if(key.equalsIgnoreCase(ShibEPerson.fnameHeader) || key.equalsIgnoreCase(ShibEPerson.lnameHeader) || key.equalsIgnoreCase(ShibEPerson.lnameHeader_fallback)){
            			value = functionalityManager.convert(key_value[1].trim());
            		} else {
            			value = key_value[1].trim();
            		}
            	}catch(Exception ex) {
            		
            	}
                if ( ignores_.contains(key) || 0 == value.trim().length() ) {
                    continue;
                } else {
                	l.addLabel(null, "label label-default").addContent(key);
                	l.addItem(value);
                }
            }
        }
        
        privacy.addPara(null, "fa fa-warning fa-4x pull-right text-error").addContent(" ");
        privacy.addPara(null, "").addContent(T_warn1);
        privacy.addPara(null, "container-fluid text-center").addXref(contextPath, 
                        T_btn_ok, "btn btn-repository btn-small");

        // remove the shib attributes
        request.getSession().removeAttribute("shib.welcome");
        
        // store that we have shown it
        eperson.setWelcome();
        eperson.update();
    }
        
}