/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;


import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

public class FailedAuthentication extends AbstractDSpaceTransformer {
	private static final String SESSION_ATTRIBUTE_NAME = "xmlui.Eperson.FailedAuthentication.message";
	
	public static final Message BAD_CREDENTIALS = message("xmlui.EPerson.FailedAuthentication.BadCreds");
	public static final Message BAD_ARGUMENTS   = message("xmlui.EPerson.FailedAuthentication.BadArgs");
	public static final Message NO_SUCH_USER    = message("xmlui.EPerson.FailedAuthentication.NoSuchUser");
	
	
	/**language strings */
    public static final Message T_title =
    message("xmlui.EPerson.FailedAuthentication.title");
    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    public static final Message T_trail =
        message("xmlui.EPerson.FailedAuthentication.trail");
    
    public static final Message T_h1 =
        message("xmlui.EPerson.FailedAuthentication.h1");
    
	
	public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
		Request request = ObjectModelHelper.getRequest(objectModel);
		
		Division div = body.addDivision("failed_auth");
		div.setHead(T_h1);
		div.addPara((Message)request.getSession().getAttribute(SESSION_ATTRIBUTE_NAME));
		deRegisterErrorCode(request);
	}

	public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
		pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
	}
	
	public static void registerErrorCode(Message message, HttpServletRequest request){
		request.getSession().setAttribute(SESSION_ATTRIBUTE_NAME, message);
	}
	
	private static void deRegisterErrorCode(Request request){
		request.getSession().removeAttribute(SESSION_ATTRIBUTE_NAME);
	}
}
