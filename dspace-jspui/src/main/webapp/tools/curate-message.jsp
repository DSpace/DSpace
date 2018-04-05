<%@ page import="org.dspace.app.webui.util.CurateTaskResult" %>

<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%
    CurateTaskResult result = (CurateTaskResult) request.getAttribute("task_result");
    if (result != null)
    {
        String type   = result.getType();
        boolean isSuccess = result.isSuccess();
        String resultClass = (isSuccess ? "success" : "danger");
%>
    <div class="alert alert-<%= resultClass %>">
      <b>
        <fmt:message key="jsp.tools.curate.task.name">
          <fmt:param value="<%= result.getTask() %>"/>
        </fmt:message>
      </b>
<%
        if ("perform".equals(type))
        {
            if (isSuccess)
            {
%>
      <p class="task-result"><fmt:message key="jsp.tools.curate.perform.success"/></p>
      <div class="task-message">
        <fmt:message key="jsp.tools.curate.perform.message.success">
          <fmt:param value="<%= result.getStatus() %>"/>
          <fmt:param value="<%= result.getResult() %>"/>
        </fmt:message>
      </div>
<%
            }
            else
            {
%>
      <p class="task-result"><fmt:message key="jsp.tools.curate.perform.failure"/></p>
      <div class="task-message">
        <fmt:message key="jsp.tools.curate.perform.message.success">
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
      <p class="task-result"><fmt:message key="jsp.tools.curate.queue.success"/></p>
      <div class="task-message">
        <fmt:message key="jsp.tools.curate.queue.message.success">
          <fmt:param value="<%= result.getHandle() %>"/>
          <fmt:param value="<%= TASK_QUEUE_NAME %>"/>
        </fmt:message>
      </div>
<%
            }
            else
            {
%>
      <p class="task-result"><fmt:message key="jsp.tools.curate.queue.failure"/></p>
      <div class="task-message">
        <fmt:message key="jsp.tools.curate.queue.message.failure">
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
