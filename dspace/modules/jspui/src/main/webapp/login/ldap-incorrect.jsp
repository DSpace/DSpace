<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Display message indicating password is incorrect, and allow a retry
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<dspace:layout navbar="off"
			   locbar="nolink"
			   titlekey="jsp.login.ldap-incorrect.title">
    <table border="0" width="90%">
        <tr>
            <td align="left">
                <h1><fmt:message key="jsp.login.ldap-incorrect.heading"/></h1>
            </td>
            <td align="right" class="standard">
                <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#login\" %>"><fmt:message key="jsp.help"/></dspace:popup>
            </td>
        </tr>
    </table>

    <p align="center"><strong><fmt:message key="jsp.login.ldap-incorrect.errormsg"/></strong></p>

    <dspace:include page="/components/ldap-form.jsp" />
</dspace:layout>
