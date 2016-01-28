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
  -    submission.info  - the SubmissionInfo object
  -    license          - the license text to display
  -    cclicense.exists   - boolean to indicate CC license already exists
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
 

    String reqURL = request.getRequestURL().toString();
    int firstIndex = reqURL.indexOf("://") + 3;
    int secondIndex = reqURL.indexOf("/", firstIndex);
    String baseURL = reqURL.substring(0, secondIndex) + request.getContextPath();
    String ssURL = baseURL + "/submit/creative-commons.css";
    // Use the submit process' cc-license component
    String exitURL = baseURL + "/submit/cc-license.jsp?license_url=[license_url]";

    String jurisdiction = ConfigurationManager.getProperty("webui.submit.cc-jurisdiction");
    if ((jurisdiction != null) && (!"".equals(jurisdiction)))
    {
        jurisdiction = "&amp;jurisdiction=" + jurisdiction.trim();
    }
    else
    {
        jurisdiction = "";
    }
%>

<dspace:layout navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer"
               titlekey="jsp.tools.creative-commons-edit.title" nocache="true">
<table cellspacing="8" cellpadding="24" class="pagecontent">
  <tr>
   <td>
    <h1><fmt:message key="jsp.tools.creative-commons-edit.heading1"/></h1>
    <form name="ccform" id="license_form" action="" method="get">
	<iframe src="https://creativecommons.org/choose/?partner=dspace&amp;stylesheet=<%= java.net.URLEncoder.encode(ssURL, "UTF-8") %>&amp;exit_url=<%= java.net.URLEncoder.encode(exitURL, "UTF-8") %><%= jurisdiction %>" width="100%" height="540">Your browser must support IFrames to use this feature
	</iframe>

        <input type="hidden" name="item_id" value='<%=request.getParameter("item_id")%>' />
        <input type="hidden" name="cc_license_url" value="" />
    </form> 
	</td>
  </tr>
</table>
</dspace:layout>
