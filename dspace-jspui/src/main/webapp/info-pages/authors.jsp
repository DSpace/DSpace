<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.storage.rdbms.DatabaseManager" %>
<%@ page import="org.dspace.storage.rdbms.TableRow" %>
<%@ page import="org.dspace.storage.rdbms.TableRowIterator" %>
<%@ page import="ua.edu.sumdu.essuir.entity.AuthorLocalization" %>
<%@ page import="ua.edu.sumdu.essuir.utils.EssuirUtils" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.List" %>

<% org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    String userEmail = "";

    if (user != null)
        userEmail = user.getEmail().toLowerCase();

    Boolean admin = (Boolean) request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    if (isAdmin || userEmail.equals("library_ssu@ukr.net") || userEmail.equals("libconsult@rambler.ru")) {
%>


<dspace:layout locbar="nolink" title="Authors list" feedData="NONE">
    <table align="center" width="95%">
        <tr>
            <td>
                <p align="center">
                    <%
                        String link = "<a href=\"authors.jsp?startWith=";
                        for (char i = 'A'; i <= 'Z'; i++) {
                    %><%=(link + i) + "\">" + i + "</a> " %><%
                    }
                %>
                </p>
            </td>
            <td align="right">
                <a href="authors_edit.jsp"><fmt:message key = "jsp.dspace-admin.new-author-button" /></a>
            </td>
        </tr>
    </table>

    <table class = "table">
        <tr>
            <th><fmt:message key = "jsp.dspace-admin.authors.surname_en" /></th>
            <th><fmt:message key = "jsp.dspace-admin.authors.initials_en" /></th>
            <th><fmt:message key = "jsp.dspace-admin.authors.surname_ru" /></th>
            <th><fmt:message key = "jsp.dspace-admin.authors.initials_ru" /></th>
            <th><fmt:message key = "jsp.dspace-admin.authors.surname_ua" /></th>
            <th><fmt:message key = "jsp.dspace-admin.authors.initials_ua" /></th>
            <th>ORCID</th>
            <th><fmt:message key = "jsp.tools.itemmap-browse.th.action" /></th>
        </tr>

        <%
            String startWith = request.getParameter("startWith");
            if (startWith == null) startWith = "A";
            List<AuthorLocalization> authors = EssuirUtils.getAllAuthors(startWith);
            for (AuthorLocalization author : authors) {
                link = "?surname=" + author.getSurname_en() + "&initials=" + author.getInitials_en();
        %>
                <tr>
                    <td><%=author.getSurname_en() %></td>
                    <td><%=author.getInitials_en() %></td>
                    <td><%=author.getSurname_uk() %></td>
                    <td><%=author.getInitials_uk() %></td>
                    <td><%=author.getSurname_ru() %></td>
                    <td><%=author.getInitials_ru() %></td>
                    <td><%= author.getOrcid()%></td>
                    <td>
                        <a href="authors_edit.jsp<%=link%>">
                            <span class="glyphicon glyphicon-pencil" aria-hidden="true" style = "color:black; font-size:14pt;"></span>
                        </a>
                        <a href="authors_remove.jsp<%=link%>">
                            <span class="glyphicon glyphicon-remove" aria-hidden="true" style = "color:red; font-size:14pt;"></span>
                        </a>
                    </td>
                </tr>
        <%
            }
        %>
    </table>
</dspace:layout>

<%
    } else {
        org.dspace.app.webui.util.JSPManager.showAuthorizeError(request, response, null);
    }
%>
