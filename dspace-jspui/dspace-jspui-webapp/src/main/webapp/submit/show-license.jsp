<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show the user a license which they may grant or reject
  -
  - Attributes to pass in:
  -    license          - the license text to display
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
 
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    String license = (String) request.getAttribute("license");
%>

<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.show-license.title"
               nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

	<div><fmt:message key="jsp.submit.show-license.info1"/>
        &nbsp;&nbsp;<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") +\"#license\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>

        <%-- <p><strong>Not granting the license will not delete your submission.</strong>
        Your item will remain in your "My DSpace" page.  You can then either remove
        the submission from the system, or agree to the license later once any
        queries you might have are resolved.</p> --%>
		<p><fmt:message key="jsp.submit.show-license.info2"/></p>

        <table class="miscTable" align="center">
            <tr>
                <td class="oddRowEvenCol">
                    <pre><%= license %></pre>
                </td>
            </tr>
        </table>

        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

        <center>
	    <p><input type="submit" name="submit_grant" value="<fmt:message key="jsp.submit.show-license.grant.button"/>" /></p>
            <p><input type="submit" name="submit_reject" value="<fmt:message key="jsp.submit.show-license.notgrant.button"/>" /></p>          
        </center>
    </form>
</dspace:layout>
