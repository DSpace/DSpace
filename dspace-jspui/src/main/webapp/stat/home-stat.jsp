<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<% org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);

   EPerson user = (EPerson) request.getAttribute("dspace.current.user");
   String userEmail = "";

   if (user != null)
       userEmail = user.getEmail().toLowerCase();

   Boolean admin = (Boolean) request.getAttribute("is.admin");
   boolean isAdmin = (admin == null ? false : admin.booleanValue());

   if (isAdmin || userEmail.equals("library_ssu@ukr.net") || userEmail.equals("libconsult@rambler.ru")) {
%>

<dspace:layout locbar="nolink" title="Statistics" feedData="NONE">
    <ul class="list-group">
        <li class="list-group-item"><a href="/statistics/report"><%= LocaleSupport.getLocalizedMessage(pageContext, "report.statistics") %></a></li>
        <li class="list-group-item"><a href="/stat/person-stat.jsp"><%= LocaleSupport.getLocalizedMessage(pageContext, "report.newest-persons") %></a></li>
    </ul>
</dspace:layout>

<%
  } else {
    org.dspace.app.webui.util.JSPManager.showAuthorizeError(request, response, null);
  }
%>
