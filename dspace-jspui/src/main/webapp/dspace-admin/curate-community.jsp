<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Form requesting a Handle or internal item ID for item editing
  -
  - Attributes:
  -     curate_group_options - options string of gropu selection. 
  -         "" unless ui.taskgroups is set
  -     curate_task_options - options string of task selection.
  -     community - the community
  -     task_result - result of the curation task
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.CurateTaskResult" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%!
    private static final String TASK_QUEUE_NAME = ConfigurationManager.getProperty("curate", "ui.queuename");
%>
<%
    CurateTaskResult result = (CurateTaskResult) request.getAttribute("task_result");
    Community community = (Community) request.getAttribute("community");
    int communityID = (community != null ? community.getID() : -1);
    String title = (community != null ? community.getMetadata("name") : "Unknown Community");
    String groupOptions = (String)request.getAttribute("curate_group_options");
    String taskOptions = (String)request.getAttribute("curate_task_options");
%>

<dspace:layout titlekey="jsp.dspace-admin.curate.community.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

<%
    if (result != null)
    {
        String type   = result.getType();
        boolean isSuccess = result.isSuccess();
        String resultClass = (isSuccess ? "success" : "failure");
%>
    <div class="notice <%= resultClass %>">
      <h1 class="task-name">
        <fmt:message key="jsp.dspace-admin.curate.task.name">
          <fmt:param value="<%= result.getTask() %>"/>
        </fmt:message>
      </h1>
<%
        if ("perform".equals(type))
        {
            if (isSuccess)
            {
%>
      <p class="task-result"><fmt:message key="jsp.dspace-admin.curate.perform.success"/></p>
      <div class="task-message">
        <fmt:message key="jsp.dspace-admin.curate.perform.message.success">
          <fmt:param value="<%= result.getStatus() %>"/>
          <fmt:param value="<%= result.getResult() %>"/>
        </fmt:message>
      </div>
<%
            }
            else
            {
%>
      <p class="task-result"><fmt:message key="jsp.dspace-admin.curate.perform.failure"/></p>
      <div class="task-message">
        <fmt:message key="jsp.dspace-admin.curate.perform.message.success">
          <fmt:param value="<%= result.getResult() %>"/>
        </fmt:message>
      </div>
<%
            }
        }
        else
        {
            if (isSuccess)
            {
%>
      <p class="task-result"><fmt:message key="jsp.dspace-admin.curate.queue.success"/></p>
      <div class="task-message">
        <fmt:message key="jsp.dspace-admin.curate.queue.message.success">
          <fmt:param value="<%= result.getHandle() %>"/>
          <fmt:param value="<%= TASK_QUEUE_NAME %>"/>
        </fmt:message>
      </div>
<%
            }
            else
            {
%>
      <p class="task-result"><fmt:message key="jsp.dspace-admin.curate.queue.failure"/></p>
      <div class="task-message">
        <fmt:message key="jsp.dspace-admin.curate.queue.message.failure">
          <fmt:param value="<%= result.getHandle() %>"/>
          <fmt:param value="<%= TASK_QUEUE_NAME %>"/>
        </fmt:message>
      </div>
<%
            }
        }
%>
    </div>
<%
    }
%>

    <h1><fmt:message key="jsp.dspace-admin.curate.community.heading">
          <fmt:param value="<%= title %>"/>
        </fmt:message>
    </h1>

    <table width="60%">
      <form action="<%=request.getContextPath()%>/dspace-admin/curate" method="post">

<%
    if (groupOptions != null && !"".equals(groupOptions))
    {
%>
      <tr>
        <td class="curate heading">
          <fmt:message key="jsp.dspace-admin.curate.select-group.tag"/>:
        </td>
        <td class="curate field">
          <select name="select_curate_group" id="select_curate_group" onchange="this.form.submit();">
            <%= groupOptions %>
          </select>
        </td>
      </tr>
<%
    }
%>
      <tr>
        <td class="curate heading">
          <fmt:message key="jsp.dspace-admin.curate.select-task.tag"/>:
        </td>
        <td class="curate field">
          <select name="curate_task" id="curate_task">
            <%= taskOptions %>
          </select>
        </td>
      </tr>
      <tr>
        <td class="curate button" colspan="2">
          <input type="hidden" name="community_id" value="<%= communityID %>"/>
          <input type="submit" name="submit_community_curate" value="<fmt:message key="jsp.dspace-admin.curate.perform.button"/>" />
          <input type="submit" name="submit_community_queue" value="<fmt:message key="jsp.dspace-admin.curate.queue.button"/>" />
          </form>
        </td>
      </tr>
      <tr>
        <td class="curate button" colspan="2">
          <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
            <input type="hidden" name="community_id" value="<%= communityID %>"/>
            <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_EDIT_COMMUNITY%>""/>
            <input type="submit" value="<fmt:message key="jsp.dspace-admin.curate.return.community.button"/>" />
          </form>
        </td>
      </tr>
    </table>

</dspace:layout>
