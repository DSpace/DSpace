<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Forgotten password DSpace form
  -
  - Form where new users enter their email address to get a token to enter a
  - new password.
  -
  - Attributes to pass in:
  -     retry  - if set, this is a retry after the user entered an invalid email
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.RegisterServlet" %>

<%
    boolean retry = (request.getAttribute("retry") != null);
%>

<dspace:layout titlekey="jsp.register.forgot-password.title">

    <%-- <h1>Forgotten Password</h1> --%>
	<h1><fmt:message key="jsp.register.forgot-password.title"/></h1>
    
<%
    if (retry)
    {
%>
    <%-- <p><strong>The e-mail address you entered was not recognized.  Please
    try again.</strong></p> --%>
	<p class="alert alert-warning"><fmt:message key="jsp.register.forgot-password.info1"/></p>
<%
    }
%>
    <%-- <p>Please enter your e-mail
    address in the box below and click "I Forgot My Password".  You'll be sent
    an e-mail which will allow you to set a new password.</p> --%>
	<p class="alert alert-info"><fmt:message key="jsp.register.forgot-password.info2"/></p>
    
    <form class="form-horizontal" action="<%= request.getContextPath() %>/forgot" method="post">
        <input type="hidden" name="step" value="<%= RegisterServlet.ENTER_EMAIL_PAGE %>"/>

                   	<%-- <td class="standard"><strong>E-mail Address:</strong></td> --%>
					<div class="form-group">
            			<label class="col-md-2 control-label" for="temail"><fmt:message key="jsp.register.forgot-password.email.field"/></label>
            			<div class="col-md-6">
            				<input class="form-control" type="text" name="email" id="temail" />
            			</div>
            		</div>                    
					<div class="row">
        				<div class="col-md-6">
							<%-- <input type="submit" name="submit" value="I Forgot My Password"> --%>
							<input class="btn btn-success pull-right" type="submit" name="submit" value="<fmt:message key="jsp.register.forgot-password.forgot.button"/>" />
						</div>
					</div>
    </form>
    
</dspace:layout>
