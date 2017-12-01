<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of Groups, with 'edit' and 'delete' buttons next to them
  -
  - Attributes:
  -
  -   groups - Group [] of groups to work on
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.Constants" %>


<%
    String news = (String)request.getAttribute("news");

    if (news == null)
    {
        news = "";
    }

%>

<dspace:layout style="submission" titlekey ="jsp.dspace-admin.news-main.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">
    

        <%-- <h1>News Editor</h1> --%>
      <h1><fmt:message key="jsp.dspace-admin.news-main.heading"/>
      <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#editnews\"%>"><fmt:message key="jsp.help"/></dspace:popup>
	  </h1>
			
		<form action="<%= request.getContextPath() %>/dspace-admin/news-edit" method="post">			
			<select class="form-control" name="position" size="5">
				<option value="<fmt:message key="news-top.html"/>"><fmt:message key="jsp.dspace-admin.news-main.news.top"/></option>
				<option value="<fmt:message key="news-side.html"/>"><fmt:message key="jsp.dspace-admin.news-main.news.sidebar"/></option>
			</select>
			<input class="btn btn-primary" type="submit" name="submit_edit" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
 		</form>		
</dspace:layout>
