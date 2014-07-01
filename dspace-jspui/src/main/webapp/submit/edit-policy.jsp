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
  -    rp                 - the target resource policy
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.apache.commons.lang.time.DateFormatUtils" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.submit.step.AccessStep" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.core.Context" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
	// Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    // Policies List
    ResourcePolicy rp = (ResourcePolicy) subInfo.get(AccessStep.SUB_INFO_SELECTED_RP);

%>

<dspace:layout style="submission" locbar="off" navbar="off" titlekey="jsp.submit.access.title" nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp" />

		<h1><fmt:message key="jsp.submit.access.edit_policy.heading"/></h1>

        <dspace:access-setting subInfo="<%= subInfo %>" rp="<%= rp %>" embargo="true" />

		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
        <input type="hidden" name="policy_id" value="<%= rp.getID() %>" />
        <div class="col-md-4 pull-right btn-group">
			<input class="btn btn-default col-md-6" type="submit" name="submit_edit_cancel" value="<fmt:message key="jsp.submit.general.cancel"/>" />
			<input class="btn btn-primary col-md-6" type="submit" name="submit_save" value="<fmt:message key="jsp.submit.general.save"/>" />
		</div>
    </form>

    <script type="text/javascript" src="<%= request.getContextPath() %>/submit/access-step.js"></script>

</dspace:layout>
