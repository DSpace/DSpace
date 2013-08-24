<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page representing an eperson reset password error
  
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page isErrorPage="true" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<% 
  Object i18nKey = request.getAttribute("authorize_error_message");
  String message = ""; 
  if(i18nKey!=null) {
    message = LocaleSupport.getLocalizedMessage(pageContext, i18nKey.toString());
  }
  request.removeAttribute("authorize_error_message");  
%> 
<dspace:layout titlekey="jsp.dspace-admin.eperson-main.loginAs.authorize.title">

    <%-- <h1>Authorization Required</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.eperson-main.loginAs.authorize.title"/></h1>

        
    <p><fmt:message key="jsp.dspace-admin.eperson-main.loginAs.authorize.errormsg"><fmt:param><%= message %></fmt:param></fmt:message></p>

     
    <p align="center">
        <a href="<%= request.getContextPath() %>"><fmt:message key="jsp.dspace-admin.eperson-main.loginAs.backtohome" /></a>
    </p>

</dspace:layout>

