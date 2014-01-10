<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - News Edit Form JSP
  -
  - Attributes:
   --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.admin.NewsEditServlet" %>
<%@ page import="org.dspace.core.Constants" %>

<%
    String position = (String)request.getAttribute("position");

    //get the existing news
    String news = (String)request.getAttribute("news");

    if (news == null)
    {
        news = "";
    }

	request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.news-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>News Editor</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.news-edit.heading"/></h1>
    <%-- <p>Add or edit text in the box below to have it appear
    in the <strong><%= positionStr%></strong> of the DSpace home page.</p> --%>

 <form action="<%= request.getContextPath() %>/dspace-admin/news-edit" method="post">

    <p class="alert alert-info">
<% if (position.contains("top"))
   { %>
    <fmt:message key="jsp.dspace-admin.news-edit.text.topbox"/>
<% }
   else
   { %>
    <fmt:message key="jsp.dspace-admin.news-edit.text.sidebar"/>
<% } %>
    </p>
    <%-- <p>You may format the text using HTML tags, but please note that the HTML will not be validated here.</p> --%>
    <p class="alert alert-warning"><fmt:message key="jsp.dspace-admin.news-edit.text3"/></p>

        <%--  <td class="submitFormLabel">News:</td> --%>
		<span class="col-md-2"><fmt:message key="jsp.dspace-admin.news-edit.news"/></span>
        <textarea class="form-control" name="news" rows="10" cols="50"><%= news %></textarea>

        <input type="hidden" name="position" value='<%= position %>'/>
        <%-- <input type="submit" name="submit_save" value="Save"> --%>
        <input class="btn btn-primary" type="submit" name="submit_save" value="<fmt:message key="jsp.dspace-admin.general.save"/>" />
        <%-- <input type="submit" name="cancel" value="Cancel"> --%>
		<input class="btn btn-default" type="submit" name="cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />

    </form>
</dspace:layout>
