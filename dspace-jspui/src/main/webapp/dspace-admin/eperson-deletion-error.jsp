<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page representing an eperson deletion error
  -
  - Attributes:
  -    eperson  - The eperson that cannot be deleted.
  -    tableList - The list of tables in which the eperson ID exists. The eperson cannot
  -             be deleted because these tables contain a column in which 
  -             there must be a valid eperson ID.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page isErrorPage="true" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.dspace.core.Utils" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");
    List tableList = (List) request.getAttribute("tableList");
    String fullName = Utils.addEntities(eperson.getFullName());
    Iterator tableIt = tableList.iterator();
  
%>
<dspace:layout style="submission" titlekey="jsp.dspace-admin.eperson-deletion-error.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.eperson-deletion-error.heading" /></h1>

    <%-- <p>The EPerson <%=fullName%> cannot be deleted because a reference to it
     exists in the following table(s):</p> --%>
    <p><fmt:message key="jsp.dspace-admin.eperson-deletion-error.errormsg">
        <fmt:param><%=fullName%></fmt:param>
    </fmt:message></p>

     <ul>
     <% while(tableIt.hasNext())
        {
            
        %><li><%=(String)tableIt.next()%></li>
        
        <%
        }  
        %>
     </ul>
    
   
    <p>&nbsp;</p>
    <p>&nbsp;</p>
    <p>&nbsp;</p>
    <p>&nbsp;</p>

    <p align="center">
        <a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople"><fmt:message key="jsp.dspace-admin.confirm-delete-format.returntoedit" /></a>
    </p>

</dspace:layout>

