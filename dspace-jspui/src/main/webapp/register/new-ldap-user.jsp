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

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="org.dspace.app.webui.servlet.RegisterServlet" %>

<%
    boolean retry = (request.getAttribute("retry") != null);
%>

<dspace:layout titlekey="jsp.register.new-ldap-user.title">

    <h1><fmt:message key="jsp.register.new-ldap-user.heading"/></h1>
    
<%
    if (retry)
    { %>
	<p><strong><fmt:message key="jsp.register.new-ldap-user.info1"/></strong></p>
<%  } %>

    <p><fmt:message key="jsp.register.new-ldap-user.info2"/></p>
    
    <form action="<%= request.getContextPath() %>/register" method="post">

        <input type="hidden" name="step" value="<%= RegisterServlet.ENTER_EMAIL_PAGE %>">
        	<table class="miscTable" align="center">
                <tr>
                    <td class="oddRowEvenCol">
                        <table border="0" cellpadding="5">
                            <tr>
                                <td class="standard"><strong><fmt:message key="jsp.register.new-ldap-user.label.username"/></strong></td>
                                <td class="standard"><input type="text" name="netid"></td>
                            </tr>
                            <tr>
                                <td class="standard"><strong><fmt:message key="jsp.register.new-ldap-user.label.password"/></strong></td>
                                <td class="standard"><input type="password" name="password"></td>
                            </tr>
                            <tr>
                                <td class="standard"><strong><fmt:message key="jsp.register.new-ldap-user.label.email"/></strong></td>
                                <td class="standard"><input type="text" name="email"></td>
                            </tr>
                            <tr>
                                <td align="center" colspan="2">
                                    <input type="submit" name="submit" value="<fmt:message key="jsp.register.new-ldap-user.button.register"/>"/>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
        </table>
    </form>
    
    <p><fmt:message key="jsp.register.new-ldap-user.info3" /></p>

    <dspace:include page="/components/contact-info.jsp" />

</dspace:layout>
