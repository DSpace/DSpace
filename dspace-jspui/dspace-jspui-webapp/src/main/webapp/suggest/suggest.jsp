<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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
