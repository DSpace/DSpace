<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Renders a page containing a statistical summary of the repository usage
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.Date" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    String report = (String) request.getAttribute("report");
    Date[] months = (Date[]) request.getAttribute("months");
    String date = (String) request.getAttribute("date");
    Boolean general = (Boolean) request.getAttribute("general");
    String navbar = (String) request.getAttribute("navbar");
    
    SimpleDateFormat sdfDisplay = new SimpleDateFormat("MM'/'yyyy");
    SimpleDateFormat sdfLink = new SimpleDateFormat("yyyy'-'M");
%>

<dspace:layout style="submission" navbar="<%= navbar %>" titlekey="jsp.statistics.report.title">

    <p>
<%
    if (general.booleanValue())
    {
%>
    <strong><fmt:message key="jsp.statistics.report.info1"/></strong>
<%
    }
    else
    {
%>
    <strong><a href="<%= request.getContextPath() %>/statistics"><fmt:message key="jsp.statistics.report.info1"/></a></strong>
    </p>
<%
    }
%>
    <p>
    <strong><fmt:message key="jsp.statistics.report.info2"/></strong>
<%
        for (int i = 0; i < months.length; i++)
        {
            if (sdfLink.format(months[i]).equals(date))
            {
%>
                <strong><%= sdfDisplay.format(months[i]) %></strong>
<%
            }
            else
            {
%>
            <a href="<%= request.getContextPath() %>/statistics?date=<%= sdfLink.format(months[i]) %>"><%= sdfDisplay.format(months[i]) %></a>
<%
            }
            
            if (i != months.length - 1)
            {
%>
                &nbsp;|&nbsp;
<%
            }
%>

    <%
        }
    %>
    </p>

    <hr />

    <%= report %>

</dspace:layout>
