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

<dspace:layout style="submission" titlekey="jsp.register.new-password.title" nocache="true">

    <%-- <h1>Enter a New Password</h1> --%>
	<h1><fmt:message key="jsp.register.new-password.title"/></h1>
    
    <!-- <p>Hello <%= Utils.addEntities(eperson.getFullName()) %>,</p> -->
	<p class="alert"><fmt:message key="jsp.register.new-password.hello">
        <fmt:param><%= Utils.addEntities(eperson.getFullName()) %></fmt:param>
    </fmt:message></p>
    
<%
    if (passwordProblem)
    {
%>
    <%-- <p><strong>The passwords you enter below must match, and need to be at
    least 6 characters long.</strong></p> --%>
	<p class="alert alert-warning"><strong><fmt:message key="jsp.register.new-password.info1"/></strong></p>
<%
    }
%>
    
    <%-- <p>Please enter a new password into the box below, and confirm it by typing it
    again into the second box.  It should be at least six characters long.</p> --%>
	<p><fmt:message key="jsp.register.new-password.info2"/></p>

    <form class="form-horizontal" action="<%= request.getContextPath() %>/forgot" method="post">
        <div class="form-group">
            <%-- <td align="right" class="standard"><strong>New Password:</strong></td> --%>
            <label class="col-md-offset-3 col-md-2 control-label" for="tpassword"><fmt:message key="jsp.register.new-password.pswd.field"/></label>
            <div class="col-md-3">
                <input class="form-control" type="password" name="password" id="tpassword" />
            </div>
        </div>
        <div class="form-group">
            <%-- <td align="right" class="standard"><strong>Again to Confirm:</strong></td> --%>
            <label class="col-md-offset-3 col-md-2 control-label" for="tpassword_confirm"><fmt:message key="jsp.register.new-password.confirm.field"/></label>
            <div class="col-md-3">
                <input class="form-control" type="password" name="password_confirm" id="tpassword_confirm" />
            </div>
		</div>
        <div class="col-md-offset-5">
            <%-- <p align="center"><input type="submit" name="submit" value="Update Profile"></p> --%>
            <input class="btn btn-success col-md-4" type="submit" name="submit" value="<fmt:message key="jsp.register.new-password.set.button"/>" />
        </div>
	 
	    <input type="hidden" name="step" value="<%= RegisterServlet.NEW_PASSWORD_PAGE %>"/>
        <input type="hidden" name="token" value="<%= token %>"/>
    </form>
    
</dspace:layout>
