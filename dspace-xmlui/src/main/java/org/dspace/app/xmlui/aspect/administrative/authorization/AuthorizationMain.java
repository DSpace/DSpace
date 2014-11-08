/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.authorization;

import java.sql.SQLException;
import java.util.ArrayList;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;

/**
 * @author Alexey Maslov
 */
public class AuthorizationMain extends AbstractDSpaceTransformer   
{	
    private static final Message T_title = 
		message("xmlui.administrative.authorization.AuthorizationMain.title");
    private static final Message T_authorize_trail =
		message("xmlui.administrative.authorization.general.authorize_trail");

    private static final Message T_main_head =
		message("xmlui.administrative.authorization.AuthorizationMain.main_head");

    private static final Message T_actions_head =
		message("xmlui.administrative.authorization.AuthorizationMain.actions_head");
    private static final Message T_actions_item_lookup =
		message("xmlui.administrative.authorization.AuthorizationMain.actions_item_lookup");

    private static final Message T_bad_name =
		message("xmlui.administrative.authorization.AuthorizationMain.bad_name");
    private static final Message T_search_help =
		message("xmlui.administrative.authorization.AuthorizationMain.search_help");
    private static final Message T_submit_find =
		message("xmlui.administrative.authorization.AuthorizationMain.submit_find");

    private static final Message T_actions_advanced =
		message("xmlui.administrative.authorization.AuthorizationMain.actions_advanced");
    private static final Message T_actions_advanced_link =
		message("xmlui.administrative.authorization.AuthorizationMain.actions_advanced_link");

    private static final Message T_containerList_head =
		message("xmlui.administrative.authorization.AuthorizationMain.containerList_head");
    private static final Message T_containerList_para =
		message("xmlui.administrative.authorization.AuthorizationMain.containerList_para");

    private static final Message T_dspace_home =
                message("xmlui.general.dspace_home");

    private static final Message T_untitled =
                message("xmlui.general.untitled");

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();



    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null, T_authorize_trail);
    }
	
    public void addBody(Body body)
            throws WingException, SQLException
    {
        /* Get and setup our parameters */
        String query = decodeFromURL(parameters.getParameter("query", null));
        String baseURL = contextPath + "/admin/epeople?administrative-continue="
                + knot.getId();

        String errorString = parameters.getParameter("errors", null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

        Division main = body.addInteractiveDivision("authorization-main", contextPath
                + "/admin/authorize", Division.METHOD_POST,
                "primary administrative authorization");
        main.setHead(T_main_head);
        //main.addPara(T_main_para);		


        // DIVISION: authorization-actions
        Division actions = main.addDivision("authorization-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_item_lookup);
        Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("identifier");
        queryField.setAutofocus("autofocus");
        if (query != null)
        {
            queryField.setValue(query);
        }
        if (errors.contains("identifier"))
        {
            queryField.addError(T_bad_name);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_edit").setValue(T_submit_find);
        actionsList.addLabel(T_actions_advanced);
        actionsList.addItemXref(baseURL+"&submit_wildcard", T_actions_advanced_link);

        // DIVISION: authorization-containerList
        Division containers = main.addDivision("authorization-containerList");
        containers.setHead(T_containerList_head);
        containers.addPara(T_containerList_para);

        List containerList = containers.addList("containerList");
        this.containerListBuilder(baseURL,containerList,null);

        main.addHidden("administrative-continue").setValue(knot.getId());
    }

    /* A recursive helper method to build the community/collection hierarchy list */
    private void containerListBuilder(String baseURL, List parentList,
            Community currentCommunity)
            throws SQLException, WingException
    {
        if (currentCommunity == null)
        {
            for (Community topLevel : communityService.findAllTop(context))
            {
                containerListBuilder(baseURL, parentList, topLevel);
            }
        }
        else
        {
            parentList.addItem().addHighlight("bold").addXref(baseURL
                    + "&submit_edit&community_id=" + currentCommunity.getID(),
                    communityService.getMetadata(currentCommunity, "name"));
            List containerSubList = null;
            for (Collection subCols : currentCommunity.getCollections())
            {
                if (containerSubList == null)
                {
                    containerSubList = parentList.addList("subList"
                            + currentCommunity.getID());
                }
                String name = collectionService.getMetadata(subCols, "name");
                if (name == null || name.length() == 0)
                {
                    containerSubList.addItemXref(baseURL
                            + "&submit_edit&collection_id=" + subCols.getID(),
                            T_untitled);
                }
                else
                {
                    containerSubList.addItemXref(baseURL
                            + "&submit_edit&collection_id=" + subCols.getID(),
                            name);
                }
            }
            for (Community subComs : currentCommunity.getSubcommunities())
            {
                if (containerSubList == null)
                {
                    containerSubList = parentList.addList("subList"
                            + currentCommunity.getID());
                }
                containerListBuilder(baseURL, containerSubList, subComs);
            }
        }
    }

}
