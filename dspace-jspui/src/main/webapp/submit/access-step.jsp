<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Initial questions for keeping UI as simple as possible.
  -
  - Attributes to pass in:
  -    submission.info    - the SubmissionInfo object
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.authorize.AuthorizeManager" %>
<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
	// Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    // Policies List
    List<ResourcePolicy> policies = AuthorizeManager.findPoliciesByDSOAndType(context, subInfo.getSubmissionItem().getItem(), ResourcePolicy.TYPE_CUSTOM);

    boolean advanced = ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

    int error_id = request.getAttribute("error_id") == null ? 0 : ((Integer)request.getAttribute("error_id")).intValue();

    Item item = subInfo.getSubmissionItem().getItem();
    String discoverableChecked = item.isDiscoverable() ? "" : " checked=\"checked\"";

%>

<dspace:layout style="submission" locbar="off" navbar="off" titlekey="jsp.submit.access.title" nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp" />

<%
    if (error_id > 0)
    {
        String key = "jsp.submit.access.error_" + error_id;
%>      
        <div class="submitFormWarn"><fmt:message key="<%= key %>"/></div>
<%
    }
%>
<%
    if (advanced)
    {
%>
		<h2 class="alert alert-info"><fmt:message key="jsp.submit.access.plist.heading"/></h2>

        <dspace:policieslist policies="<%= policies %>" />
<%
    }
%>

		<h2 class="alert alert-info"><fmt:message key="jsp.submit.access.access_setting.heading"/></h2>

        <h3 class="access-setting"><fmt:message key="jsp.submit.access.private_setting.heading"/></h3>

		<div class="form-group">
			<div class="input-group-addon">
            	<label for="private_option"><fmt:message key="jsp.submit.access.private_setting.label"/></label>
				<input class="form-control" id="private_option" name="private_option" type="checkbox" value="1" <%= discoverableChecked %>/><span class="help-block"><fmt:message key="jsp.submit.access.private_setting.help"/></span>
			</div>
		</div>

        <h3 class="access-setting"><fmt:message key="jsp.submit.access.embargo_setting.heading"/></h3>

        <dspace:access-setting subInfo="<%= subInfo %>" dso="<%= subInfo.getSubmissionItem().getItem() %>" embargo="<%= advanced ? true : false %>" addpolicy="<%= advanced ? true : false %>" />


		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
			<div class="btn-group pull-right col-md-offset-5">
				<input class="btn btn-default" type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />                
                <input class="btn btn-default" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                <input class="btn btn-success" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
			</div>
    </form>

    <script type="text/javascript" src="<%= request.getContextPath() %>/submit/access-step.js"></script>
</dspace:layout>
