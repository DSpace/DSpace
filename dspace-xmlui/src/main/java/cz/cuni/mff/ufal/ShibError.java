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
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.core.ConfigurationManager;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

public class ShibError extends AbstractDSpaceTransformer{
	
	private static final Logger log = Logger.getLogger(ShibError.class);
	
	private static final String T_title = "Shibboleth exception";

	private static final Message T_dspace_home = message("xmlui.general.dspace_home");	
	
	private static final String T_trail_shib_error = "Shibboleth exception";

	
	public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {

		// Set the page title
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail_shib_error);
		
	}

	public void addBody(Body body) throws WingException {
		Request request = ObjectModelHelper.getRequest(objectModel);
    
		//log.warn("User found removed handle. Came from [" + referrer +"]");
		//log.warn("User found removed handle. The PID was '" + parameters.getParameter("myHandle","null") +"' and the referrer was '"+referrer+"'");
    	String now = request.getParameter("now");
    	String requestURL = request.getParameter("requestURL");
		String errorType = request.getParameter("errorType");
		String errorText = request.getParameter("errorText");
		String entityID = request.getParameter("entityID");
		String statusCode = request.getParameter("statusCode");
		String statusCode2 = request.getParameter("statusCode2");
		String statusMessage = request.getParameter("statusMessage");
		String support = ConfigurationManager.getProperty("lr.help.mail");
		Division div = body.addDivision("ShibError");		
		div.addPara().addFigure("./themes/UFAL/images/shibboleth_logo.jpg", null, null);
		div.setHead(errorType);
		div.addPara("The system encountered an error at " + now);
		Para p = div.addPara();
		p.addContent("To report this problem, please contact the site administrator at ");		
		p.addXref("mailto:" + support, support, null, null);
		div.addPara("Please include the following message in any email:");
		p = div.addPara(null, "error");
		p.addContent(errorType + " at " + requestURL);
		div.addPara(errorText);
		if(entityID != null){
			div.addPara("EntityID: " + entityID);
		}
		if(statusCode != null){
			div.addPara("Error from identity provider:");
			Table t = div.addTable("status", 3, 2);
			Row row = t.addRow();
			row.addCellContent("Status:");
			row.addCellContent(statusCode);
			row = t.addRow();
			row.addCellContent("Sub-Status:");
			row.addCellContent(statusCode2);
			row = t.addRow();
			row.addCellContent("Message:");
			row.addCellContent(statusMessage);
			
		}
	}

}
