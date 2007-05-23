<%--
  - eperson-deletion-error.jsp
  
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
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
<%@ page import="java.util.Vector" %>
<%@ page import="java.util.Iterator" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");
    Vector tableList = (Vector) request.getAttribute("tableList");
    String fullName = eperson.getFullName();
    Iterator tableIt = tableList.iterator();
  
%>
<dspace:layout titlekey="jsp.dspace-admin.eperson-deletion-error.title"
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

