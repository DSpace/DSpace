<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Renders a whole HTML page for displaying an agreement page. If user accepts agremment
  - UI moves on to show bitstream
  -
  - Attributes:
  -    item         - item, parent of bitstream, which is configured to require agreement
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.app.webui.util.ViewAgreement" %>

<%
    // Attributes
    Item item = (Item) request.getAttribute("item");
    String agreementText = ViewAgreement.getText(request.getSession(), item);
%>


<dspace:layout titlekey="jsp.agreement.title">

    <h1><fmt:message key="jsp.agreement.title"/></h1>

    <p>  <%= agreementText %> </p>

</dspace:layout>
