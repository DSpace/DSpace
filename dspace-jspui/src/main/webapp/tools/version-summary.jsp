<%@ page import="java.util.UUID" %>
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
    UUID itemID = (UUID)request.getAttribute("itemID");
	request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.version-summary.title">

    <h1><fmt:message key="jsp.dspace-admin.version-summary.heading"/></h1>

 <form action="<%= request.getContextPath() %>/tools/version" method="post">
		<input type="hidden" name="itemID" value="<%= itemID %>" />
        <p><fmt:message key="jsp.dspace-admin.version-summary.text3"><fmt:param><%= itemID%></fmt:param></fmt:message></p>
				
                   <%--  <td class="submitFormLabel">News:</td> --%>
                   <div class="form-group">
                    	<label for="summary"><fmt:message key="jsp.dspace-admin.version-summary.text"/></label>
                    	<textarea class="form-control" name="summary" rows="10" cols="50"></textarea>
                    </div>
                    <%-- <input type="submit" name="submit_save" value="Save"> --%>
                    <input class="btn btn-success" type="submit" name="submit_version" value="<fmt:message key="jsp.version.version-summary.submit_version"/>" />
                    <%-- <input type="submit" name="cancel" value="Cancel"> --%>
                    <input class="btn btn-default" type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
</form>
</dspace:layout>
