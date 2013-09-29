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

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%
    String itemID = (String)request.getAttribute("itemID");
	request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout titlekey="jsp.dspace-admin.version-summary.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.version-summary.heading"/></h1>

 <form action="<%= request.getContextPath() %>/tools/version-item" method="post">

        <p><fmt:message key="jsp.dspace-admin.version-summary.text3"><fmt:param><%= itemID%></fmt:param></fmt:message></p>
        <center>
            <table>
                <tr>
                   <%--  <td class="submitFormLabel">News:</td> --%>
                    <td class="submitFormLabel"><fmt:message key="jsp.dspace-admin.version-summary.text"/></td>
                    <td><textarea name="summary" rows="10" cols="50"></textarea></td>
                </tr>
                <tr>
                    <td colspan="2" align="center">                    
                    <%-- <input type="submit" name="submit_save" value="Save"> --%>
                    <input type="submit" name="submit_version" value="<fmt:message key="jsp.version.version-summary.submit_version"/>" />
                    <%-- <input type="submit" name="cancel" value="Cancel"> --%>
                    <input type="submit" name="cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
</form>
</dspace:layout>
