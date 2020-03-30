package org.ssu.controller;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseInfo;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.sort.SortException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.response.ItemResponse;
import org.ssu.service.BrowseContext;
import org.ssu.service.BrowseRequestProcessor;
import org.ssu.service.CommunityService;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Controller
@RequestMapping("")
public class BrowseController {
    @Resource
    private CommunityService communityService;

    @Resource
    private BrowseRequestProcessor browseRequestProcessor;
    private HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    @RequestMapping(value = "/123456789/{itemId}/browse")
    public ModelAndView browseInCommunity(ModelAndView model, HttpServletRequest request, HttpServletResponse response, @PathVariable("itemId") String itemId) throws ServletException, AuthorizeException, IOException, SQLException, BrowseException, SortException {
        Context dspaceContext = UIUtil.obtainContext(request);
        DSpaceObject dSpaceObject = handleService.resolveToObject(dspaceContext, "123456789/" + itemId);
        request.setAttribute("dspace.context", dspaceContext);
        if (authorizeService.authorizeActionBoolean(dspaceContext, dSpaceObject, Constants.READ)) {
            if (dSpaceObject.getType() == Constants.COLLECTION) {
                request.setAttribute("dspace.collection", dSpaceObject);
            }
            if (dSpaceObject.getType() == Constants.COMMUNITY) {
                request.setAttribute("dspace.community", dSpaceObject);
            }
        }
        ModelAndView result = getBrowseItems(model, request, response, dspaceContext);
        result.addObject("contextName", dSpaceObject.getName());
        return result;
    }

    @RequestMapping("/browse")
    public ModelAndView getBrowseItems(ModelAndView model, HttpServletRequest request, HttpServletResponse response, Context dspaceContext) throws SortException, ServletException, IOException, AuthorizeException, SQLException {
        String type = request.getParameter("type");
        String value = request.getParameter("value");

        BrowseInfo browseInfo = new BrowseContext().getBrowseInfo(dspaceContext, request, response);
        Boolean isExtendedTable = false;
        List<ItemResponse> items;
        if (("author".equals(type) || "subject".equals(type)) && (value == null || value.isEmpty())) {
            items = communityService.getShortList(dspaceContext, browseInfo);
        } else {
            items = communityService.getItems(dspaceContext, browseInfo);
            isExtendedTable = true;
            model.addObject("searchQuery", value);
        }

        browseRequestProcessor.fillModelWithData(model, items, browseInfo, request, isExtendedTable);
        return model;
    }

}
