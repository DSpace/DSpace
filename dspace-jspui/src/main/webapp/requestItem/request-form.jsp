<%--
  - RequestItem-form.jsp
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
  - Sugest form JSP
  -
  - Attributes:
  -    requestItem.problem  - if present, report that all fields weren't filled out
  -    authenticated.email - email of authenticated user, if any
  -	   handle - URL of handle item
  --%>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="pt.uminho.sdum.dspace.requestItem.servlet.RequestItemServlet"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
	request.setCharacterEncoding("UTF-8");
	
    boolean problem = (request.getAttribute("requestItem.problem") != null);

    String email = (String) request.getAttribute("email");
    if (email == null)
        email = "";

    String userName = (String) request.getAttribute("reqname");
    if (userName == null)
        userName = "";

    String handle = (String) request.getAttribute("handle");
    if (handle == null )
        handle = "";
	
    String title = (String) request.getAttribute("title");
    if (title == null)
        title = "";
		
    String coment = (String) request.getAttribute("coment");
    if (coment == null)
        coment = "";
    
    String bitstream_id = (String) request.getAttribute("bitstream-id");
    boolean allfiles = (request.getAttribute("allfiles") != null);

%>

<dspace:layout locbar="off" navbar="off" titlekey="jsp.request.item.request-form.title" >

<br />
<p><fmt:message key="jsp.request.item.request-form.info2">
<fmt:param><a href="<%=request.getContextPath()%>/handle/<%=handle %>"><%=title %></a></fmt:param>
</fmt:message>
</p>

<%
    	if (problem)
    	{
%>
        <P ALIGN="CENTER" STYLE="color: RED"><strong><fmt:message key="jsp.request.item.request-form.problem"/></strong></P>
<%
    	}
%>
    <form name="form1" action="<%= request.getContextPath() %>/request-item" method="POST">
        <center>
            <table>
                <tr>
                    <td class="submitFormLabel"><fmt:message key="jsp.request.item.request-form.reqname"/></td>
                    <td><input type="TEXT" name="reqname" size="50" value="<%= userName %>"></td>
                </tr>
                <tr>
                    <td class="submitFormLabel"><fmt:message key="jsp.request.item.request-form.email"/></td>
                    <td><input type="TEXT" name="email" size="50" value="<%= email %>"></td>
                </tr>
                <tr>
                    <td class="submitFormLabel"><fmt:message key="jsp.request.item.request-form.allfiles"/></td>
                    <td>
                        <table><tr><td align="left">
                            <input type="radio" name="allfiles" value="true" <%=allfiles?"checked":""%>><fmt:message key="jsp.request.item.request-form.yes"/></input>
                        </td></tr><tr><td align="left">
                            <input type="radio" name="allfiles" value="false" <%=allfiles?"":"checked"%>><fmt:message key="jsp.request.item.request-form.no"/></input>
                        </td></tr></table>
                    </td>
                </tr>	
                <tr>
                    <td class="submitFormLabel"><fmt:message key="jsp.request.item.request-form.coment"/></td>
                    <td><textarea name="coment" rows="6" cols="46" wrap=soft><%= coment %></textarea></td>
                </tr>	
                <tr>
                    <td colspan="2" align="center">
                    <input type="HIDDEN" name="handle" value='<%= handle %>'>
                    <input type="HIDDEN" name="bitstream-id" value='<%= bitstream_id %>'>
                    <input type="HIDDEN" name="step" value="<%=RequestItemServlet.ENTER_FORM_PAGE %>">
                    <input type="SUBMIT" name="submit" value="<fmt:message key="jsp.request.item.request-form.go"/>" >
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>