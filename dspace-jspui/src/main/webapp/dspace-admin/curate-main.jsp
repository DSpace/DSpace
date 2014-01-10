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
  -     handle - handle of the DSpaceObject
  -     task_result - result of the curation task
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.util.CurateTaskResult" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%!
    private static final String TASK_QUEUE_NAME = ConfigurationManager.getProperty("curate", "ui.queuename");
%>
<%
    String handle  = (String) request.getAttribute("handle");
    if (handle == null)
    {
        handle = "";
    }
    String groupOptions = (String)request.getAttribute("curate_group_options");
    String taskOptions = (String)request.getAttribute("curate_task_options");
%>

<dspace:layout 
			   style="submission"
			   titlekey="jsp.dspace-admin.curate.main.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

<%@ include file="/tools/curate-message.jsp" %>

<form action="<%=request.getContextPath()%>/dspace-admin/curate" method="post">

  <h1><fmt:message key="jsp.dspace-admin.curate.main.heading"/></h1>

	<div class="input-group">
       	<label class="input-group-addon"><fmt:message key="jsp.dspace-admin.curate.main.info1"/>:</label>
       	<input class="form-control" type="text" name="handle" value="<%= handle %>" size="20"/>
       	<span class="col-md-10"><fmt:message key="jsp.dspace-admin.curate.main.info2"/></span>
	</div>
	
	
    
<%
    if (groupOptions != null && !"".equals(groupOptions))
    {
%>
 	<div class="input-group">
      <label class="input-group-addon"><fmt:message key="jsp.tools.curate.select-group.tag"/>:</label>
  
        <select class="form-control" name="select_curate_group" id="select_curate_group" onchange="this.form.submit();">
          <%= groupOptions %>
        </select>
  	</div>
<%
    }
%>
  <div class="input-group">
      <label class="input-group-addon"><fmt:message key="jsp.tools.curate.select-task.tag"/>:</label>
  
        <select class="form-control" name="curate_task" id="curate_task">
          <%= taskOptions %>
        </select>
  </div>
  
  <div class="input-group">
	<input type="hidden" name="handle" value="<%= handle %>"/>
    <input class="btn btn-default" type="submit" name="submit_main_curate" value="<fmt:message key="jsp.tools.curate.perform.button"/>" />
    <input class="btn btn-default" type="submit" name="submit_main_queue" value="<fmt:message key="jsp.tools.curate.queue.button"/>" />
    <input class="btn btn-default" type="submit" name="submit_main_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
  </div>
</form>
</dspace:layout>
