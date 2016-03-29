/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.statistics;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.cris.integration.statistics.StatComponentsService;
import org.dspace.app.cris.statistics.bean.ResultBean;
import org.dspace.app.cris.statistics.bean.TreeKeyMap;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.constants.Constants;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.springframework.web.servlet.ModelAndView;

public class StatisticsController extends AStatisticsController
{
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(StatisticsController.class);
    
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException, IllegalStateException, SQLException
    {

        String id = getId(request);
        String type = StringUtils.isNotBlank( request.getParameter("type")) ? request.getParameter("type") : "item";

        Date startDate = null;
        Date endDate = null;
        String startDateParam = request.getParameter("stats_from_date");
        String endDateParam = request.getParameter("stats_to_date");
        try {
			if (StringUtils.isNotBlank(startDateParam)) {
				
        		startDate = df.parse(startDateParam);

        	}
        }
        catch (Exception ex) {
        	log.error("Malformed input for stats start date "+startDateParam);
        }
        try {
			if (StringUtils.isNotBlank(endDateParam)) {
        		endDate = df.parse(endDateParam);
        	}
        }
        catch (Exception ex) {
        	log.error("Malformed input for stats end date "+endDateParam);
        }

        ModelAndView modelAndView = new ModelAndView(success);
        try
        {
        	DSpaceObject dso = getObject(request);
        	Context c = UIUtil.obtainContext(request);
        	if (!canSeeStatistics(c, dso)) {
        		throw new AuthorizeException("Only administrator can access the statistics of the object "+dso.getHandle());
        	}
        	Map<String, Object> data = new HashMap<String, Object>();
            data.put(_ID_LABEL, id);
            data.put(_JSP_KEY, jspKey);
            data.put(_MAX_LIST_MOST_VIEWED_ITEM, maxListMostViewedItem);

            Map<String, IStatsComponent> components = statsComponentsService.getComponents();
            TwoKeyMap label = new TwoKeyMap();
            TreeKeyMap dataBeans = new TreeKeyMap();
            IStatsComponent statcomponent = null;
            
            if(components.containsKey(type)) {
                statcomponent = components.get(type);
            }
            else {
                type = StatComponentsService._SELECTED_OBJECT;
                statcomponent = (IStatsComponent)statsComponentsService.getSelectedObjectComponent(); 
            }
            
            if (statcomponent != null) {
	            dataBeans.putAll(statcomponent.query(id, solrServer, startDate, endDate));                
	            label.putAll(statcomponent.getLabels(UIUtil.obtainContext(request),type));
            }
            
            ResultBean result = new ResultBean(dataBeans, statsComponentsService.getCommonsParams());
            data.put(_RESULT_BEAN, result);
            data.put("label",label);
            data.put("title", getTitle(request));
			data.put("object", dso);
            DSpaceObject parentObject = getParentObject(request);
			data.put("parentObject", parentObject);
            data.put("seeParentObject", canSeeStatistics(c, parentObject));
            data.put("childrenObjects",getChildrenObjects(c,dso));
            data.put("seeUpload", canSeeUpload(c,dso));
            data.put("stats_from_date", startDateParam);
            data.put("stats_to_date", endDateParam);
            data.put("type", type);
            data.put("showExtraTab", statsComponentsService.isShowExtraTab());
            modelAndView.addObject("data", data);

        }catch (AuthorizeException e){
            log.error(e.getMessage(), e);
            JSPManager
            .showAuthorizeError(
                    request,
                    response,
                    new AuthorizeException(
                            "Only system administrator can access to disabled researcher page"));
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            JSPManager
            .showInternalError(
                    request,
                    response);
        }
        return modelAndView;
    }




	private Object getChildrenObjects(Context context,DSpaceObject dso) throws SQLException {
		List<DSpaceObject> dsos = new ArrayList<DSpaceObject>();
		if(!ConfigurationManager.getBooleanProperty("usage-statistics", "webui.statistics.showChildList", false)){
			return dsos;
		}
		if (dso.getType() == org.dspace.core.Constants.COMMUNITY ) {
			for (Community com : ((Community) dso).getSubcommunities()) {
				dsos.add(com);
			}
			for (Collection col : ((Community) dso).getCollections()) {
				dsos.add(col);
			}
		} else if( dso.getType() == org.dspace.core.Constants.SITE){
			 Community[] comm = Community.findAllTop(context);
			 dsos.addAll(Arrays.asList(comm));
		}
		return dsos;
	}

	private boolean canSeeStatistics(Context c, DSpaceObject dso)
			throws SQLException {
			if(dso != null){
				return ConfigurationManager.getBooleanProperty("usage-statistics", "webui.statistics."+dso.getTypeText().toLowerCase()+".public", "webui.statistics.item.public") || AuthorizeManager.isAdmin(c, dso);
			}
			return false;
			
	}

	private boolean canSeeUpload(Context c, DSpaceObject dso)
			throws SQLException {
		if (dso != null) {

			return ConfigurationManager
					.getBooleanProperty("usage-statistics", "webui.statistics.upload."
							+ dso.getTypeText().toLowerCase() + ".public")
					|| AuthorizeManager.isAdmin(c, dso);
		}
		return false;
	}

  
    @Override
    public String getId(HttpServletRequest request) throws IllegalStateException, SQLException
    {
        String id = request.getParameter("handle");
        if (StringUtils.isNotEmpty(id))
        {           
            return Integer.toString(_getObject(request, id).getID());
        }
        return null;
    }
    

    @Override
    public DSpaceObject getObject(HttpServletRequest request) throws IllegalStateException, SQLException
    {
        String id = request.getParameter("handle");
        if (StringUtils.isNotEmpty(id))
        {
            return _getObject(request, id);
        }else {
        	id = HandleManager.getPrefix() + "/" + String.valueOf(Site.SITE_ID);
        	return _getObject(request, id);
        }
    }
    
    private DSpaceObject _getObject(HttpServletRequest request, String id) throws IllegalStateException, SQLException
    {
		DSpaceObject dso;
		dso = HandleManager.resolveToObject(UIUtil.obtainContext(request), id);
		return dso;
    }

    public DSpaceObject getParentObject(HttpServletRequest request) throws SQLException
    {
        DSpaceObject dso = getObject(request);
		
        if(dso != null && ConfigurationManager.getBooleanProperty("usage-statistics", "webui.statistics.showParent", false)){
			return  dso.getParentObject();
        }
        return null;
    }

    @Override
    public String getTitle(HttpServletRequest request) throws IllegalStateException, SQLException
    {
        String id = request.getParameter("handle");
        if (StringUtils.isNotEmpty(id))
        {          
            return _getObject(request, id).getName();
        }
        return null;

    }
    

    
  

}
