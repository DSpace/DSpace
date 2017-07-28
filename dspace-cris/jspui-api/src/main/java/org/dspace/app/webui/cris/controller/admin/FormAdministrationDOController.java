/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;


import it.cilea.osd.jdyna.web.Utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.dto.DynamicObjectDTO;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.cris.util.DODisplayTagData;
import org.dspace.app.webui.cris.util.OUDisplayTagData;
import org.dspace.core.ConfigurationManager;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * This SpringMVC controller is responsible to handle the administrative
 * browsing of ResearcherPage and changes to the status (active) flag
 * 
 * @author cilea
 * 
 */
public class FormAdministrationDOController extends
        SimpleFormController
{
    private static final String JSP_DSPACE_ADMIN_HKU_CHANGESTATUS_DYNAMICOBJECT_MESSAGE = "jsp.dspace-admin.hku.changestatus-dynamicobject.message";
    private static final String OTHERRESEARCHOBJECT_ADMINISTRATION_TABLE_PAGESIZE = "otherresearchobject.administration.table.pagesize";
    /**
     * the applicationService for query the RP db, injected by Spring IoC
     */
    private ApplicationService applicationService;

    @Override
    protected Object formBackingObject(HttpServletRequest request)
            throws Exception
    {
        String mode = request.getParameter("mode");
        String paramSort = request.getParameter("sort");
        String paramPage = request.getParameter("page");
        String paramDir = request.getParameter("dir");
        String paramOldPage = request.getParameter("oldpage");
                
        
        if(paramOldPage!=null && (paramOldPage.equals(paramPage) || (Integer.parseInt(paramOldPage)==1 && paramPage==null))) {
            String message = request.getParameter("message");
            request.setAttribute("message", message);
        }
            
        String shortName = Utils.getAdminSpecificPath(request, null);     
        
        DynamicObjectType typo = applicationService.findTypoByShortName(DynamicObjectType.class, shortName);

        String sort = paramSort != null ? paramSort : "id";
        String dir = paramDir != null ? paramDir : "asc";
        int page = paramPage != null ? Integer.parseInt(paramPage) : 1;
        long count = applicationService.countResearchObjectByType(typo);
        Integer pagesize = Integer.parseInt(ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE,OTHERRESEARCHOBJECT_ADMINISTRATION_TABLE_PAGESIZE));
        
        //mode position only when administrator click on direct link on RP page  
        Integer id = null;
        if(mode!=null && mode.equals("position") && paramPage==null && paramSort==null) {
            String id_s = request.getParameter("id");
            id = Integer.parseInt(id_s);                        
            page = id/pagesize + 1;            
        }
        

        List<ResearchObject> researchers = applicationService
                .getResearchObjectPaginateListByType(typo, sort,
                        "desc".equals(dir), page, pagesize);
        LinkedList<DynamicObjectDTO> objectList = new LinkedList<DynamicObjectDTO>();
        for(ResearchObject r : researchers) {             
            DynamicObjectDTO rpd = new DynamicObjectDTO();
            rpd.setId(r.getId());
            rpd.setSourceID(r.getSourceID());
            rpd.setUuid(r.getUuid());
            rpd.setStatus(r.getStatus());
            rpd.setName(r.getName());
            rpd.setDynamicObject(r); 
            if((r.getId()).equals(id)) {
                objectList.addFirst(rpd);
            }
            else {
                objectList.add(rpd);
            }
        }

        DODisplayTagData displayList = new DODisplayTagData(count,
                objectList, sort, dir, page, pagesize);

        return displayList;

    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {
        String path = Utils.getAdminSpecificPath(request, null);
       
        DODisplayTagData dto = (DODisplayTagData) command;
        boolean check_change = false;
        for (DynamicObjectDTO researcher : dto.getList())
        {
//            ResearchObject realResearcher = applicationService
//                .get(ResearchObject.class, researcher.getId());
            ResearchObject realResearcher = researcher.getDynamicObject();
            
            if (realResearcher.getStatus() != null
                    && realResearcher.getStatus() != researcher.getStatus())
            {
                realResearcher.setStatus(researcher.getStatus());
                applicationService.saveOrUpdate(ResearchObject.class,
                        realResearcher);
                check_change = true;
            }
        }
               
        if(check_change) {
            Map<String, Object> model = new HashMap<String, Object>();        
            model.put("message", JSP_DSPACE_ADMIN_HKU_CHANGESTATUS_DYNAMICOBJECT_MESSAGE);        
            model.put("sort", request.getParameter("sort"));
            model.put("page", request.getParameter("page"));
            model.put("oldpage", request.getParameter("page")!=null?request.getParameter("page"):1);
            model.put("dir", request.getParameter("dir"));            
            return new ModelAndView(getSuccessView()+"?path="+path,model);
        }
        return new ModelAndView(getSuccessView()+"?path="+path);
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

}
