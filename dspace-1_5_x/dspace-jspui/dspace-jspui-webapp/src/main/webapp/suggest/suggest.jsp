<%--
  - suggest.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
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
  - Suggest form JSP
  -
  - Attributes:
  -    suggest.problem  - if present, report that all fields weren't filled out
  -    suggest.title - item title
  -    authenticated.email - email of authenticated user, if any
  -    eperson.name - name of suggesting eperson
  --%>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
	request.setCharacterEncoding("UTF-8");

    boolean problem = (request.getAttribute("suggest.problem") != null);

    String sender_email = request.getParameter("sender_email");
    if (sender_email == null || sender_email.equals(""))
    {
        sender_email = (String) request.getAttribute("authenticated.email");
    }
    if (sender_email == null)
    {
        sender_email = "";
    }

    String sender_name = (String) request.getParameter("sender_name");
	if (sender_name == null || sender_name.equals(""))
	{
		sender_name = (String) request.getAttribute("eperson.name");
	}
	if (sender_name == null)
	{
		sender_name = "";
	}

    String handle = request.getParameter("handle");
    if (handle == null || handle.equals(""))
    {
        handle = "";
    }

	String title = (String) request.getAttribute("suggest.title");
	if (title == null)
	{
		title = "";
	}

    String recip_email = request.getParameter("recip_email");
    if (recip_email == null)
    {
        recip_email = "";
    }

    String recip_name = request.getParameter("recip_name");
    if (recip_name == null)
    {
        recip_name = "";
    }

    String message = request.getParameter("message");
    if (message == null)
    {
        message = "";
    }
%>

<dspace:layout locbar="off" navbar="off" titlekey="jsp.suggest.title">

<br/>
<h1><fmt:message key="jsp.suggest.heading"/>
    <a href="<%= request.getContextPath() %>/handle/<%= handle %>"><%= title %></a>
</h1>
<p><fmt:message key="jsp.suggest.invitation"/></p>

    <form name="form1" method="post" action="">
        <center>
            <table>
<%
    if (problem)
    {
%>
        		<tr>
            		<td class="submitFormWarn"><fmt:message key="jsp.suggest.warning"/></td>
        		</tr>
<%
    }
%>
				<tr>
                    <td class="submitFormLabel"><fmt:message key="jsp.suggest.recipname"/></td>
                    <td><input type="text" name="recip_name" size="50" value="<%=StringEscapeUtils.escapeHtml(recip_name)%>" /></td>
                </tr>
				<tr>
                    <td class="submitFormLabel"><fmt:message key="jsp.suggest.recipemail"/></td>
                    <td><input type="text" name="recip_email" size="50" value="<%=StringEscapeUtils.escapeHtml(recip_email)%>"/></td>
                </tr>
				<tr>
                    <td class="submitFormLabel"><fmt:message key="jsp.suggest.sendername"/></td>
                    <td><input type="text" name="sender_name" size="50" value="<%=StringEscapeUtils.escapeHtml(sender_name)%>"/></td>
                </tr>
                <tr>
                    <td class="submitFormLabel"><fmt:message key="jsp.suggest.senderemail"/></td>
                    <td><input type="text" name="sender_email" size="50" value="<%=StringEscapeUtils.escapeHtml(sender_email)%>"/></td>
                </tr>
                <tr>
                    <td class="submitFormLabel"><fmt:message key="jsp.suggest.message"/></td>
                    <td><textarea name="message" rows="6" cols="46"><%=StringEscapeUtils.escapeHtml(message)%></textarea></td>
                </tr>

                <tr>
                    <td colspan="2" align="center">
                    <input type="hidden" name="handle" value='<%= handle %>'/>
                    <input type="submit" name="submit" value="<fmt:message key="jsp.suggest.button.send"/>" />
                    <input type="button" name="cancel" onclick="window.close();" value="<fmt:message key="jsp.suggest.button.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>
