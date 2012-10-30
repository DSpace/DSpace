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

    boolean advanced = ConfigurationManager.getBooleanProperty("xmlui.submission.restrictstep.enableAdvancedForm", false);

    int error_id = request.getAttribute("error_id") == null ? 0 : ((Integer)request.getAttribute("error_id")).intValue();

    Item item = subInfo.getSubmissionItem().getItem();
    String discarableChecked = item.isDiscoverable() ? "" : " checked=\"checked\"";

%>

<dspace:layout locbar="off" navbar="off" titlekey="jsp.submit.access.title" nocache="true">

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
		<h1><fmt:message key="jsp.submit.access.plist.heading"/></h1>

        <dspace:policieslist policies="<%= policies %>" />
<%
    }
%>

		<h1><fmt:message key="jsp.submit.access.access_setting.heading"/></h1>

        <h2 class="access-setting"><fmt:message key="jsp.submit.access.private_setting.heading"/></h2>

        <center>
            <table class="miscTable" width="80%">
                <tr id="private_setting">
                    <th class="accessOdd" align="left" style="padding-right: 10px"><fmt:message key="jsp.submit.access.private_setting.label"/></th>
                    <td class="accessOdd"><input id="private_option" name="private_option" type="checkbox" value="1" <%= discarableChecked %>/>&nbsp;<fmt:message key="jsp.submit.access.private_setting.help"/></td>
                </tr>
            </table>
        </center>

        <h2 class="access-setting"><fmt:message key="jsp.submit.access.embargo_setting.heading"/></h2>

        <dspace:access-setting subInfo="<%= subInfo %>" dso="<%= subInfo.getSubmissionItem().getItem() %>" embargo="<%= advanced ? true : false %>" addpolicy="<%= advanced ? true : false %>" />

        <center>
            <table class="miscTable">

		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
        <center>
            <table border="0" width="80%">
                <tr>
					<td width="100%">&nbsp;</td>
                    <td>
                        <input type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
                    </td>
                    <td>
                        <input type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <input type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>

    <script type="text/javascript" src="<%= request.getContextPath() %>/submit/access-step.js"></script>
</dspace:layout>
