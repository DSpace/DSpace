<%--
  - Component which displays the search box and Login/Logout button.
  --%>

<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.eperson.EPerson" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%
  // Is anyone logged in?
  EPerson user = (EPerson) request.getAttribute("dspace.current.user");
%>

<div>
  <%-- Search Box --%>
  <form method="get" action="<%= request.getContextPath() %>/simple-search" style="display: inline;">

    <input type="text" name="query" id="tequery" size="20" placeholder="Search DRUM ..."/>
    <input type="submit" class="btn btn-primary" id="main-query-submit" value="<fmt:message key="jsp.layout.navbar-default.go"/>" alt="<fmt:message key="jsp.layout.navbar-default.go"/>" name="submit" />
<%
    if (ConfigurationManager.getBooleanProperty("webui.controlledvocabulary.enable"))
	{
%>
      <br/><a href="<%= request.getContextPath() %>/subject-search"><fmt:message key="jsp.layout.navbar-default.subjectsearch"/></a>
<%
    }
%>
  </form>

  <%-- Login/Logout --%>
  <p>
<%
  // Logged in
  if (user != null) {
%>
    <%= user.getEmail() %>
	<a href="<%= request.getContextPath() %>/logout">
	  <button class="btn btn-primary">Logout</button>
	</a>
  <%
  // Not logged in
  } else {
%>
    <a href="<%= request.getContextPath() %>/mydspace">
	  <button class="btn btn-primary">Login</button>
	</a>

<%
  }
%>
  </p>

</div>