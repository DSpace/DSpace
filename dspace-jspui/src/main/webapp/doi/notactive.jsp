<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@page import="java.util.Map"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<dspace:layout locbar="link" navbar="admin" style="submission" titlekey="jsp.dspace-admin.doi">
<table width="95%">
    <tr>      
      <td align="left"><h1><fmt:message key="jsp.dspace-admin.doi"/></h1>   
      </td>
    </tr>
</table>
<div class="callout callout-success">
<h3><fmt:message key="jsp.dspace-admin.doi.inactive-heading" /></h3>
<p><fmt:message key="jsp.dspace-admin.doi.inactive-description" /></p>
</div>
</dspace:layout>