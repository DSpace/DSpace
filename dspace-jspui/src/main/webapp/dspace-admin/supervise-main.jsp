<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
   - This page provides the options for administering supervisor settings
   --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<dspace:layout titlekey="jsp.dspace-admin.supervise-main.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.supervise-main.heading"/></h1>

<h3><fmt:message key="jsp.dspace-admin.supervise-main.subheading"/></h3>

<br/>

<div align="center" />
<%-- form to navigate to any of the three options available --%>
<form method="post" action="">
    <input type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.supervise-main.add.button"/>"/>
    <br/><br/>
    <input type="submit" name="submit_view" value="<fmt:message key="jsp.dspace-admin.supervise-main.view.button"/>"/>
    <br/><br/>
    <input type="submit" name="submit_clean" value="<fmt:message key="jsp.dspace-admin.supervise-main.clean.button"/>"/>
</form>
<div align="center" />

</dspace:layout>
