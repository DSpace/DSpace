<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Suggest received OK acknowledgement
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout style="submission" locbar="off" navbar="off" titlekey="jsp.suggestok.title">

    <h1><fmt:message key="jsp.suggestok.heading"/></h1>

    <form action="">
     	<input class="btn btn-primary" type="button" name="close" onclick="window.close();" value="<fmt:message key="jsp.suggestok.button.close"/>" />
    </form>

</dspace:layout>
