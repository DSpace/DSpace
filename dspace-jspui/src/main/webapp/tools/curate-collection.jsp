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
  -     collection - the collection
  -     task_result - result of the curation task
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.CurateTaskResult" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%!
    private static final String TASK_QUEUE_NAME = ConfigurationManager.getProperty("curate", "ui.queuename");
%>
<%
    Collection collection = (Collection) request.getAttribute("collection");
    int collectionID = (collection != null ? collection.getID() : -1);
    int communityID = (collection.getParentObject() != null ? collection.getParentObject().getID() : -1);
    String title = (collection != null ? collection.getMetadata("name") : "Unknown Collection");
    String groupOptions = (String)request.getAttribute("curate_group_options");
    String taskOptions = (String)request.getAttribute("curate_task_options");
%>

<dspace:layout style="submission" titlekey="jsp.tools.curate.collection.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

<%@ include file="/tools/curate-message.jsp" %>

    <h1><fmt:message key="jsp.tools.curate.collection.heading">
          <fmt:param value="<%= title %>"/>
        </fmt:message>
    </h1>
	<div class="row container">
      <form action="<%=request.getContextPath()%>/tools/curate" method="post">

<%
    if (groupOptions != null && !"".equals(groupOptions))
    {
%>
          <label for="select_curate_group"><fmt:message key="jsp.tools.curate.select-group.tag"/></label>:
          <select class="form-control" name="select_curate_group" id="select_curate_group" onchange="this.form.submit();">
            <%= groupOptions %>
          </select>  
<%
    }
%>
		<div class="input-group">
          <label for="curate_task" class="input-group-addon"><fmt:message key="jsp.tools.curate.select-task.tag"/>:</label>
          <select class="form-control" name="curate_task" id="curate_task">
            <%= taskOptions %>
          </select>
          <input type="hidden" name="collection_id" value="<%= collectionID %>"/>
		</div>
		  <br/>
          <div class="col-md-4 row pull-right">
          <input class="btn btn-warning col-md-6" type="submit" name="submit_collection_queue" value="<fmt:message key="jsp.tools.curate.queue.button"/>" />
          <input class="btn btn-primary col-md-6" type="submit" name="submit_collection_curate" value="<fmt:message key="jsp.tools.curate.perform.button"/>" />
          </div>
        </form>
		</div>
		<br/>
		<div class="row container">
          <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
            <input type="hidden" name="collection_id" value="<%= collectionID %>"/>
            <input type="hidden" name="community_id" value="<%= communityID %>" />
            <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_EDIT_COLLECTION %>" />
            <input class="btn btn-default" type="submit" value="<fmt:message key="jsp.tools.curate.return.collection.button"/>" />
          </form>
       </div>

</dspace:layout>
