package org.dspace.app.webui.cris.processor;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.batch.dao.ImpRecordDAO;
import org.dspace.app.cris.batch.dao.ImpRecordDAOFactory;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.util.ICrisHomeProcessor;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.plugin.PluginException;

public class RPImpRecordProcessor
        implements ICrisHomeProcessor<ResearcherPage>
{

    private String sourceRef;
    
    @Override
    public Class<ResearcherPage> getClazz()
    {
        return ResearcherPage.class;
    }

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, ResearcherPage entity)
            throws PluginException, AuthorizeException
    {
        Integer epersonID = entity.getEpersonID();
        if(epersonID!=null) {
            try
            {
                ImpRecordDAO impRecordDAO = ImpRecordDAOFactory.getInstance(context);
                Integer count = impRecordDAO.countByEPersonIDAndSourceRefAndLastModifiedInNull(epersonID, sourceRef);
                Map<String, String> mapInfo = (Map<String, String>)request.getAttribute("infoPendingImpRecord");
                if(mapInfo == null) {
                    mapInfo = new HashMap<String, String>();
                }
                mapInfo.put(sourceRef, ""+count);
                request.setAttribute("infoPendingImpRecord", mapInfo);
            }
            catch (SQLException e)
            {
                throw new PluginException(e);
            }
        }
    }
    
    public void setSourceRef(String sourceRef)
    {
        this.sourceRef = sourceRef;
    }
}
