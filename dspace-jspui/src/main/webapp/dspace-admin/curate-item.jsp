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
  -     item - the item
  -     task_result - result of the curation task
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.CurateTaskResult" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="java.util.UUID" %>
<%!
    private static final String TASK_QUEUE_NAME = ConfigurationManager.getProperty("curate", "ui.queuename");
%>
<%
    Item item = (Item) request.getAttribute("item");
    UUID itemID = (item != null ? item.getID() : null);
    String title = "Unknown Item";
    if (item != null)
    {
        item.getName();
    }
    String groupOptions = (String)request.getAttribute("curate_group_options");
    String taskOptions = (String)request.getAttribute("curate_task_options");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.curate.item.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

<%@ include file="/dspace-admin/curate-message.jsp" %>

    <h1><fmt:message key="jsp.dspace-admin.curate.item.heading">
          <fmt:param value="<%= title %>"/>
        </fmt:message>
    </h1>
	<div class="row container">
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
    	
    	<br/>
          <div class="col-md-4 row pull-right">
          	<input type="hidden" name="item_id" value="<%= itemID %>"/>
          	<input class="btn btn-warning col-md-6" type="submit" name="submit_item_queue" value="<fmt:message key="jsp.tools.curate.queue.button"/>" />
          	<input class="btn btn-primary col-md-6" type="submit" name="submit_item_curate" value="<fmt:message key="jsp.tools.curate.perform.button"/>" />
          </div>
        
  	</form>
      </div>  
    
    	<div class="row container">
         	<form method="get" action="<%=request.getContextPath()%>/tools/edit-item">
            	<input type="hidden" name="item_id" value="<%= itemID %>"/>
    			<input class="btn btn-default" type="submit" value="<fmt:message key="jsp.dspace-admin.curate.return.item.button"/>"/>
	        </form>
       </div>
</dspace:layout>
