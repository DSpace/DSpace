<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - License Edit Form JSP
  -
  - Attributes:
  -  license - The license to edit
   --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    // Get the existing license
    String license = (String)request.getAttribute("license");
    if (license == null)
    {
    	license = "";
    }

    // Are there any messages to show?
    String message = (String)request.getAttribute("edited");
    boolean edited = false;
    if ((message != null) && (message.equals("true")))
    {
    	edited = true;
    }
    message = (String)request.getAttribute("empty");
    boolean empty = false;
    if ((message != null) && (message.equals("true")))
    {
    	empty = true;
    }
    
%>

<dspace:layout titlekey="jsp.dspace-admin.license-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.license-edit.heading"/></h1>
    
    <form action="<%= request.getContextPath() %>/dspace-admin/license-edit" method="post">

    <%
    	if (edited)
    	{
    		%>
	    		<p>
	    			<strong><fmt:message key="jsp.dspace-admin.license-edit.edited"/></strong>
    			</p>
    		<%
    	}
    %>
    <%
    	if (empty)
    	{
    		%>
	    		<p>
	    			<strong><fmt:message key="jsp.dspace-admin.license-edit.empty"/></strong>
    			</p>
    		<%
    	}
    %>
    
    <p><fmt:message key="jsp.dspace-admin.license-edit.description"/></p>
    <p><textarea name="license" rows="15" cols="70"><%= license %></textarea><br />
       <input type="submit" name="submit_save" value="<fmt:message key="jsp.dspace-admin.general.save"/>" />
       <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
    </p>
    </form>
</dspace:layout>
