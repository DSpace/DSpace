/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.json;

import flexjson.JSONSerializer;
import it.cilea.osd.common.controller.BaseAbstractController;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.cris.dto.RelatedObject;
import org.dspace.app.webui.cris.dto.RelatedObjects;
import org.dspace.app.webui.cris.util.RelationPreferenceUtil;
import org.dspace.app.webui.cris.util.RelationPreferenceUtil.Sort;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.utils.DSpace;
import org.springframework.web.servlet.ModelAndView;

public class RelationPreferenceJSONController extends BaseAbstractController
{
    private RelationPreferenceUtil relationPreferenceUtil;

    public void setRelationPreferenceUtil(
            RelationPreferenceUtil relationPreferenceUtil)
    {
        this.relationPreferenceUtil = relationPreferenceUtil;
    }

    private ApplicationService applicationService = new DSpace()
            .getServiceManager().getServiceByName("applicationService",
                    ApplicationService.class);

    private CrisSearchService crisSearchService = new DSpace()
            .getServiceManager().getServiceByName(
                    SearchService.class.getName(), CrisSearchService.class);

    private ACrisObject getCRISObject(HttpServletRequest request)
    {
        String uuid = getUUID(request);
        ACrisObject cris = applicationService.getEntityByUUID(uuid);
        return cris;
    }

    private String getUUID(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo();
        // example /uuid/XXXXXXXXX/relMgmt/publications
        String path = pathInfo.substring("/uuid/".length());
        String[] splitted = path.split("/");
        String uuid = splitted[0];
        return uuid;
    }

    private String getRelationType(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo();
        // example /uuid/XXXXXXXXX/relMgmt/publications.json
        String path = pathInfo.substring("/uuid/".length());
        String[] splitted = path.split("/");
        String type = splitted[2].split("\\.json")[0];
        return type;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        Context context = UIUtil.obtainContext(request);
        ACrisObject cris = getCRISObject(request);
        String relationType = getRelationType(request);
        String fQuery = getSearchQuery(request);
        String status = getFilterStatus(request);
        List<Sort> sorts = getSorts(request);
        int rpp = getRpp(request);
        int offset = getOffset(request);
        int sEcho = getsEcho(request);

        RelatedObjects relatedObjects = relationPreferenceUtil
                .getRelatedObject(context, cris, relationType, fQuery, status,
                        sorts, rpp, offset);
        Response resp = new Response();
        resp.setRelatedObjects(relatedObjects.getObjects());
        resp.setiTotalRecords(relatedObjects.getTotalRecords());
        resp.setiTotalDisplayRecords(relatedObjects.getFilterRecords());
        resp.setsEcho(sEcho);
        JSONSerializer serializer = new JSONSerializer();
        serializer.exclude("class", "objects.class");
        serializer.deepSerialize(resp, response.getWriter());
        response.setContentType("application/json");
        return null;
    }

    private int getsEcho(HttpServletRequest request)
    {
        return UIUtil.getIntParameter(request, "sEcho");
    }

    private String getFilterStatus(HttpServletRequest request)
    {
        return request.getParameter("filterStatus");
    }

    private int getOffset(HttpServletRequest request)
    {
        int offset = UIUtil.getIntParameter(request, "iDisplayStart");
        return offset > 0 ? offset : 0;
    }

    private int getRpp(HttpServletRequest request)
    {
        int rpp = UIUtil.getIntParameter(request, "iDisplayLength");
        return rpp > 0 ? rpp : 20;
    }

    private List<Sort> getSorts(HttpServletRequest request)
    {
        List<Sort> sorts = new ArrayList<Sort>();
        int numSort = UIUtil.getIntParameter(request, "iSortingCols");
        if (numSort > 0)
        {
            for (int idx = 0; idx < numSort; idx++)
            {
                Sort sort = new Sort();
                // we need to remove 2 because the datatable structure has uuid
                // and relationPreference as first columns
                sort.col = UIUtil.getIntParameter(request, "iSortCol_"+idx)-2;
                sort.asc = "asc".equalsIgnoreCase(request.getParameter("sSortDir_"+idx));
                sorts.add(sort);
            }
        }
            
        return sorts;
    }

    private String getSearchQuery(HttpServletRequest request)
    {
        return request.getParameter("sSearch");
    }
    
    private class Response {
        private int sEcho;
        private int iTotalRecords;
        private int iTotalDisplayRecords;
        private List<RelatedObject> relatedObjects;
        public int getsEcho()
        {
            return sEcho;
        }
        public void setsEcho(int sEcho)
        {
            this.sEcho = sEcho;
        }
        public int getiTotalRecords()
        {
            return iTotalRecords;
        }
        public void setiTotalRecords(int iTotalRecords)
        {
            this.iTotalRecords = iTotalRecords;
        }
        public int getiTotalDisplayRecords()
        {
            return iTotalDisplayRecords;
        }
        public void setiTotalDisplayRecords(int iTotalDisplayRecords)
        {
            this.iTotalDisplayRecords = iTotalDisplayRecords;
        }
        public List<RelatedObject> getRelatedObjects()
        {
            return relatedObjects;
        }
        public void setRelatedObjects(List<RelatedObject> relatedObjects)
        {
            this.relatedObjects = relatedObjects;
        }
    }
}
