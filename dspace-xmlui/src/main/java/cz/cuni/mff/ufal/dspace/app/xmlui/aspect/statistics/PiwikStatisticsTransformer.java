package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.statistics;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

public class PiwikStatisticsTransformer extends AbstractDSpaceTransformer {

	private static Logger log = Logger.getLogger(GAStatisticsTransformer.class);

	private static final Message T_alredy_subscribed = message("PiwikStatisticsTransformer.already_subscribed");
	private static final Message T_unsubscribe = message("PiwikStatisticsTransformer.unsubscribe");
	private static final Message T_get_monthly = message("PiwikStatisticsTransformer.get_monthly");
	private static final Message T_subscribe = message("PiwikStatisticsTransformer.subscribe");

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_head_title = message("xmlui.statistics.piwik.title");
    private static final Message T_statistics_trail = message("xmlui.statistics.piwik.trail");
	
    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) 
    		throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
    	pageMeta.addTrailLink(contextPath + "/",T_dspace_home);        
        if(dso != null){
        	HandleUtil.buildHandleTrailTerminal(dso, pageMeta, contextPath);
        }        
        pageMeta.addTrail().addContent(T_statistics_trail);
        // Add the page title
        pageMeta.addMetadata("title").addContent(T_head_title);
        
        pageMeta.addMetadata("include-library", "jqplot");        
    }

    /**
	 * What to add at the end of the body
	 */
	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {

        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
		Item item = (Item)dso;

		Request request = ObjectModelHelper.getRequest(objectModel);
		
		Division home = body.addDivision("home", "primary repository");
		Division division = home.addDivision("stats", "secondary stats");
		
		//if(isOwnerOrAdmin(context, eperson, item))
		{

			String sub = request.getParameter("subscribe");
			
			if(sub!=null && sub.equals("yes")) {				
				subscribeAction(eperson.getID(), item.getID());
			} else
			if(sub!=null && sub.equals("no")) {
				unsubscribeAction(eperson.getID(), item.getID());
			}

			boolean isSubscribe = isSubscribe(eperson.getID(), item.getID());
			
			if(isSubscribe) {
				Division subscribe = division.addDivision("subscribe", "alert alert-success");
				Para para = subscribe.addPara(null, "bold");
				para.addHighlight("fa fa-envelope").addContent(" ");			
				para.addContent(T_alredy_subscribed);
				para.addXref("?subscribe=no", T_unsubscribe, "btn btn-xs btn-success pull-right bold", "");
			} else {
				Division subscribe = division.addDivision("subscribe", "alert alert-warning");
				Para para = subscribe.addPara(null, "bold");
				para.addHighlight("fa fa-envelope").addContent(" ");			
				para.addContent(T_get_monthly);
				para.addXref("?subscribe=yes", T_subscribe, "btn btn-xs btn-warning pull-right bold", "");
			}
		}
				
		division.addDivision("report");
	}
	
	public static boolean isOwnerOrAdmin(Context context, EPerson eperson, Item item) throws SQLException {
		return eperson!=null && item.getSubmitter().getID()==eperson.getID() || AuthorizeManager.isAdmin(context);
	}
    
	private boolean subscribeAction(int epersonId, int itemId) {
		boolean status = false;
		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
		functionalityManager.openSession();			
		status = functionalityManager.subscribe(epersonId, itemId); 
		functionalityManager.closeSession();
        return status;		
	}
	
	private boolean unsubscribeAction(int epersonId, int itemId) {
		boolean status = false;
		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
		functionalityManager.openSession();			
		status = functionalityManager.unSubscribe(epersonId, itemId); 
        functionalityManager.closeSession();
        return status;		
	}
	
	private boolean isSubscribe(int epersonId, int itemId) {
		boolean isSubscribe = false;
		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
		functionalityManager.openSession();			
		isSubscribe = functionalityManager.isSubscribe(epersonId, itemId); 
		functionalityManager.closeSession();
        return isSubscribe;
	}

}
