<%--
  - request-information.jsp
  -
  - Version: $Revision: 1.0 $
  -
  - Date: $Date: 2004/12/29 19:51:49 $
  -
  - Copyright (c) 2004, University of Minho
  -   All rights reserved.
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
  - request-information JSP
  -
  - Attributes:
  -     token - 
  -     handle - URL of handle item
  -     title - 
  -     request-name -
  --%>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="pt.uminho.sdum.dspace.requestItem.servlet.RequestItemServlet"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
    String token = request.getParameter("token");

    String handle = (String) request.getAttribute("handle");
    if (handle == null)
        handle = "";
	
    String title = (String) request.getAttribute("title");
    if (title == null)
        title = "";
    
    String requestName = (String) request.getAttribute("request-name");
    if (requestName == null)
        requestName = "";

%>

<dspace:layout locbar="off" navbar="off" titlekey="jsp.request.item.request-information.title" >

<br>
<p><fmt:message key="jsp.request.item.request-information.info1">
<fmt:param><a href="<%=request.getContextPath()%>/handle/<%=handle %>"><%=title %></a></fmt:param>
<fmt:param><%=requestName %></a></fmt:param>
</fmt:message> 
</p>
    <form name="form1" action="<%= request.getContextPath() %>/request-item" method="POST">
        <input type="HIDDEN" name="token" value='<%= token %>'>
        <input type="HIDDEN" name="step" value='<%=RequestItemServlet.APROVE_TOKEN %>'>
        <center>
            <table>
                <tr>
                    <td align="center">
                    <input type="SUBMIT" name="submit_yes" value="<fmt:message key="jsp.request.item.request-information.yes"/>" >
                    </td>
                </tr>
                <tr>
                    <td align="center">
                    <input type="SUBMIT" name="submit_no" value="<fmt:message key="jsp.request.item.request-information.no"/>" >
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>
