<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Register with DSpace form
  -
  - Form where new users enter their email address to get a token to access
  - the personal info page.
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

<dspace:layout titlekey="jsp.register.new-user.title">
    <%-- <h1>User Registration</h1> --%>
	<h1><fmt:message key="jsp.register.new-user.title"/></h1>
    
<%
    if (retry)
    { %>
    <%-- <p><strong>The e-mail address you entered was invalid.</strong>  Please try again.</strong></p> --%>
	<p><fmt:message key="jsp.register.new-user.info1"/></p>
<%  } %>

    <%-- <p>If you've never logged on to DSpace before, please enter your e-mail
    address in the box below and click "Register".</p> --%>
	<p><fmt:message key="jsp.register.new-user.info2"/></p>
    
    <form action="<%= request.getContextPath() %>/register" method="post">

        <input type="hidden" name="step" value="<%= RegisterServlet.ENTER_EMAIL_PAGE %>"/>

        <table class="miscTable" align="center">
            <tr>
                <td class="oddRowEvenCol">
                    <table border="0" cellpadding="5">
                        <tr>
                            <%-- <td class="standard"><strong>E-mail Address:</strong></td> --%>
							<td class="standard"><label for="temail"><strong><fmt:message key="jsp.register.new-user.email.field"/></strong></label></td>
                            <td class="standard"><input type="text" name="email" id="temail" /></td>
                        </tr>
                        <tr>
                            <td align="center" colspan="2">
                                <%-- <input type="submit" name="submit" value="Register"> --%>
								<input type="submit" name="submit" value="<fmt:message key="jsp.register.new-user.register.button"/>" />
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </form>
    <%-- <p>If you or your department are interested in registering with DSpace, please
    contact the DSpace site administrators.</p> --%>
	<p><fmt:message key="jsp.register.new-user.info3"/></p>

    <dspace:include page="/components/contact-info.jsp" />

</dspace:layout>
