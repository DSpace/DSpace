<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show user's previous (accepted) submissions
  -
  - Attributes to pass in:
  -    user     - the e-person who's submissions these are (EPerson)
  -    items    - the submissions themselves (Item[])
  -    handles  - Corresponding Handles (String[])
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="java.util.List" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("user");
    List<Item> items = (List<Item>) request.getAttribute("items");
%>

<dspace:layout style="submission" locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace">

    <%-- <h2>Your Submissions</h2> --%>
    <h2><fmt:message key="jsp.mydspace.own-submissions.title"/></h2>
    
<%
    if (items.size() == 0)
    {
%>
    <%-- <p>There are no items in the main archive that have been submitted by you.</p> --%>
	<p><fmt:message key="jsp.mydspace.own-submissions.text1"/></p>
<%
    }
    else
    {
%>
    <%-- <p>Below are listed your previous submissions that have been accepted into
    the archive.</p> --%>
	<p><fmt:message key="jsp.mydspace.own-submissions.text2"/></p>
<%
        if (items.size() == 1)
        {
%>
    <%-- <p>There is <strong>1</strong> item in the main archive that was submitted by you.</p> --%>
	<p><fmt:message key="jsp.mydspace.own-submissions.text3"/></p>
<%
        }
        else
        {
%>
    <%-- <p>There are <strong><%= items.length %></strong> items in the main archive that were submitted by you.</p> --%>
	<p><fmt:message key="jsp.mydspace.own-submissions.text4">
        <fmt:param><%= items.size() %></fmt:param>
    </fmt:message></p>
<%
        }
%>
    <dspace:itemlist items="<%= items %>" />
<%
    }
%>

    <%-- <p align="center"><a href="<%= request.getContextPath() %>/mydspace">Back to My DSpace</a></p> --%>
	<p align="center"><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.mydspace.general.backto-mydspace"/></a></p>
</dspace:layout>
