/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.statistics;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.RPTypeNestedObject;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public class RPStatisticsController
        extends
        CrisStatisticsController<ResearcherPage, RPProperty, RPPropertiesDefinition, RPNestedProperty, RPNestedPropertiesDefinition, RPNestedObject, RPTypeNestedObject>
{
    @Override
    public String getId(HttpServletRequest request)
    {
        String uuid = request.getParameter("id");
        if (uuid != null && !uuid.isEmpty())
        {
            return String.valueOf(getApplicationService().getEntityByUUID(uuid)
                    .getId());
        }
        Context context = null;
        String crisID = "";
        try
        {
            context = UIUtil.obtainContext(request);
            crisID = context.getCrisID();            
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        return String.valueOf(getApplicationService().getEntityByCrisId(
                crisID, ResearcherPage.class).getId());
    }
    
    @Override
    public DSpaceObject getObject(HttpServletRequest request)
    {
        String uuid = request.getParameter("id");
        if(uuid!=null && !uuid.isEmpty()) {
            return getApplicationService().getEntityByUUID(uuid);
        }
        Context context = null;
        String crisID = "";
        try
        {
            context = UIUtil.obtainContext(request);
            crisID = context.getCrisID();            
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        return getApplicationService().getEntityByCrisId(
                crisID, ResearcherPage.class);
    }

    @Override
    public String getTitle(HttpServletRequest request)
    {
        String uuid = request.getParameter("id");
        if(uuid!=null && !uuid.isEmpty()) {
            return getApplicationService().getEntityByUUID(uuid).getName();
        }
        Context context = null;
        String crisID = "";
        try
        {
            context = UIUtil.obtainContext(request);
            crisID = context.getCrisID();            
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        return getApplicationService().getEntityByCrisId(
                crisID, ResearcherPage.class).getName();
    }
}
