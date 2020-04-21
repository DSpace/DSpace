<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="java.util.Locale" %>


<dspace:layout locbar="nolink" title="Authors list" feedData="NONE">
    <table align="center" width="95%">
        <tr>
            <td>
                <p align="center">
                    <%
                        String link = "<a href=\"/authors/list?startsWith=";
                        for (char i = 'A'; i <= 'Z'; i++) {
                    %><%=(link + i) + "\">" + i + "</a> " %><%
                    }
                %>
                </p>
            </td>
            <td align="right">
                    <a href="/authors/edit"><fmt:message key = "jsp.dspace-admin.new-author-button" /></a>
            </td>
        </tr>
    </table>

    <table class="table">
        <tr>
                <th><fmt:message key = "jsp.dspace-admin.authors.surname_en" /></th>
                <th><fmt:message key = "jsp.dspace-admin.authors.initials_en" /></th>
                <th><fmt:message key = "jsp.dspace-admin.authors.surname_ru" /></th>
                <th><fmt:message key = "jsp.dspace-admin.authors.initials_ru" /></th>
                <th><fmt:message key = "jsp.dspace-admin.authors.surname_ua" /></th>
                <th><fmt:message key = "jsp.dspace-admin.authors.initials_ua" /></th>
            <th>ORCID</th>
            <th><fmt:message key="jsp.tools.itemmap-browse.th.action"/></th>
        </tr>
        <c:forEach items="${authors}" var="author">
            <tr>
                <td>${author.getSurname(Locale.ENGLISH)}</td>
                <td>${author.getInitials(Locale.ENGLISH)}</td>

                <td>${author.getSurname(Locale.forLanguageTag("ru"))}</td>
                <td>${author.getInitials(Locale.forLanguageTag("ru"))}</td>

                <td>${author.getSurname(Locale.forLanguageTag("uk"))}</td>
                <td>${author.getInitials(Locale.forLanguageTag("uk"))}</td>
                <td>${author.getOrcid()}</td>
                <td>
                    <a href="/authors/edit?author_uuid=${author.uuid}">
                        <span class="glyphicon glyphicon-pencil" aria-hidden="true"
                              style="color:black; font-size:14pt;"></span>
                    </a>
                </td>
            </tr>
        </c:forEach>
    </table>
</dspace:layout>