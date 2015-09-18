<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Registration information form
  -
  - Form where new users enter their personal information and select a
  - password.
  -
  - Attributes to pass in:
  -
  -   eperson          - the EPerson who's registering
  -   token            - the token key they've been given for registering
  -   set.password     - if Boolean true, the user can set a password
  -   missing.fields   - if a Boolean true, the user hasn't entered enough
  -                      information on the form during a previous attempt
  -   password.problem - if a Boolean true, there's a problem with password
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.RegisterServlet" %>
<%@ page import="org.dspace.eperson.EPerson" %>

<%
    EPerson eperson = (EPerson) request.getAttribute( "eperson" );
    String token = (String) request.getAttribute("token");
    String netid = (String) request.getParameter("netid");
    String email = (String) request.getParameter("email");	

    Boolean attr = (Boolean) request.getAttribute("missing.fields");
    boolean missingFields = (attr != null && attr.booleanValue());

    attr = (Boolean) request.getAttribute("password.problem");
    boolean passwordProblem = (attr != null && attr.booleanValue());

    attr = (Boolean) request.getAttribute("set.password");
    boolean setPassword = (attr != null && attr.booleanValue());
%>

<dspace:layout style="submission" titlekey="jsp.register.registration-form.title" nocache="true">

    <%-- <h1>Registration Information</h1> --%>
	<h1><fmt:message key="jsp.register.registration-form.title"/></h1>
    
<%
    if (missingFields)
    {
%>
    <%-- <p><strong>Please fill out all of the required fields.</strong></p> --%>
	<p class="alert alert-warning"><strong><fmt:message key="jsp.register.registration-form.instruct1"/></strong></p>
<%
    }

    if( passwordProblem)
    {
%>
    <%-- <p><strong>The passwords you enter below must match, and need to be at
    least 6 characters long.</strong></p> --%>
	<p class="alert alert-warning"><strong><fmt:message key="jsp.register.registration-form.instruct2"/></strong></p>
<%
    }
%>

    <%-- <p>Please enter the following information.  The fields marked with a * are
    required.</p> --%>
	<p class="alert"><fmt:message key="jsp.register.registration-form.instruct3"/></p>
    <form class="form-horizontal" action="<%= request.getContextPath() %>/register" method="post">
    <% if (netid!=null) { %> <input type="hidden" name="netid" value="<%= netid %>" /> <% } %>
    <% if (email!=null) { %> <input type="hidden" name="email" value="<%= email %>" /> <% } %>
        <dspace:include page="/register/profile-form.jsp" />
<%

    if (setPassword)
    {
%>
        <%-- <p>Please choose a password and enter it into the box below, and confirm it by typing it
        again into the second box.  It should be at least six characters long.</p> --%>
		<p class="alert"><fmt:message key="jsp.register.registration-form.instruct4"/></p>
        
        <div class="form-group">
            <%-- <td align="right" class="standard"><strong>New Password:</strong></td> --%>
            <label class="col-md-offset-3 col-md-2 control-label" for="tpassword"><fmt:message key="jsp.register.registration-form.pswd.field"/></label>
            <div class="col-md-3">
                <input class="form-control" type="password" name="password" id="tpassword" />
            </div>
        </div>
        <div class="form-group">
            <%-- <td align="right" class="standard"><strong>Again to Confirm:</strong></td> --%>
            <label class="col-md-offset-3 col-md-2 control-label" for="tpassword_confirm"><fmt:message key="jsp.register.registration-form.confirm.field"/></label>
            <div class="col-md-3">
                <input class="form-control" type="password" name="password_confirm" id="tpassword_confirm" />
            </div>
		</div>
       
<%
    }
%>

        <input type="hidden" name="step" value="<%= RegisterServlet.PERSONAL_INFO_PAGE %>"/>
        <input type="hidden" name="token" value="<%= token %>"/>
        
        <%-- <p align="center"><input type="submit" name="submit" value="Complete Registration"></p> --%>
       	<div class="col-md-offset-5">       
	   		<input class="btn btn-success col-md-4" type="submit" name="submit" value="<fmt:message key="jsp.register.registration-form.complete.button"/>" />
	 	</div>
		
    </form>
</dspace:layout>
