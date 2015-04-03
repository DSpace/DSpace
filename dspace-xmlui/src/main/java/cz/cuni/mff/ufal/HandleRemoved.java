/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

public class HandleRemoved extends AbstractDSpaceTransformer{
	
	private static final Logger log = Logger.getLogger(HandleRemoved.class);
	
	private static final Message T_title = message("xmlui.ufal.HandleRemoved.title");

	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_trail_item = message("xmlui.ufal.HandleRemoved.trail_item");
	
	private static final Message T_trail_handle_removed = message("xmlui.ufal.HandleRemoved.trail_handle_removed");

	private static final Message T_head = message("xmlui.ufal.HandleRemoved.head");

	private static final Message T_message = message("xmlui.ufal.HandleRemoved.message");
	
	public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {

		// Set the page title
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail_handle_removed);
		
	}

	public void addBody(Body body) throws WingException {
		Request request = ObjectModelHelper.getRequest(objectModel);	
		String referrer = request.getHeader("referer");
		//log.warn("User found removed handle. Came from [" + referrer +"]");
		log.warn("User found removed handle. The PID was '" + parameters.getParameter("myHandle","null") +"' and the referrer was '"+referrer+"'");
		Division div = body.addDivision("HandleRemoved");
		div.setHead(T_head);
		div.addPara(T_message);
	}

}
