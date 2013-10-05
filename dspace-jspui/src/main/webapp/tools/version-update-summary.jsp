<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Insert summary for versionable item JSP
  -
  - Attributes:
   --%>

<%@page import="org.dspace.core.Context"%>
<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="org.dspace.app.webui.util.VersionUtil"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%
    Integer itemID = (Integer)request.getAttribute("itemID");	
	String versionID = (String)request.getAttribute("versionID");
	
	Context context = UIUtil.obtainContext(request);
	
	request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout titlekey="jsp.dspace-admin.version-summary.title">
    <h1><fmt:message key="jsp.dspace-admin.version-summary.heading"/></h1>

 <form action="<%= request.getContextPath() %>/tools/history" method="post">
		<input type="hidden" name="itemID" value="<%= itemID %>" />
		<input type="hidden" name="versionID" value="<%= versionID %>" />
        <p><fmt:message key="jsp.dspace-admin.version-summary.text3"><fmt:param><%= itemID%></fmt:param></fmt:message></p>
        <center>
            <table>
                <tr>
                   <%--  <td class="submitFormLabel">News:</td> --%>
                    <td class="submitFormLabel"><fmt:message key="jsp.dspace-admin.version-summary.text"/></td>
                    <td><textarea name="summary" rows="10" cols="50"><%= VersionUtil.getSummary(context, versionID) %></textarea></td>
                </tr>
                <tr>
                    <td colspan="2" align="center">                    
                    <%-- <input type="submit" name="submit_save" value="Save"> --%>
                    <input type="submit" name="submit_update" value="<fmt:message key="jsp.version.history.update"/>" />
                    <%-- <input type="submit" name="cancel" value="Cancel"> --%>
                    <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
</form>
</dspace:layout>
