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
<%@ page import="org.dspace.authorize.AuthorizeServiceImpl" %>
<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.authorize.factory.AuthorizeServiceFactory" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
	// Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    // Policies List
    List<ResourcePolicy> policies = AuthorizeServiceFactory.getInstance().getAuthorizeService().findPoliciesByDSOAndType(context, subInfo.getBitstream(), ResourcePolicy.TYPE_CUSTOM);

    boolean advanced = ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

    int error_id = request.getAttribute("error_id") == null ? 0 : ((Integer)request.getAttribute("error_id")).intValue();

    Item item = subInfo.getSubmissionItem().getItem();
    String discarableChecked = item.isDiscoverable() ? "" : " checked=\"checked\"";

%>

<dspace:layout style="submission" locbar="off" navbar="off" titlekey="jsp.submit.edit-bitstream-access.title" nocache="true">

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

		<h2 class="alert alert-info"><fmt:message key="jsp.submit.edit-bitstream-access.heading"/></h2>
        
        <dspace:access-setting subInfo="<%= subInfo %>" dso="<%= subInfo.getBitstream() %>" embargo="<%= advanced ? true : false %>" addpolicy="<%= advanced ? true : false %>" />


		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
    		<div class="col-md-4 pull-right btn-group">
                <input class="btn btn-default col-md-6" type="submit" name="submit_edit_cancel" value="<fmt:message key="jsp.submit.general.cancel"/>" />
                <input class="btn btn-primary col-md-6" type="submit" name="submit_save" value="<fmt:message key="jsp.submit.edit-bitstream-access.save.button"/>" />
			</div>
    </form>

    <script type="text/javascript" src="<%= request.getContextPath() %>/submit/access-step.js"></script>
</dspace:layout>
