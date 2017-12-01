<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - request-information JSP
  -
  - Attributes:
  -     token - 
  -     handle - URL of handle item
  -     title - 
  -     request-name -
  --%>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="org.dspace.app.webui.servlet.RequestItemServlet"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
    String token = request.getParameter("token");

    String handle = (String) request.getAttribute("handle");
    if (handle == null)
        handle = "";
	
    String title = (String) request.getAttribute("title");
    if (title == null)
        title = "";
    
    String requestName = (String) request.getAttribute("request-name");
    if (requestName == null)
        requestName = "";

%>

<dspace:layout locbar="off" navbar="default" titlekey="jsp.request.item.request-information.title" >
<h2><fmt:message key="jsp.request.item.request-information.info1" /></h2>
<p><fmt:message key="jsp.request.item.request-information.info2">
<fmt:param><a href="<%=request.getContextPath()%>/handle/<%=handle %>"><%=title %></a></fmt:param>
<fmt:param><%=requestName %></a></fmt:param>
</fmt:message></p>
<p class="alert alert-info"><fmt:message key="jsp.request.item.request-information.note" /></p>
<form name="form1" action="<%= request.getContextPath() %>/request-item" method="post">
    <input type="hidden" name="token" value='<%= token %>' />
    <input type="hidden" name="step" value='<%=RequestItemServlet.APROVE_TOKEN %>' />
	<div class="text-center">
        <input class="btn btn-danger" type="submit" name="submit_no" value="<fmt:message key="jsp.request.item.request-information.no"/>" />
        <input class="btn btn-success" type="submit" name="submit_yes" value="<fmt:message key="jsp.request.item.request-information.yes"/>" />
    </div>
    </div>
</form>

</dspace:layout>
