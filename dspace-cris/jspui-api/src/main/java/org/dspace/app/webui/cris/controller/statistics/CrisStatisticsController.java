/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.statistics;


import it.cilea.osd.jdyna.components.IBeanSubComponent;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.ICRISComponent;
import org.dspace.app.cris.integration.statistics.AStatComponentService;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.cris.integration.statistics.IStatsDualComponent;
import org.dspace.app.cris.integration.statistics.StatComponentsService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.statistics.bean.ResultBean;
import org.dspace.app.cris.statistics.bean.RightMenuBean;
import org.dspace.app.cris.statistics.bean.TreeKeyMap;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.app.webui.cris.components.statistics.StatsComponent;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.springframework.web.servlet.ModelAndView;

public class CrisStatisticsController<T extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>>
        extends AStatisticsController<IStatsDualComponent>
{
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(CrisStatisticsController.class);

    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    
    private Class<T> target;

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {

        String id = getId(request);
        String type = request.getParameter("type");
        String mode = request.getParameter("mode");
        
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
            log.error("Malformed input for stas start date "+startDateParam);
        }
        try {
            if (StringUtils.isNotBlank(endDateParam)) {
                endDate = df.parse(endDateParam);
            }
        }
        catch (Exception ex) {
            log.error("Malformed input for stas end date "+endDateParam);
        }
        
        ACrisObject crisObject = (ACrisObject) getObject(request);
        
        if (mode == null || mode.isEmpty())
        {
            mode = StatsComponent.VIEW;
        }
        if (type == null || type.isEmpty())
        {
            type = StatComponentsService._SELECTED_OBJECT;
        }
        ModelAndView modelAndView = new ModelAndView(success);
        try
        {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put(_ID_LABEL, id);
            data.put(_JSP_KEY, jspKey);


            Map<String, IStatsDualComponent> components = statsComponentsService
                    .getComponents();
            TwoKeyMap label = new TwoKeyMap();
            TreeKeyMap dataBeans = new TreeKeyMap();
            IStatsComponent statcomponent = null;

            // create right menu
            List<RightMenuBean> rightMenu = new ArrayList<RightMenuBean>();
            if (statsComponentsService.isShowSelectedObject())
            {
                RightMenuBean menuV = new RightMenuBean();
                menuV.setMode(StatsComponent.VIEW);
                menuV.setType(AStatComponentService._SELECTED_OBJECT);
                if (type.equals(menuV.getType())
                        && mode.equals(menuV.getMode()))
                {
                    menuV.setCurrent(true);
                }
                rightMenu.add(menuV);

                if (ConfigurationManager.getBooleanProperty("solr-statistics",
                        "statistics.show.download.file."
                                + getTarget().getName()))
                {
                    RightMenuBean menuD = new RightMenuBean();
                    menuD.setMode(StatsComponent.DOWNLOAD);
                    menuD.setType(AStatComponentService._SELECTED_OBJECT);
                    if (type.equals(menuD.getType())
                            && mode.equals(menuD.getMode()))
                    {
                        menuD.setCurrent(true);
                    }
                    rightMenu.add(menuD);
                }
            }

            for (String key : components.keySet())
            {
                ICRISComponent component = (ICRISComponent) components.get(key);

                Map<String, IBeanSubComponent> comp = component.getTypes();
				request.setAttribute(component.getClass().getName() + "-"
						+ component.getRelationConfiguration().getRelationName(), crisObject);
				List l = component.sublinks(request, response);
				boolean hasValue = l != null && l.size() > 0;
				
                boolean createMenu = true;
                if (ResearchObject.class.isAssignableFrom(getTarget()))
                {
                    String relationName = ((ICRISComponent) components.get(key))
                            .getRelationConfiguration().getRelationName();
                    if(!relationName.startsWith(getApplicationService().get(ResearchObject.class, Integer.parseInt(id)).getTypeText()))
                    {   
                    	createMenu = false;
                    }
                }

                if (createMenu && hasValue)
                {
                    RightMenuBean menuV = new RightMenuBean();
                    menuV.setMode(StatsComponent.VIEW);
                    menuV.setType(key);
                    if (type.equals(menuV.getType())
                            && mode.equals(menuV.getMode()))
                    {
                        menuV.setCurrent(true);
                    }
                    rightMenu.add(menuV);

                    if (ConfigurationManager.getBooleanProperty(
                            "solr-statistics", "statistics.show.download.file."
                                    + component.getRelationConfiguration()
                                            .getRelationClass().getName()))
                    {                    
	                    RightMenuBean menuD = new RightMenuBean();
	                    menuD.setMode(StatsComponent.DOWNLOAD);
	                    menuD.setType(key);
	                    if (type.equals(menuD.getType())
	                            && mode.equals(menuD.getMode()))
	                    {
	                        menuD.setCurrent(true);
	                    }
	                    rightMenu.add(menuD);
                    }
                }
            }

            if (components.containsKey(type))
            {
                if (mode.equals(StatsComponent.VIEW))
                {
                    statcomponent = components.get(type)
                            .getStatsViewComponent();
                }
                if (mode.equals(StatsComponent.DOWNLOAD))
                {
                    statcomponent = components.get(type)
                            .getStatsDownloadComponent();
                }
            }
            else
            {
                if (mode.equals(StatsComponent.VIEW))
                {
                    statcomponent = statsComponentsService
                            .getSelectedObjectComponent()
                            .getStatsViewComponent();
                }
                if (mode.equals(StatsComponent.DOWNLOAD))
                {
                    statcomponent = statsComponentsService
                            .getSelectedObjectComponent()
                            .getStatsDownloadComponent();
                }

            }

            Integer relationObjectType = statcomponent.getRelationObjectType();
            if (relationObjectType
                    .equals(CrisConstants.CRIS_DYNAMIC_TYPE_ID_START))
            {
                relationObjectType = getApplicationService().get(
                        ResearchObject.class, Integer.parseInt(id)).getType();
                statcomponent.setRelationObjectType(relationObjectType);
            }

            dataBeans.putAll(statcomponent.query(id, solrServer, startDate, endDate));
            label.putAll(statcomponent.getLabels(UIUtil.obtainContext(request),
                    CrisConstants.getEntityTypeText(relationObjectType)));

            ResultBean result = new ResultBean(dataBeans,
                    statsComponentsService.getCommonsParams());
            data.put(_RESULT_BEAN, result);
            data.put("label", label);
            data.put("title", getTitle(request));
            data.put("object", getObject(request));
            data.put("target", getTarget());
            data.put("rightMenu", rightMenu);
            data.put("stats_from_date", startDateParam);
            data.put("stats_to_date", endDateParam);
            data.put("type", type);
            data.put("relationType",
                    CrisConstants.getEntityTypeText(relationObjectType));
            data.put("showExtraTab", statsComponentsService.isShowExtraTab());
            modelAndView.addObject("data", data);
            addSubscriptionStatus(modelAndView, request);

        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            modelAndView = new ModelAndView(error);
        }
        return modelAndView;
    }

    @Override
    public String getId(HttpServletRequest request)
    {
        String uuid = request.getParameter("id");
        return String.valueOf(getApplicationService().getEntityByUUID(uuid)
                .getId());
    }

    @Override
    public DSpaceObject getObject(HttpServletRequest request)
    {
        String uuid = request.getParameter("id");
        return getApplicationService().getEntityByUUID(uuid);
    }

    @Override
    public String getTitle(HttpServletRequest request)
    {
        String uuid = request.getParameter("id");
        return getApplicationService().getEntityByUUID(uuid).getName();
    }

    public void setTarget(Class<T> target)
    {
        this.target = target;
    }

    public Class<T> getTarget()
    {
        return target;
    }
}
