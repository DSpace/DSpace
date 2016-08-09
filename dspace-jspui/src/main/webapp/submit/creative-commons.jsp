<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show the user the Creative Commons license which they may grant or reject
  -
  - Attributes to pass in:
  -    cclicense.exists   - boolean to indicate CC license already exists
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.license.CreativeCommonsServiceImpl" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.license.factory.LicenseServiceFactory" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    String reqURL = request.getRequestURL().toString();
    int firstIndex = reqURL.indexOf("://") + 3;
    int secondIndex = reqURL.indexOf("/", firstIndex);
    String baseURL = reqURL.substring(0, secondIndex) + request.getContextPath();
    String ssURL = baseURL + "/submit/creative-commons.css";
    String exitURL = baseURL + "/submit/cc-license.jsp?license_url=[license_url]";
    Boolean lExists = (Boolean)request.getAttribute("cclicense.exists");
    boolean licenseExists = (lExists == null ? false : lExists.booleanValue());

    String jurisdiction = ConfigurationManager.getProperty("cc.license.jurisdiction");
    if ((jurisdiction != null) && (!"".equals(jurisdiction)))
    {
        jurisdiction = "&amp;jurisdiction=" + jurisdiction.trim();
    }
    else
    {
        jurisdiction = "";
    }

    String licenseURL = "";
    if(licenseExists)
        licenseURL = LicenseServiceFactory.getInstance().getCreativeCommonsService().getLicenseURL(context, subInfo.getSubmissionItem().getItem());
%>

<dspace:layout style="submission"
			   locbar="off"
               navbar="off"
               titlekey="jsp.submit.creative-commons.title"
               nocache="true">

    <form name="foo" id="license_form" action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

        <%-- <h1>Submit: Use a Creative Commons License</h1> --%>
		<h1><fmt:message key="jsp.submit.creative-commons.heading"/></h1>

<%
        if (licenseExists)
        {
%>
        <%-- <p>You have already chosen a Creative Commons license and added it to this item.
        You may:</p> --%>
		<p class="help-block"><fmt:message key="jsp.submit.creative-commons.info1"/></p>
    <%-- <ul>
            <li>Press the 'Next' button below to <em>keep</em> the license previously chosen.</li>
            <li>Press the 'Skip Creative Commons' button below to <em>remove</em> the current choice, and forego a Creative Commons license.</li>
            <li>Complete the selection process below to <em>replace</em> the current choice.</li>
         </ul> --%>
		 <ul class="alert alert-info">
            <li><fmt:message key="jsp.submit.creative-commons.choice1"/></li>
            <li><fmt:message key="jsp.submit.creative-commons.choice2"/></li>
            <li><fmt:message key="jsp.submit.creative-commons.choice3"/></li>
         </ul>
<%
        }
        else
        {
%>
        <%-- <p>To license your Item under Creative Commons, follow the instructions below. You will be given an opportunity to review your selection.
        Follow the 'proceed' link to add the license. If you wish to omit a Creative Commons license, press the 'Skip Creative Commons' button.</p> --%>
		<p><fmt:message key="jsp.submit.creative-commons.info2"/></p>
<%
        }
%>  

	<%-- <iframe src="http://creativecommons.org/license/?partner=dspace&stylesheet=<%= java.net.URLEncoder.encode(ssURL) %>&exit_url=<%= java.net.URLEncoder.encode(exitURL) %>" width="100%" height="540">Your browser must support IFrames to use this feature
	</iframe> --%>
	<iframe src="https://creativecommons.org/choose/?partner=dspace&amp;stylesheet=<%= java.net.URLEncoder.encode(ssURL, "UTF-8") %>&amp;exit_url=<%= java.net.URLEncoder.encode(exitURL, "UTF-8") %><%= jurisdiction %>" width="100%" height="540"><fmt:message key="jsp.submit.creative-commons.info3"/>
	</iframe>

    <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
    <%= SubmissionController.getSubmissionParameters(context, request) %>

	<input type="hidden" name="cc_license_url" value="<%=licenseURL %>" />
    <input type="submit" id="submit_grant" name="submit_grant" value="submit_grant" style="display: none;" />	
	<%
		int numButton = 2 + (!SubmissionController.isFirstStep(request, subInfo)?1:0) + (licenseExists?1:0);
	
	%>
    <div class="row col-md-<%= 2*numButton %> pull-right btn-group">
                <%  //if not first step, show "Previous" button
					if(!SubmissionController.isFirstStep(request, subInfo))
					{ %>
			<input class="btn btn-default col-md-<%= 12 / numButton %>" type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
                <%  } %>

            <input class="btn btn-default col-md-<%= 12 / numButton %>" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>"/>
			<input class="btn btn-warning col-md-<%= 12 / numButton %>" type="submit" name="submit_no_cc" value="<fmt:message key="jsp.submit.creative-commons.skip.button"/>"/>
<%
     if (licenseExists)
     {
%>
			<input class="btn btn-primary col-md-<%= 12 / numButton %>" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
<%
     }
%>
    </div>
    </form>
</dspace:layout>
