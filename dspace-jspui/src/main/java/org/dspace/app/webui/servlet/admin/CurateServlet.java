/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.CurateTaskResult;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.curate.Curator;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 *
 * @author Keiji Suzuki
 */
public class CurateServlet extends DSpaceServlet
{
    // Name of queue used when tasks queued in Admin UI
    private final String TASK_QUEUE_NAME;

    // curation status codes in Admin UI: key=status code, value=localized name
    private final Map<String, String> statusMessages = new HashMap<>();

    // curation tasks to appear in admin UI: key=taskID, value=friendly name
    private Map<String, String> allTasks = new LinkedHashMap<>();

    // named groups which display together in admin UI: key=groupID, value=friendly group name
    private Map<String, String> taskGroups = new LinkedHashMap<>();

    // group membership: key=groupID, value=array of taskID
    private Map<String, String[]> groupedTasks = new LinkedHashMap<>();

    public CurateServlet()
    {
        TASK_QUEUE_NAME = configurationService.getProperty("curate.ui.queuename");
        try
        {
            setStatusMessages();
            setAllTasks();
            setTaskGroups();
            setGroupedTasks();
        }
        catch (Exception we)
        {
            // noop
        }
    }

    /** Logger */
    private static final Logger log = Logger.getLogger(CurateServlet.class);

    private final transient CommunityService communityService
             = ContentServiceFactory.getInstance().getCommunityService();
    
    private final transient CollectionService collectionService
             = ContentServiceFactory.getInstance().getCollectionService();
    
    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();
    
    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();
    
    private final transient ConfigurationService configurationService
             = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSPost(context, request, response);
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        // When task gropu is changed, no submit button is clicked.
        // Reset the submit button to inform the original page.
        if ("submit".equals(button))
        {
            if (request.getParameter("community_id") != null)
            {
                button = "submit_community_select";
            }
            else if (request.getParameter("collection_id") != null)
            {
                button = "submit_collection_select";
            }
            else if (request.getParameter("item_id") != null)
            {
                button = "submit_item_select";
            }
            else
            {
                button = "submit_main_select";
            }
        }

        if (button.startsWith("submit_community_"))
        {
            Community community = communityService.find(context, 
                UIUtil.getUUIDParameter(request, "community_id"));
            request.setAttribute("community", community);

            if (!authorizeService.isAdmin(context, community))
            {
                throw new AuthorizeException("Only community admins are allowed to perform curation tasks");
            }

            if ("submit_community_curate".equals(button))
            {
                processCurateObject(context, request, community.getHandle());
            }
            else if ("submit_community_queue".equals(button))
            {
                processQueueObject(context, request, community.getHandle());
            }

            showPage(request, response, "/tools/curate-community.jsp");
        }
        else if (button.startsWith("submit_collection_"))
        {
            Collection collection = collectionService.find(context, 
                UIUtil.getUUIDParameter(request, "collection_id"));
            request.setAttribute("collection", collection);

            if (!authorizeService.isAdmin(context, collection))
            {
                throw new AuthorizeException("Only collection admins are allowed to perform curation tasks");
            }

            if ("submit_collection_curate".equals(button))
            {
                processCurateObject(context, request, collection.getHandle());
            }
            else if ("submit_collection_queue".equals(button))
            {
                processQueueObject(context, request, collection.getHandle());
            }

            showPage(request, response, "/tools/curate-collection.jsp");
        }
        else if (button.startsWith("submit_item_"))
        {
            Item item = itemService.find(context, 
                UIUtil.getUUIDParameter(request, "item_id"));
            request.setAttribute("item", item);

            if (!authorizeService.isAdmin(context, item))
            {
                throw new AuthorizeException("Only item admins are allowed to perform curation tasks");
            }

            if ("submit_item_curate".equals(button))
            {
                processCurateObject(context, request, item.getHandle());
            }
            else if ("submit_item_queue".equals(button))
            {
                processQueueObject(context, request, item.getHandle());
            }

            showPage(request, response, "/tools/curate-item.jsp");
        }
        else if (button.startsWith("submit_main_"))
        {
            String handle = request.getParameter("handle");
            if (handle != null)
            {
                if (handle.endsWith("/0"))
                {
                    if (!authorizeService.isAdmin(context))
                    {
                        throw new AuthorizeException("Only system admins are allowed to perform curation tasks over the site");
                    } 
                }
                else
                {
                    DSpaceObject dso = handleService.resolveToObject(context, handle);
                    if (!authorizeService.isAdmin(context, dso))
                    {
                        throw new AuthorizeException("Only object (hdl:"+handle+") admins are allowed to perform curation tasks");
                    }
                }
                if ("submit_main_curate".equals(button))
                {
                    processCurateObject(context, request, handle);
                }
                else if ("submit_main_queue".equals(button))
                {
                    processQueueObject(context, request, handle);
                }
                else if ("submit_main_cancel".equals(button))
                {
                    handle = null;
                }
                request.setAttribute("handle", handle);
            }

            showPage(request, response, "/dspace-admin/curate-main.jsp");
        }
        else
        {
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response,
            String page) throws ServletException, IOException, 
                                SQLException, AuthorizeException
    {
        String group = request.getParameter("select_curate_group");
        String groupOptions = getGroupSelectOptions(group);
        String taskOptions  = getTaskSelectOptions(group);

        request.setAttribute("curate_group_options", groupOptions);
        request.setAttribute("curate_task_options", taskOptions);
        JSPManager.showJSP(request, response, page);
    }

    private void processCurateObject(Context context, HttpServletRequest request, String handle)
    {
        String task   = request.getParameter("curate_task");
        Curator curator = getCurator(task);
        boolean success = false;
        try
        {
            curator.curate(context, handle);
            success = true;
        }
        catch (Exception e)
        {
            curator.setResult(task, e.getMessage());
        }

        request.setAttribute("task_result", getCurateMessage(context, curator, task, handle, success));
    }

    private void processQueueObject(Context context, HttpServletRequest request, String handle)    {
        String task   = request.getParameter("curate_task");
        Curator curator = getCurator(task);
        boolean success = false;
        try
        {
            curator.queue(context, handle, TASK_QUEUE_NAME);
            success = true;
        }
        catch (Exception e)
        {
            // no-op (any error should be logged by the Curator itself)
        }

        request.setAttribute("task_result", new CurateTaskResult("queue", getTaskName(task), handle, null, null, success));
    }

    private CurateTaskResult getCurateMessage(Context context, Curator curator, String task, String handle, boolean success)
    {
        String status = statusMessages.get(String.valueOf(curator.getStatus(task)));
        if (status == null)
        {
            status = statusMessages.get("other");
        }

        String result = curator.getResult(task);
        if (result == null)
        {
            result = I18nUtil.getMessage("org.dspace.app.webui.servlet.admin.CurationServlet.null-result", context);
        }

        return new CurateTaskResult("perform", getTaskName(task), handle, status, result, success);
    }

    private Curator getCurator(String task)
    {
        if (task != null && task.length() == 0)
        {
            task = null;
        }
        Curator curator = new Curator();
        curator.addTask(task);
        curator.setInvoked(Curator.Invoked.INTERACTIVE);
        return curator;
    }
    
    private void setStatusMessages() throws UnsupportedEncodingException
    {
        String[] statusCodes = configurationService.getArrayProperty("curate.ui.statusmessages");
        for (String property : statusCodes)
        {
            String[] keyValuePair = property.split("=");
            statusMessages.put(URLDecoder.decode(keyValuePair[0].trim(), "UTF-8"),
                               URLDecoder.decode(keyValuePair[1].trim(), "UTF-8"));
        }
    }

    private void setAllTasks() throws UnsupportedEncodingException
    {
        String[] properties = configurationService.getArrayProperty("curate.ui.tasknames");
        for (String property : properties)
        {
            String[] keyValuePair = property.split("=");
            allTasks.put(URLDecoder.decode(keyValuePair[0].trim(), "UTF-8"),
                         URLDecoder.decode(keyValuePair[1].trim(), "UTF-8"));
        }
    }
    
    private void setTaskGroups() throws UnsupportedEncodingException
    {
        String[] groups = configurationService.getArrayProperty("curate.ui.taskgroups");
        if (groups != null)
        {
            for (String property : groups)
            {
                String[] keyValuePair = property.split("=");
                taskGroups.put(URLDecoder.decode(keyValuePair[0].trim(), "UTF-8"),
                               URLDecoder.decode(keyValuePair[1].trim(), "UTF-8"));
            }
        }
    }
    
    private void setGroupedTasks() throws UnsupportedEncodingException
    {
        if (!taskGroups.isEmpty())
        {
            Iterator<String> iterator = taskGroups.keySet().iterator();
            while (iterator.hasNext())
            {
                String groupID = iterator.next();
                String[] members = configurationService.getArrayProperty("curate.ui.taskgroup" + "." + groupID);
                groupedTasks.put(URLDecoder.decode(groupID, "UTF-8"), members);
            }
        }
    }

    /**
     * Get the string of html option elements for group selection
     * 
     * @param group the short name / identifier for the group
     * @return the string of the html option elements
     *         return "" if no group exists.
     */
    private String getGroupSelectOptions(String group)
    {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = taskGroups.keySet().iterator();
        while (iterator.hasNext())
        {
            String groupID = iterator.next();
            sb.append("<option");
            if (groupID.equals(group))
            {
                sb.append(" selected=\"selected\"");
            }
            sb.append(" value=\"").append(groupID).append("\">")
              .append(taskGroups.get(groupID)).append("</option>\n");
        }
        return sb.toString();
    }

    /**
     * Get the string of html option elements for task selection of the specified task group
     *   when no task group exists, made from all tasks
     * 
     * @param group the short name / identifier for the group
     * @return the string of the html option elements
     */
    private String getTaskSelectOptions(String group)
    {
        StringBuilder sb = new StringBuilder();
        if (groupedTasks.isEmpty())
        {
            Iterator<String> iterator = allTasks.keySet().iterator();
            while (iterator.hasNext())
            {
                String task = iterator.next();
                sb.append("<option value=\"").append(task).append("\">")
                  .append(allTasks.get(task)).append("</option>\n");
            }
        }
        else
        {
            if (group == null || "".equals(group))
            {
                group = groupedTasks.keySet().iterator().next();
            }

            String[] members = groupedTasks.get(group);
            if (members != null && members.length > 0)
            {
                for (String member : members)
                {
                    Iterator<String> innerIterator = allTasks.keySet().iterator();
                    while (innerIterator.hasNext())
                    {
                        String taskID = innerIterator.next().trim();
                        if (taskID.equals(member.trim()))
                        {
                            sb.append("<option value=\"").append(taskID).append("\">")
                              .append(allTasks.get(taskID)).append("</option>\n");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Retrieve UI "friendly" Task Name for display to user
     * 
     * @param taskID the short name / identifier for the task
     * @return the User Friendly name for this task
     */
    private String getTaskName(String taskID)
    {
        return allTasks.containsKey(taskID) ? allTasks.get(taskID) : taskID;
    }

}
