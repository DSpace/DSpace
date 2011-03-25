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

<dspace:layout titlekey ="jsp.dspace-admin.news-main.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">
    
  <table width="95%">
    <tr>
      <td align="left">
        <%-- <h1>News Editor</h1> --%>
        <h1><fmt:message key="jsp.dspace-admin.news-main.heading"/></h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#editnews\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

 <form action="<%= request.getContextPath() %>/dspace-admin/news-edit" method="post">
    <table class="miscTable" align="center">
          <tr>
                <%-- <td class="oddRowOddCol">Top News</td> --%>
                <th id="t1" class="oddRowOddCol"><fmt:message key="jsp.dspace-admin.news-main.news.top"/></th> 
                <td headers="t1" class="oddRowEvenCol">
                    <input type="hidden" name="position" value="<fmt:message key="news-top.html"/>" />
                    <%-- <input type="submit" name="submit_edit" value="Edit..."> --%>
                    <input type="submit" name="submit_edit" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                </td>
            </tr>
    </table>
 </form>
 <form action="<%= request.getContextPath() %>/dspace-admin/news-edit" method="post">
    <table class="miscTable" align="center">
            <tr>
                <%-- <td class="evenRowOddCol">Sidebar News</td> --%>
                <th id="t2" class="evenRowOddCol"><fmt:message key="jsp.dspace-admin.news-main.news.sidebar"/></th>
                <td headers="t2" class="evenRowEvenCol">
                    <input type="hidden" name="position" value="<fmt:message key="news-side.html" />" />
                    <%-- <input type="submit" name="submit_edit" value="Edit..."> --%>
                    <input type="submit" name="submit_edit" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                </td>
            </tr>
    </table>
  </form>
</dspace:layout>
