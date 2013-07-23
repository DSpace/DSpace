<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - New password form
  -
  - Form where users can enter a new password, after having been sent a token
  - because they forgot the old one.
  -
  - Attributes to pass in:
  -    eperson          - the eperson
  -    token            - the token associated with this password setting
  -    password.problem - Boolean true if the user has already tried a password
  -                       which is for some reason unnacceptible
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.RegisterServlet" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.Utils" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");
    String token = (String) request.getAttribute("token");
    Boolean attr = (Boolean) request.getAttribute("password.problem");

    boolean passwordProblem = (attr != null && attr.booleanValue());
%>

<dspace:layout titlekey="jsp.register.new-password.title" nocache="true">

    <%-- <h1>Enter a New Password</h1> --%>
	<h1><fmt:message key="jsp.register.new-password.title"/></h1>
    
    <!-- <p>Hello <%= Utils.addEntities(eperson.getFullName()) %>,</p> -->
	<p><fmt:message key="jsp.register.new-password.hello">
        <fmt:param><%= Utils.addEntities(eperson.getFullName()) %></fmt:param>
    </fmt:message></p>
    
<%
    if (passwordProblem)
    {
%>
    <%-- <p><strong>The passwords you enter below must match, and need to be at
    least 6 characters long.</strong></p> --%>
	<p><strong><fmt:message key="jsp.register.new-password.info1"/></strong></p>
<%
    }
%>
    
    <%-- <p>Please enter a new password into the box below, and confirm it by typing it
    again into the second box.  It should be at least six characters long.</p> --%>
	<p><fmt:message key="jsp.register.new-password.info2"/></p>

    <form action="<%= request.getContextPath() %>/forgot" method="post">
        <table class="misc" align="center">
            <tr>
                <td class="oddRowEvenCol">
                    <table border="0" cellpadding="5">
                        <tr>
                            <%-- <td align="right" class="standard"><strong>New Password:</strong></td> --%>
							<td align="right" class="standard"><label for="tpassword"><strong><fmt:message key="jsp.register.new-password.pswd.field"/></strong></label></td>
                            <td class="standard"><input type="password" name="password" id="tpassword" /></td>
                        </tr>
                        <tr>
                            <%-- <td align="right" class="standard"><strong>Again to Confirm:</strong></td> --%>
							<td align="right" class="standard"><label for="tpassword_confirm"><strong><fmt:message key="jsp.register.new-password.confirm.field"/></strong></label></td>
                            <td class="standard"><input type="password" name="password_confirm" id="tpassword_confirm" /></td>
                        </tr>
                        <tr>
                            <td align="center" colspan="2">
                                <%-- <input type="submit" name="submit" value="Set New Password"> --%>
								<input type="submit" name="submit" value="<fmt:message key="jsp.register.new-password.set.button"/>" />
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>

        <input type="hidden" name="step" value="<%= RegisterServlet.NEW_PASSWORD_PAGE %>"/>
        <input type="hidden" name="token" value="<%= token %>"/>
    </form>
    
</dspace:layout>
