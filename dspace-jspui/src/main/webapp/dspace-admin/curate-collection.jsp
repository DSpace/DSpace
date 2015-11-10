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
<%@ page import="java.util.UUID" %>
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>
<%@ page import="org.dspace.content.service.CollectionService" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.Context" %>
<%!
    private static final String TASK_QUEUE_NAME = ConfigurationManager.getProperty("curate", "ui.queuename");
%>
<%
    Context context = UIUtil.obtainContext(request);
    CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    Collection collection = (Collection) request.getAttribute("collection");
    UUID collectionID = (collection != null ? collection.getID() : null);
    UUID communityID = (collectionService.getParentObject(context, collection) != null ? collectionService.getParentObject(context, collection).getID() : null);
    String title = (collection != null ? collection.getName() : "Unknown Collection");
    String groupOptions = (String)request.getAttribute("curate_group_options");
    String taskOptions = (String)request.getAttribute("curate_task_options");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.curate.collection.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

<%@ include file="/dspace-admin/curate-message.jsp" %>

    <h1><fmt:message key="jsp.dspace-admin.curate.collection.heading">
          <fmt:param value="<%= title %>"/>
        </fmt:message>
    </h1>

    
      <form action="<%=request.getContextPath()%>/dspace-admin/curate" method="post">

<%
    if (groupOptions != null && !"".equals(groupOptions))
    {
%>
    	<div class="input-group">
          <label class="input-group-addon"><fmt:message key="jsp.dspace-admin.curate.select-group.tag"/>:</label>
    
          <select class="form-control" name="select_curate_group" id="select_curate_group" onchange="this.form.submit();">
            <%= groupOptions %>
          </select>
    	</div>
<%
    }
%>
    	<div class="input-group">      
          <label class="input-group-addon"><fmt:message key="jsp.dspace-admin.curate.select-task.tag"/>:</label>
    
          <select class="form-control" name="curate_task" id="curate_task">
            <%= taskOptions %>
          </select>
        </div>
    	<div class="input-group">      
        	<input type="hidden" name="collection_id" value="<%= collectionID %>"/>
        	<input class="btn btn-default" type="submit" name="submit_collection_curate" value="<fmt:message key="jsp.dspace-admin.curate.perform.button"/>" />
        	<input class="btn btn-default" type="submit" name="submit_collection_queue" value="<fmt:message key="jsp.dspace-admin.curate.queue.button"/>" />
        </div>
        </form>
    	<div class="input-group">      
          <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
            <input type="hidden" name="collection_id" value="<%= collectionID %>"/>
            <input type="hidden" name="community_id" value="<%= communityID %>" />
            <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_EDIT_COLLECTION %>" />
            <input class="btn btn-default" type="submit" value="<fmt:message key="jsp.dspace-admin.curate.return.collection.button"/>" />
          </form>
		</div>
</dspace:layout>
