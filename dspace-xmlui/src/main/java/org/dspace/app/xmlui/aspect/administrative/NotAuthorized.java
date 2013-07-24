/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;

/**
 * Display a generic error message saying the user can
 * not perform the requested action.
 * 
 * @author Scott Phillips
 */
public class NotAuthorized extends AbstractDSpaceTransformer   
{	
	
	private static final Message T_title = 
		message("xmlui.administrative.NotAuthorized.title");
	
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");

	private static final Message T_trail = 
		message("xmlui.administrative.NotAuthorized.trail");
	
	private static final Message T_head = 
		message("xmlui.administrative.NotAuthorized.head");
	
	private static final Message T_para1a = 
		message("xmlui.administrative.NotAuthorized.para1a");

	private static final Message T_para1b = 
		message("xmlui.administrative.NotAuthorized.para1b");
	
	private static final Message T_para1c = 
		message("xmlui.administrative.NotAuthorized.para1c");
	
	private static final Message T_para2 = 
		message("xmlui.administrative.NotAuthorized.para2");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException 
	{
		String loginURL = contextPath+"/login";
		String feedbackURL = contextPath+"/feedback";
		
        Division main = body.addDivision("not-authorized","primary administrative");
		main.setHead(T_head);
		Para para1 = main.addPara();
		para1.addContent(T_para1a);
		para1.addXref(feedbackURL,T_para1b);
		para1.addContent(T_para1c);

		main.addPara().addXref(loginURL,T_para2);
		
	}
	
}
