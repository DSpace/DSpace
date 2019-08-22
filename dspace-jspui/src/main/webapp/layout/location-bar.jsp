<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Location bar component
  -
  - This component displays the "breadcrumb" style navigation aid at the top
  - of most screens.
  -
  - Uses request attributes set in org.dspace.app.webui.jsptag.Layout, and
  - hence must only be used as part of the execution of that tag.  Plus,
  - dspace.layout.locbar should be verified to be true before this is included.
  -
  -  dspace.layout.parenttitles - List of titles of parent pages
  -  dspace.layout.parentlinks  - List of URLs of parent pages, empty string
  -                               for non-links
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>


<%
    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    if (supportedLocales != null && supportedLocales.length > 1)
    {
%>
<ol class="breadcrumb btn-success">
    <div style="position:absolute;right:30px;">
        <form method="get" name="repost" action="">
            <input type ="hidden" name ="locale"/>
        </form>
        <%
            for (int i = supportedLocales.length-1; i >= 0; i--)
            {
        %>
        <a href="#" class ="langChangeOn"
           onclick="javascript:document.repost.locale.value='<%=supportedLocales[i].toString()%>';
                   document.repost.submit();">
            <img width="20px" height="14px" src="/flags/<%=supportedLocales[i].toString()%>.gif" alt="<%= supportedLocales[i].getDisplayLanguage(supportedLocales[i])%>"/>
        </a>
        <%
            }
        %>
    </div>
        <%
    }
%>

<%
    List parentTitles = (List) request.getAttribute("dspace.layout.parenttitles");
    List parentLinks = (List) request.getAttribute("dspace.layout.parentlinks");

    for (int i = 0; i < parentTitles.size(); i++)
    {
        String s = (String) parentTitles.get(i);
        String u = (String) parentLinks.get(i);

        if (u.equals(""))
        {
            if (i == parentTitles.size())
            {
%>
<li class="active"><%= s %></li>
<%
            }
            else
            {
%>
<li><%= s %></li>
<%			}
        }
        else
        {
%>
  <li><a href="<%= request.getContextPath() %><%= u %>"><%= s %></a></li>
<%
        }
}
%>
</ol>
